package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.GoodsReceiptDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.PickListDao;
import com.oms.dao.SalesOrderDao;
import com.oms.dao.StockReservationDao;
import com.oms.dao.SystemSettingDao;
import com.oms.model.OrderStatus;
import com.oms.model.PageResult;
import com.oms.model.PickListDetail;
import com.oms.model.PickListFilter;
import com.oms.model.PickListRow;
import com.oms.model.PickListStatus;
import com.oms.model.PickPendingOrder;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

// Phiếu soạn hàng (S6-03).
//  * Gộp nhiều đơn đã duyệt cùng kho xuất thành một lượt soạn; đơn chuyển sang "Đang soạn hàng".
//  * Dòng soạn chia theo lô hạn sử dụng gần nhất (FEFO), kèm vị trí lưu của SKU, sắp theo thứ tự đi soạn của vị trí.
//  * Dòng đơn chưa có giữ chỗ (giữ chỗ đã hết hạn) được giữ lại lúc lập phiếu; hàng đã vào soạn thì bỏ hạn giữ chỗ.
//  * Người soạn đánh dấu từng dòng và ghi số thực lấy; lấy thiếu bắt buộc chọn lý do. Hoàn tất khi mọi dòng đã soạn.
//  * Đơn còn nợ hàng (xuất thiếu, giao bổ sung sau - S6-05) lập phiếu soạn bổ sung, giữ nguyên trạng thái đơn.
public class PickListService {

    public static final int PAGE_SIZE = 10;
    // Một lượt soạn gộp tối đa bấy nhiêu đơn
    static final int MAX_ORDERS = 30;
    private static final Pattern QTY_PATTERN = Pattern.compile("\\d{1,12}([.,]\\d{1,3})?");
    private static final String ENTITY = "PICK_LIST";

    private final PickListDao pickListDao = new PickListDao();
    private final GoodsReceiptDao receiptDao = new GoodsReceiptDao();
    private final StockReservationDao reservationDao = new StockReservationDao();
    private final SalesOrderDao salesOrderDao = new SalesOrderDao();
    private final SystemSettingDao settingDao = new SystemSettingDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Lỗi nghiệp vụ hiện cho người dùng (không lập / ghi nhận được)
    public static class PickListException extends Exception {
        public PickListException(String message) {
            super(message);
        }
    }

    // Dữ liệu màn danh sách: đơn chờ soạn, đơn còn nợ hàng (hiện riêng), trang phiếu soạn
    public record Overview(List<PickPendingOrder> pending, List<PickPendingOrder> backorders,
                           PageResult<PickListRow> pickLists) {
        public List<PickPendingOrder> getPending() {
            return pending;
        }

        public List<PickPendingOrder> getBackorders() {
            return backorders;
        }

        public PageResult<PickListRow> getPickLists() {
            return pickLists;
        }
    }

    public record Created(long id, String pickNo) {
    }

    // Một dòng trên form soạn hàng: đã đánh dấu soạn, số thực lấy, lý do thiếu (chuỗi gốc người dùng nhập)
    public record LineInput(boolean picked, String qty, String reason) {
        public boolean isPicked() {
            return picked;
        }

        public String getQty() {
            return qty;
        }

        public String getReason() {
            return reason;
        }
    }

    public DeliveryScope scope(long userId) throws SQLException {
        return DeliveryScope.of(permissionDao, userId);
    }

    public Overview overview(PickListFilter filter, int requestedPage, DeliveryScope scope) throws SQLException {
        Long salesRepId = scope.salesRepId();
        long total = pickListDao.countPickLists(filter, salesRepId);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        PageResult<PickListRow> pickLists = new PageResult<>(
                pickListDao.findPickLists(filter, salesRepId, (page - 1) * PAGE_SIZE, PAGE_SIZE), page, PAGE_SIZE, total);
        return new Overview(pickListDao.findPendingOrders(filter, salesRepId, DateTimeUtil.today()),
                pickListDao.findBackorders(filter, salesRepId), pickLists);
    }

