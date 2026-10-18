package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.DeliveryNoteDao;
import com.oms.dao.GoodsReceiptDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.PickListDao;
import com.oms.dao.SalesOrderDao;
import com.oms.model.DeliveryIssueDraft;
import com.oms.model.DeliveryNoteDetail;
import com.oms.model.DeliveryNoteStatus;
import com.oms.model.OrderStatus;
import com.oms.model.PickListStatus;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

// Xác nhận xuất kho & giao thiếu (S6-05), lập từ một đơn trong phiếu soạn đã soạn xong.
//  * Nhập số thực xuất từng dòng (không vượt số đã soạn); xuất thiếu bắt buộc chọn lý do và chọn xử lý phần thiếu:
//    giao bổ sung sau (phần thiếu thành hàng còn nợ, giữ chỗ lại, hiện ở bảng "Đơn còn nợ hàng") hoặc đóng phần thiếu.
//  * Xác nhận: trừ tồn thực tế theo lô đã soạn (FEFO), ghi thẻ kho ISSUE, chuyển giữ chỗ thành xuất thực (FULFILLED),
//    cộng số đã xuất của dòng đơn, đơn "Đang soạn hàng" chuyển "Đã xuất kho". Tất cả trong một transaction.
//  * Phiếu xuất đã xác nhận không sửa được (mỗi phiếu soạn chỉ lập một phiếu xuất cho mỗi đơn).
public class DeliveryNoteService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern QTY_PATTERN = Pattern.compile("\\d{1,12}([.,]\\d{1,3})?");
    private static final Pattern TIME_PATTERN = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");
    // Giờ giao bổ sung mặc định khi chỉ chọn ngày
    static final LocalTime DEFAULT_BACKORDER_TIME = LocalTime.of(8, 0);
    private static final int NOTE_MAX_LENGTH = 1000;
    private static final String ENTITY = "DELIVERY_NOTE";

    private final DeliveryNoteDao noteDao = new DeliveryNoteDao();
    private final PickListDao pickListDao = new PickListDao();
    private final GoodsReceiptDao receiptDao = new GoodsReceiptDao();
    private final SalesOrderDao salesOrderDao = new SalesOrderDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Lỗi nghiệp vụ hiện cho người dùng (không xác nhận được)
    public static class DeliveryException extends Exception {
        public DeliveryException(String message) {
            super(message);
        }
    }

    public record Issued(long id, String deliveryNo) {
    }

    // Một dòng trên form: số thực xuất, lý do thiếu (chuỗi gốc người dùng nhập)
    public record LineInput(String qty, String reason) {
        public String getQty() {
            return qty;
        }

        public String getReason() {
            return reason;
        }
    }

    // Form xác nhận xuất kho; lines theo id dòng đơn
    public record IssueForm(Map<Long, LineInput> lines, String shortageAction, String backorderDate,
                            String backorderTime, String shortageNote) {
        public Map<Long, LineInput> getLines() {
            return lines;
        }

        public String getShortageAction() {
            return shortageAction;
        }

        public String getBackorderDate() {
            return backorderDate;
        }

        public String getBackorderTime() {
            return backorderTime;
        }

        public String getShortageNote() {
            return shortageNote;
        }
    }

    public DeliveryScope scope(long userId) throws SQLException {
        return DeliveryScope.of(permissionDao, userId);
    }

    // null nếu phiếu soạn không có đơn này hoặc người dùng không được xem đơn
    public DeliveryIssueDraft draft(long pickListId, long orderId, DeliveryScope scope) throws SQLException {
        if (!scope.allowed() || (scope.salesRepId() != null
                && !scope.salesRepId().equals(noteDao.findSalesRepOfOrder(orderId)))) {
            return null;
        }
        return noteDao.findDraft(pickListId, orderId);
    }

    public DeliveryNoteDetail find(long noteId, DeliveryScope scope) throws SQLException {
        DeliveryNoteDao.NoteOwner owner = noteDao.findOwner(noteId);
        if (owner == null || !scope.allowed()
                || (scope.salesRepId() != null && !scope.salesRepId().equals(owner.salesRepId()))) {
            return null;
        }
        return noteDao.findDetail(noteId);
    }

    public boolean canUseWarehouse(long userId, long warehouseId) throws SQLException {
        Set<Long> assigned = receiptDao.findUserWarehouseIds(userId);
        return assigned.isEmpty() || assigned.contains(warehouseId);
    }

    // Form mở lần đầu: thực xuất = số đã soạn, lý do lấy từ lúc soạn; phần thiếu mặc định giao bổ sung ngày mai 8:00
    public static IssueForm defaultForm(DeliveryIssueDraft draft, LocalDate today) {
        Map<Long, LineInput> lines = new LinkedHashMap<>();
        for (DeliveryIssueDraft.Line line : draft.getLines()) {
            String reason = PickListStatus.isShortReason(line.getPickShortReason()) ? line.getPickShortReason() : null;
            lines.put(line.getOrderItemId(), new LineInput(line.getQtyPickedText(), reason));
        }
        return new IssueForm(lines, DeliveryNoteStatus.BACKORDER, today.plusDays(1).toString(),
                DEFAULT_BACKORDER_TIME.toString(), null);
    }

    // Lỗi theo tên ô ("line_<id dòng đơn>", "shortageAction", "backorderDate", "backorderTime", "shortageNote",
    // "lines"); rỗng là hợp lệ
    public static Map<String, String> validate(DeliveryIssueDraft draft, IssueForm form, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean shortage = false;
        for (DeliveryIssueDraft.Line line : draft.getLines()) {
            String key = "line_" + line.getOrderItemId();
            LineInput input = form.lines().get(line.getOrderItemId());
            BigDecimal qty = input == null ? null : parseQty(input.qty());
            if (qty == null) {
                errors.put(key, "Nhập số thực xuất (số không âm, tối đa 3 số lẻ).");
                continue;
            }
            if (qty.compareTo(line.getQtyPicked()) > 0) {
                errors.put(key, "Thực xuất không vượt số đã soạn (" + PickListService.plain(line.getQtyPicked()) + ").");
                continue;
            }
            total = total.add(qty);
            if (qty.compareTo(line.getQtyRequired()) < 0) {
                shortage = true;
                if (!PickListStatus.isShortReason(input.reason())) {
                    errors.put(key, "Dòng " + line.getSku() + " thiếu "
                            + PickListService.plain(line.getQtyRequired().subtract(qty))
                            + ", bắt buộc chọn lý do trước khi xác nhận.");
                }
            }
        }
        if (errors.isEmpty() && total.signum() == 0) {
            errors.put("lines", "Phiếu xuất phải có ít nhất một sản phẩm. Nếu không xuất được hàng, huỷ phiếu soạn"
                    + " để đơn quay về chờ soạn.");
        }
        if (shortage) {
            if (!DeliveryNoteStatus.isShortageAction(form.shortageAction())) {
                errors.put("shortageAction", "Chọn cách xử lý phần thiếu.");
            } else if (DeliveryNoteStatus.BACKORDER.equals(form.shortageAction())) {
                LocalDate date = parseDate(form.backorderDate());
                if (date == null) {
                    errors.put("backorderDate", "Chọn ngày giao bổ sung.");
                } else if (date.isBefore(today)) {
                    errors.put("backorderDate", "Ngày giao bổ sung không được trước hôm nay.");
                }
                if (form.backorderTime() != null && !TIME_PATTERN.matcher(form.backorderTime()).matches()) {
                    errors.put("backorderTime", "Giờ giao bổ sung dạng HH:mm, vd 08:30.");
                }
            }
        }
        if (form.shortageNote() != null && form.shortageNote().length() > NOTE_MAX_LENGTH) {
            errors.put("shortageNote", "Ghi chú tối đa " + NOTE_MAX_LENGTH + " ký tự.");
        }
        return errors;
    }

    // Gọi validate trước (không lỗi)
    public Issued issue(DeliveryIssueDraft draft, IssueForm form, long actorUserId, String ipAddress)
            throws SQLException, DeliveryException {
        Map<Long, BigDecimal> issuedQty = new HashMap<>();
        boolean shortage = false;
        for (DeliveryIssueDraft.Line line : draft.getLines()) {
            BigDecimal qty = parseQty(form.lines().get(line.getOrderItemId()).qty());
            issuedQty.put(line.getOrderItemId(), qty);
            shortage |= qty.compareTo(line.getQtyRequired()) < 0;
        }
        String action = shortage ? form.shortageAction() : null;
        boolean backorder = DeliveryNoteStatus.BACKORDER.equals(action);
        LocalDateTime dueUtc = backorder ? toUtc(parseDate(form.backorderDate()), form.backorderTime()) : null;
        String shortageNote = shortage ? form.shortageNote() : null;
        long warehouseId = draft.getWarehouseId();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PickListDao.PickHead head = pickListDao.lockPickList(connection, draft.getPickListId());
                if (head == null || !PickListStatus.DONE.equals(head.status())) {
                    throw new DeliveryException("Phiếu soạn " + draft.getPickNo()
                            + " chưa soạn xong hoặc đã huỷ, chưa xác nhận xuất kho được.");
                }
                if (!canUseWarehouse(actorUserId, warehouseId)) {
                    throw new DeliveryException("Bạn chỉ xác nhận xuất kho cho kho mình phụ trách.");
                }
                PickListDao.OrderLock order = pickListDao.lockOrders(connection, List.of(draft.getOrderId())).get(0);
                if (noteDao.findNoteId(connection, draft.getPickListId(), draft.getOrderId()) != null) {
                    throw new DeliveryException("Đơn " + order.orderNo() + " đã được xác nhận xuất kho; phiếu xuất"
                            + " đã xác nhận không sửa được.");
                }
                if (!OrderStatus.PICKING.equals(order.status()) && !OrderStatus.SHIPPED.equals(order.status())
                        && !OrderStatus.DELIVERED.equals(order.status())) {
                    throw new DeliveryException("Đơn " + order.orderNo() + " đang ở trạng thái \""
                            + OrderStatus.label(order.status()) + "\", không xuất kho được.");
                }
                // Chia số thực xuất của từng dòng đơn vào các lô đã soạn, lô hạn gần nhất trước
                Map<Long, List<DeliveryNoteDao.PickedLot>> allocations = allocate(
                        noteDao.findPickedLots(connection, draft.getPickListId(), draft.getOrderId()), issuedQty);
                // Khoá sổ tồn theo thứ tự SKU rồi tồn theo lô theo thứ tự lô (tránh khoá chéo)
                Map<Long, PickListDao.Balance> balances = new TreeMap<>();
                Map<Long, BigDecimal> lotOnHand = new TreeMap<>();
                for (DeliveryIssueDraft.Line line : draft.getLines()) {
                    balances.put(line.getProductId(), null);
                }
                for (List<DeliveryNoteDao.PickedLot> lots : allocations.values()) {
                    lots.forEach(lot -> lotOnHand.put(lot.lotId(), null));
                }
                for (Long productId : balances.keySet()) {
                    PickListDao.Balance balance = pickListDao.lockBalance(connection, warehouseId, productId);
                    if (balance == null) {
                        throw new DeliveryException("Sổ tồn chưa có SKU của đơn " + order.orderNo() + " tại kho.");
                    }
                    balances.put(productId, balance);
                }
                for (Long lotId : lotOnHand.keySet()) {
                    lotOnHand.put(lotId, receiptDao.lockLotBalance(connection, warehouseId, lotId));
                }
                String deliveryNo = noteDao.nextDeliveryNo(connection);
                long noteId = noteDao.insertNote(connection, deliveryNo, draft.getOrderId(), warehouseId,
                        draft.getPickListId(), action, dueUtc, shortageNote, actorUserId);
                Map<Long, BigDecimal> onHand = new HashMap<>();
                Map<Long, BigDecimal> reserved = new HashMap<>();
                balances.forEach((productId, balance) -> {
                    onHand.put(productId, balance.onHand());
                    reserved.put(productId, balance.reserved());
                });
                String movementNote = "Xuất kho theo phiếu " + deliveryNo + " (đơn " + order.orderNo() + ")";
                BigDecimal totalIssued = BigDecimal.ZERO;
                int shortLines = 0;
                for (DeliveryIssueDraft.Line line : draft.getLines()) {
                    long itemId = line.getOrderItemId();
                    long productId = line.getProductId();
                    BigDecimal issued = issuedQty.get(itemId);
                    BigDecimal shortQty = line.getQtyRequired().subtract(issued);
                    DeliveryNoteDao.Reservation reservation = noteDao.lockReservation(connection, itemId);
                    if (reservation != null) {
                        noteDao.markFulfilled(connection, reservation.id());
                        reserved.merge(productId, reservation.qty().negate(), BigDecimal::add);
                    }
                    if (shortQty.signum() > 0 && backorder) {
                        pickListDao.insertReservation(connection, itemId, warehouseId, productId, shortQty, actorUserId);
                        reserved.merge(productId, shortQty, BigDecimal::add);
                    }
                    for (DeliveryNoteDao.PickedLot lot : allocations.getOrDefault(itemId, List.of())) {
                        BigDecimal lotLeft = lotOnHand.get(lot.lotId()).subtract(lot.qtyPicked());
                        BigDecimal skuLeft = onHand.get(productId).subtract(lot.qtyPicked());
                        if (lotLeft.signum() < 0 || skuLeft.signum() < 0) {
                            throw new DeliveryException("SKU " + line.getSku() + ": lô " + lot.lotNo()
                                    + " không đủ tồn để xuất " + PickListService.plain(lot.qtyPicked()) + ".");
                        }
                        lotOnHand.put(lot.lotId(), lotLeft);
                        onHand.put(productId, skuLeft);
                        receiptDao.updateLotBalance(connection, warehouseId, lot.lotId(), lotLeft);
                        noteDao.insertItem(connection, noteId, itemId, productId, lot.lotId(), lot.qtyPicked());
                        noteDao.insertMovement(connection, warehouseId, productId, lot.lotId(), "ISSUE",
                                lot.qtyPicked().negate(), skuLeft, noteId, movementNote, actorUserId);
                    }
                    if (shortQty.signum() > 0) {
                        noteDao.insertShortage(connection, noteId, itemId, line.getQtyRequired(), issued,
                                form.lines().get(itemId).reason());
                        shortLines++;
                    }
                    noteDao.addShipped(connection, itemId, issued,
                            shortQty.signum() > 0 && !backorder ? shortQty : BigDecimal.ZERO, actorUserId);
                    totalIssued = totalIssued.add(issued);
                }
                for (Long productId : balances.keySet()) {
                    BigDecimal newReserved = reserved.get(productId);
                    if (newReserved.signum() < 0 || newReserved.compareTo(onHand.get(productId)) > 0) {
                        throw new DeliveryException("Sổ tồn của đơn " + order.orderNo() + " không khớp giữ chỗ;"
                                + " kiểm tra lại tồn kho trước khi xuất.");
                    }
                    noteDao.setBalance(connection, warehouseId, productId, onHand.get(productId), newReserved);
                }
                if (OrderStatus.PICKING.equals(order.status())) {
                    pickListDao.changeOrderStatus(connection, order.id(), OrderStatus.PICKING, OrderStatus.SHIPPED,
                            actorUserId);
                    salesOrderDao.insertStatusHistory(connection, order.id(), OrderStatus.PICKING, OrderStatus.SHIPPED,
                            "Xuất kho theo phiếu " + deliveryNo + (shortLines > 0 ? ", xuất thiếu " + shortLines
                                    + " dòng (" + DeliveryNoteStatus.shortageLabel(action).toLowerCase() + ")" : ""),
                            actorUserId);
                }
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("deliveryNo", deliveryNo);
                values.put("orderNo", order.orderNo());
                values.put("pickNo", draft.getPickNo());
                values.put("issuedQtyBase", totalIssued);
                values.put("shortLines", shortLines);
                if (action != null) {
                    values.put("shortageAction", action);
                }
                auditLogDao.insert(connection, actorUserId, "DELIVERY_NOTE_ISSUE", ENTITY, noteId, null,
                        JsonUtil.object(values), shortageNote, ipAddress);
                connection.commit();
                return new Issued(noteId, deliveryNo);
            } catch (SQLException | DeliveryException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Chia số thực xuất của mỗi dòng đơn vào các lô đã soạn theo thứ tự (FEFO); trả về lô và số xuất từ lô đó
    static Map<Long, List<DeliveryNoteDao.PickedLot>> allocate(List<DeliveryNoteDao.PickedLot> pickedLots,
                                                               Map<Long, BigDecimal> issuedQty)
            throws DeliveryException {
        Map<Long, BigDecimal> left = new HashMap<>(issuedQty);
        Map<Long, List<DeliveryNoteDao.PickedLot>> allocations = new LinkedHashMap<>();
        for (DeliveryNoteDao.PickedLot lot : pickedLots) {
            BigDecimal need = left.getOrDefault(lot.orderItemId(), BigDecimal.ZERO);
            BigDecimal take = need.min(lot.qtyPicked());
            if (take.signum() <= 0) {
                continue;
            }
            allocations.computeIfAbsent(lot.orderItemId(), key -> new ArrayList<>()).add(
                    new DeliveryNoteDao.PickedLot(lot.orderItemId(), lot.productId(), lot.lotId(), lot.lotNo(), take));
            left.put(lot.orderItemId(), need.subtract(take));
        }
        for (BigDecimal remaining : left.values()) {
            if (remaining.signum() > 0) {
                throw new DeliveryException("Số thực xuất vượt số đã soạn; tải lại trang rồi nhập lại.");
            }
        }
        return allocations;
    }

    static BigDecimal parseQty(String value) {
        return PickListService.parseQty(value);
    }

    private static LocalDate parseDate(String value) {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Ngày giờ giao bổ sung theo giờ Việt Nam đổi sang UTC để lưu
    static LocalDateTime toUtc(LocalDate date, String time) {
        LocalTime localTime = time == null ? DEFAULT_BACKORDER_TIME : LocalTime.parse(time);
        return date.atTime(localTime).atZone(DISPLAY_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