    // null nếu không có phiếu hoặc người dùng không được xem
    public PickListDetail find(long id, DeliveryScope scope) throws SQLException {
        if (!scope.allowed()) {
            return null;
        }
        if (scope.salesRepId() != null && !pickListDao.hasOrderOfSalesRep(id, scope.salesRepId())) {
            return null;
        }
        return pickListDao.findDetail(id);
    }

    // Kho người dùng được thao tác (S1-09); rỗng là mọi kho
    public boolean canUseWarehouse(long userId, long warehouseId) throws SQLException {
        Set<Long> assigned = receiptDao.findUserWarehouseIds(userId);
        return assigned.isEmpty() || assigned.contains(warehouseId);
    }

    public Created create(List<Long> orderIds, long actorUserId, String ipAddress)
            throws SQLException, PickListException {
        List<Long> ids = orderIds.stream().distinct().sorted().toList();
        if (ids.isEmpty()) {
            throw new PickListException("Chọn ít nhất một đơn chờ soạn để tạo phiếu soạn.");
        }
        if (ids.size() > MAX_ORDERS) {
            throw new PickListException("Một phiếu soạn gộp tối đa " + MAX_ORDERS + " đơn.");
        }
        Set<Long> assigned = receiptDao.findUserWarehouseIds(actorUserId);
        LocalDate today = DateTimeUtil.today();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<PickListDao.OrderLock> orders = pickListDao.lockOrders(connection, ids);
                if (orders.size() != ids.size()) {
                    throw new PickListException("Có đơn không còn tồn tại; tải lại trang rồi chọn lại.");
                }
                long warehouseId = orders.get(0).warehouseId();
                String warehouseCode = orders.get(0).warehouseCode();
                checkOrders(connection, orders, assigned);
                Map<Long, List<PickListDao.OpenItem>> itemsByOrder = new LinkedHashMap<>();
                Map<Long, BigDecimal> toReserve = new TreeMap<>();
                Map<Long, String> skus = new HashMap<>();
                for (PickListDao.OrderLock order : orders) {
                    List<PickListDao.OpenItem> items = pickListDao.findOpenItems(connection, order.id());
                    if (items.isEmpty()) {
                        throw new PickListException("Đơn " + order.orderNo() + " không còn hàng phải giao.");
                    }
                    itemsByOrder.put(order.id(), items);
                    for (PickListDao.OpenItem item : items) {
                        skus.put(item.productId(), item.sku());
                        toReserve.merge(item.productId(),
                                item.reservationId() == null ? item.remaining() : BigDecimal.ZERO, BigDecimal::add);
                    }
                }
                // Khoá sổ tồn theo thứ tự SKU; dòng chưa giữ chỗ phải đủ tồn khả dụng
                Map<Long, PickListDao.Balance> balances = new HashMap<>();
                List<String> shortages = new ArrayList<>();
                for (Map.Entry<Long, BigDecimal> entry : toReserve.entrySet()) {
                    PickListDao.Balance balance = pickListDao.lockBalance(connection, warehouseId, entry.getKey());
                    balances.put(entry.getKey(), balance);
                    if (entry.getValue().signum() > 0) {
                        String shortage = StockReservationService.shortage(skus.get(entry.getKey()), warehouseCode,
                                entry.getValue(), balance == null ? null : balance.available());
                        if (shortage != null) {
                            shortages.add(shortage);
                        }
                    }
                }
                if (!shortages.isEmpty()) {
                    throw new PickListException("Không đủ tồn khả dụng để giữ chỗ cho phiếu soạn: "
                            + String.join("; ", shortages) + ".");
                }
                for (List<PickListDao.OpenItem> items : itemsByOrder.values()) {
                    for (PickListDao.OpenItem item : items) {
                        if (item.reservationId() == null) {
                            reservationDao.addReserved(connection, warehouseId, item.productId(), item.remaining());
                            pickListDao.insertReservation(connection, item.id(), warehouseId, item.productId(),
                                    item.remaining(), actorUserId);
                        }
                    }
                }
                String pickNo = pickListDao.nextPickNo(connection);
                long pickListId = pickListDao.insertPickList(connection, pickNo, warehouseId, actorUserId);
                int lines = allocate(connection, pickListId, warehouseId, itemsByOrder, balances, today, actorUserId);
                List<String> orderNos = new ArrayList<>();
                for (PickListDao.OrderLock order : orders) {
                    orderNos.add(order.orderNo());
                    pickListDao.clearHoldClock(connection, order.id());
                    if (OrderStatus.APPROVED.equals(order.status())) {
                        pickListDao.changeOrderStatus(connection, order.id(), OrderStatus.APPROVED, OrderStatus.PICKING,
                                actorUserId);
                        salesOrderDao.insertStatusHistory(connection, order.id(), OrderStatus.APPROVED,
                                OrderStatus.PICKING, "Đưa vào phiếu soạn " + pickNo, actorUserId);
                    }
                }
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("pickNo", pickNo);
                values.put("warehouseId", warehouseId);
                values.put("orders", orderNos);
                values.put("lines", lines);
                auditLogDao.insert(connection, actorUserId, "PICK_LIST_CREATE", ENTITY, pickListId, null,
                        JsonUtil.object(values), null, ipAddress);
                connection.commit();
                return new Created(pickListId, pickNo);
            } catch (SQLException | PickListException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Cùng kho xuất, kho mình phụ trách; đơn đã duyệt (hoặc còn nợ hàng) và chưa nằm trong phiếu soạn nào đang mở
    private void checkOrders(Connection connection, List<PickListDao.OrderLock> orders, Set<Long> assigned)
            throws SQLException, PickListException {
        long warehouseId = orders.get(0).warehouseId();
        if (!assigned.isEmpty() && !assigned.contains(warehouseId)) {
            throw new PickListException("Bạn chỉ tạo phiếu soạn cho kho mình phụ trách.");
        }
        for (PickListDao.OrderLock order : orders) {
            if (order.warehouseId() != warehouseId) {
                throw new PickListException("Các đơn gộp trong một phiếu soạn phải cùng kho xuất; đơn "
                        + order.orderNo() + " thuộc kho khác.");
            }
            boolean backorder = OrderStatus.SHIPPED.equals(order.status())
                    || OrderStatus.DELIVERED.equals(order.status());
            if (!OrderStatus.APPROVED.equals(order.status()) && !backorder) {
                throw new PickListException("Đơn " + order.orderNo() + " đang ở trạng thái \""
                        + OrderStatus.label(order.status()) + "\", không còn chờ soạn.");
            }
            if (pickListDao.hasOpenPick(connection, order.id())) {
                throw new PickListException("Đơn " + order.orderNo() + " đã nằm trong một phiếu soạn khác.");
            }
        }
    }

    // Chia phần còn phải giao của từng dòng đơn vào các lô theo FEFO. Lô không đủ (tồn theo lô lệch sổ tồn) thì phần
    // còn lại ghi vào lô mặc định để người soạn báo thiếu. Trả về số dòng soạn đã ghi.
    private int allocate(Connection connection, long pickListId, long warehouseId,
                         Map<Long, List<PickListDao.OpenItem>> itemsByOrder, Map<Long, PickListDao.Balance> balances,
                         LocalDate today, long actorUserId) throws SQLException {
        Map<Long, List<PickListDao.LotStock>> lotsByProduct = new HashMap<>();
        Map<Long, BigDecimal> used = new HashMap<>();
        int lines = 0;
        for (List<PickListDao.OpenItem> items : itemsByOrder.values()) {
            for (PickListDao.OpenItem item : items) {
                List<PickListDao.LotStock> lots = lotsByProduct.get(item.productId());
                if (lots == null) {
                    lots = pickListDao.findPickableLots(connection, warehouseId, item.productId(), today);
                    lotsByProduct.put(item.productId(), lots);
                }
                PickListDao.Balance balance = balances.get(item.productId());
                Long locationId = balance == null ? null : balance.locationId();
                BigDecimal left = item.remaining();
                for (PickListDao.LotStock lot : lots) {
                    BigDecimal free = lot.free().subtract(used.getOrDefault(lot.lotId(), BigDecimal.ZERO));
                    if (free.signum() <= 0) {
                        continue;
                    }
                    BigDecimal take = free.min(left);
                    pickListDao.insertItem(connection, pickListId, item.id(), item.productId(), lot.lotId(), locationId,
                            take);
                    used.merge(lot.lotId(), take, BigDecimal::add);
                    left = left.subtract(take);
                    lines++;
                    if (left.signum() == 0) {
                        break;
                    }
                }
                if (left.signum() > 0) {
                    long defaultLotId = receiptDao.findOrCreateLot(connection, item.productId(), null, null, 0L,
                            actorUserId);
                    pickListDao.insertItem(connection, pickListId, item.id(), item.productId(), defaultLotId, locationId,
                            left);
                    lines++;
                }
            }
        }
        return lines;
    }

    // Lỗi theo id dòng soạn; rỗng là hợp lệ. complete = true (Hoàn tất) thì mọi dòng phải đã đánh dấu soạn.
    public static Map<Long, String> validate(PickListDetail detail, Map<Long, LineInput> inputs, boolean complete) {
        Map<Long, String> errors = new LinkedHashMap<>();
        for (PickListDetail.Line line : detail.getLines()) {
            LineInput input = inputs.get(line.getId());
            if (input == null || !input.picked()) {
                if (complete) {
                    errors.put(line.getId(), "Chưa đánh dấu đã soạn dòng này.");
                }
                continue;
            }
            BigDecimal qty = parseQty(input.qty());
            if (qty == null) {
                errors.put(line.getId(), "SL lấy là số không âm, tối đa 3 số lẻ.");
            } else if (qty.compareTo(line.getQtyRequired()) > 0) {
                errors.put(line.getId(), "SL lấy không vượt SL cần (" + plain(line.getQtyRequired()) + ").");
            } else if (qty.compareTo(line.getQtyRequired()) < 0 && !PickListStatus.isShortReason(input.reason())) {
                errors.put(line.getId(), "Soạn thiếu " + plain(line.getQtyRequired().subtract(qty))
                        + ", bắt buộc chọn lý do.");
            }
        }
        return errors;
    }

    // Gọi validate trước (không lỗi). Lưu số thực lấy từng dòng; complete = true thì chuyển phiếu sang Đã soạn.
    public void savePicking(long pickListId, Map<Long, LineInput> inputs, boolean complete, long actorUserId,
                            String ipAddress) throws SQLException, PickListException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PickListDao.PickHead head = lockEditable(connection, pickListId, actorUserId);
                int picked = 0;
                int shortLines = 0;
                for (PickListDao.PickLine line : pickListDao.findLines(connection, pickListId)) {
                    LineInput input = inputs.get(line.id());
                    BigDecimal qty = input == null || !input.picked() ? null : parseQty(input.qty());
                    if (qty == null) {
                        if (complete) {
                            throw new PickListException("Còn dòng chưa soạn; đánh dấu đủ các dòng rồi hoàn tất.");
                        }
                        pickListDao.updateLine(connection, line.id(), null, null);
                        continue;
                    }
                    boolean isShort = qty.compareTo(line.qtyRequired()) < 0;
                    pickListDao.updateLine(connection, line.id(), qty, isShort ? input.reason() : null);
                    picked++;
                    shortLines += isShort ? 1 : 0;
                }
                String status = complete ? PickListStatus.DONE : PickListStatus.PICKING;
                pickListDao.updatePickStatus(connection, pickListId, status, actorUserId);
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("pickNo", head.pickNo());
                values.put("status", status);
                values.put("pickedLines", picked);
                values.put("shortLines", shortLines);
                auditLogDao.insert(connection, actorUserId, complete ? "PICK_LIST_COMPLETE" : "PICK_LIST_PROGRESS",
                        ENTITY, pickListId, JsonUtil.object(Map.of("pickNo", head.pickNo(), "status", head.status())),
                        JsonUtil.object(values), null, ipAddress);
                connection.commit();
            } catch (SQLException | PickListException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Huỷ phiếu chưa hoàn tất, hoặc đã soạn xong mà chưa xác nhận xuất kho đơn nào (vd không lấy được hàng):
    // đơn quay về "Đã duyệt" chờ soạn lại và giữ chỗ tính hạn lại từ lúc huỷ
    public String cancel(long pickListId, long actorUserId, String ipAddress) throws SQLException, PickListException {
        int holdHours = settingDao.getInt(StockReservationService.HOLD_HOURS_SETTING,
                StockReservationService.DEFAULT_HOLD_HOURS);
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PickListDao.PickHead head = pickListDao.lockPickList(connection, pickListId);
                if (head == null) {
                    throw new PickListException("Không tìm thấy phiếu soạn.");
                }
                boolean doneNotIssued = PickListStatus.DONE.equals(head.status())
                        && pickListDao.countDeliveryNotes(connection, pickListId) == 0;
                if (!PickListStatus.isEditable(head.status()) && !doneNotIssued) {
                    throw new PickListException("Phiếu " + head.pickNo() + " đã xác nhận xuất kho hoặc đã huỷ,"
                            + " không huỷ được nữa.");
                }
                if (!canUseWarehouse(actorUserId, head.warehouseId())) {
                    throw new PickListException("Bạn chỉ thao tác phiếu soạn của kho mình phụ trách.");
                }
                pickListDao.cancelPickList(connection, pickListId, actorUserId);
                List<String> orderNos = new ArrayList<>();
                for (String[] order : pickListDao.findOrders(connection, pickListId)) {
                    long orderId = Long.parseLong(order[0]);
                    orderNos.add(order[1]);
                    if (OrderStatus.PICKING.equals(order[2]) && pickListDao.changeOrderStatus(connection, orderId,
                            OrderStatus.PICKING, OrderStatus.APPROVED, actorUserId)) {
                        salesOrderDao.insertStatusHistory(connection, orderId, OrderStatus.PICKING, OrderStatus.APPROVED,
                                "Huỷ phiếu soạn " + head.pickNo() + ", đơn chờ soạn lại", actorUserId);
                        reservationDao.startHoldClock(connection, orderId, holdHours);
                    }
                }
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("pickNo", head.pickNo());
                values.put("status", PickListStatus.CANCELLED);
                values.put("orders", orderNos);
                auditLogDao.insert(connection, actorUserId, "PICK_LIST_CANCEL", ENTITY, pickListId,
                        JsonUtil.object(Map.of("pickNo", head.pickNo(), "status", head.status())),
                        JsonUtil.object(values), null, ipAddress);
                connection.commit();
                return head.pickNo();
            } catch (SQLException | PickListException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private PickListDao.PickHead lockEditable(Connection connection, long pickListId, long actorUserId)
            throws SQLException, PickListException {
        PickListDao.PickHead head = pickListDao.lockPickList(connection, pickListId);
        if (head == null) {
            throw new PickListException("Không tìm thấy phiếu soạn.");
        }
        if (!PickListStatus.isEditable(head.status())) {
            throw new PickListException("Phiếu " + head.pickNo() + " đã ở trạng thái \""
                    + PickListStatus.label(head.status()) + "\", không thay đổi được nữa.");
        }
        if (!canUseWarehouse(actorUserId, head.warehouseId())) {
            throw new PickListException("Bạn chỉ thao tác phiếu soạn của kho mình phụ trách.");
        }
        return head;
    }

    // Số lượng không âm, tối đa 3 số lẻ (dấu chấm hoặc phẩy); null nếu sai
    static BigDecimal parseQty(String value) {
        if (value == null || !QTY_PATTERN.matcher(value.trim()).matches()) {
            return null;
        }
        return new BigDecimal(value.trim().replace(',', '.'));
    }

    static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
