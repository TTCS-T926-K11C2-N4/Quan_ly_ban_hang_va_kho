package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.StockReservationDao;
import com.oms.dao.SystemSettingDao;
import com.oms.model.PageResult;
import com.oms.model.Permission;
import com.oms.model.ReservationFilter;
import com.oms.model.ReservationOrderDetail;
import com.oms.model.ReservationOrderRow;
import com.oms.model.ReservationState;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Giữ chỗ tồn cho đơn đã duyệt (S5-06).
//  * reserve: gọi khi đơn chuyển sang Đã duyệt (S4-05), TRONG transaction duyệt đơn. Dòng đã giữ chỗ từ lúc gửi đơn
//    (S4-03, chưa có hạn) thì bắt đầu tính hạn giữ chỗ; dòng chưa giữ thì khoá sổ tồn, trừ tồn khả dụng
//    (cộng qty_reserved_base) và ghi stock_reservations. Thiếu tồn thì ném lỗi để transaction hoàn lại toàn bộ.
//  * Huỷ đơn nhả giữ chỗ ở OrderLifecycleService (SalesOrderDao.releaseReservations, S4-03).
//  * releaseExpired: ReservationExpiryJob gọi định kỳ, nhả giữ chỗ đã duyệt quá inventory.reservation_ttl_hours giờ
//    mà chưa xuất kho.
public class StockReservationService {

    public static final int PAGE_SIZE = 10;
    // Giữ chỗ tự nhả sau bao lâu nếu đơn chưa xuất kho (system_settings, mặc định 48 giờ)
    static final String HOLD_HOURS_SETTING = "inventory.reservation_ttl_hours";
    static final int DEFAULT_HOLD_HOURS = 48;
    private static final String AUDIT_ENTITY = "ORDER";

    private final StockReservationDao reservationDao = new StockReservationDao();
    private final SystemSettingDao settingDao = new SystemSettingDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Không đủ tồn khả dụng để giữ chỗ cho đơn; message liệt kê từng SKU thiếu
    public static class InsufficientStockException extends Exception {
        public InsufficientStockException(String message) {
            super(message);
        }
    }

    public record Summary(long orders, BigDecimal reservedQty) {
        public long getOrders() {
            return orders;
        }

        public BigDecimal getReservedQty() {
            return reservedQty;
        }
    }

    // Giữ chỗ mọi dòng hàng chưa giữ của đơn tại kho phục vụ của đơn và tính hạn giữ chỗ từ lúc duyệt cho mọi dòng
    // đang giữ. Trả về số dòng vừa giữ thêm (0 nếu đơn đã giữ đủ từ lúc gửi).
    // Gọi trong transaction đổi trạng thái đơn sang APPROVED; ném InsufficientStockException thì người gọi rollback.
    public int reserve(Connection connection, long orderId, Long actorUserId)
            throws SQLException, InsufficientStockException {
        StockReservationDao.OrderHead order = reservationDao.lockOrder(connection, orderId);
        if (order == null) {
            throw new IllegalArgumentException("Không có đơn " + orderId);
        }
        List<StockReservationDao.PendingItem> items = reservationDao.findPendingItems(connection, orderId);
        // Gộp theo SKU vì một đơn có thể có hai dòng cùng SKU; khoá theo thứ tự SKU (items đã sắp sẵn)
        Map<Long, BigDecimal> needed = new LinkedHashMap<>();
        Map<Long, String> skus = new LinkedHashMap<>();
        for (StockReservationDao.PendingItem item : items) {
            needed.merge(item.productId(), item.qtyBase(), BigDecimal::add);
            skus.put(item.productId(), item.sku());
        }
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : needed.entrySet()) {
            BigDecimal available = reservationDao.lockAvailable(connection, order.warehouseId(), entry.getKey());
            String shortage = shortage(skus.get(entry.getKey()), order.warehouseCode(), entry.getValue(), available);
            if (shortage != null) {
                shortages.add(shortage);
            }
        }
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException("Không đủ tồn khả dụng để giữ chỗ cho đơn " + order.orderNo() + ": "
                    + String.join("; ", shortages) + ".");
        }
        int holdHours = holdHours();
        reservationDao.startHoldClock(connection, orderId, holdHours);
        for (StockReservationDao.PendingItem item : items) {
            reservationDao.addReserved(connection, order.warehouseId(), item.productId(), item.qtyBase());
            reservationDao.insertReservation(connection, item, order.warehouseId(), holdHours, actorUserId);
        }
        return items.size();
    }

    // null nếu đủ tồn; ngược lại là mô tả phần thiếu, vd "SP011 tại KHO-TT cần 24, khả dụng 10"
    static String shortage(String sku, String warehouseCode, BigDecimal needed, BigDecimal available) {
        BigDecimal have = available == null ? BigDecimal.ZERO : available;
        if (have.compareTo(needed) >= 0) {
            return null;
        }
        return sku + " tại " + warehouseCode + " cần " + needed.stripTrailingZeros().toPlainString() + ", khả dụng "
                + have.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString();
    }

    private int release(Connection connection, List<StockReservationDao.ActiveReservation> reservations,
                        String reason) throws SQLException {
        int released = 0;
        for (StockReservationDao.ActiveReservation reservation : reservations) {
            if (reservationDao.markReleased(connection, reservation.id(), reason)) {
                reservationDao.addReserved(connection, reservation.warehouseId(), reservation.productId(),
                        reservation.qtyBase().negate());
                released++;
            }
        }
        return released;
    }

    // Nhả các giữ chỗ đã quá hạn, mỗi đơn một transaction và một dòng nhật ký (người thực hiện: hệ thống).
    // Trả về số đơn đã nhả.
    public int releaseExpired() throws SQLException {
        int orders = 0;
        for (Long orderId : reservationDao.findOrdersWithExpired()) {
            try (Connection connection = DbConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    StockReservationDao.OrderHead order = reservationDao.lockOrder(connection, orderId);
                    int released = release(connection, reservationDao.lockExpired(connection, orderId),
                            ReservationState.REASON_EXPIRED);
                    if (released > 0) {
                        Map<String, Object> values = new LinkedHashMap<>();
                        values.put("orderNo", order.orderNo());
                        values.put("releasedReservations", released);
                        auditLogDao.insert(connection, null, "ORDER_RESERVATION_EXPIRE", AUDIT_ENTITY, orderId, null,
                                JsonUtil.object(values), "Quá " + holdHours() + " giờ chưa xuất kho", null);
                        orders++;
                    }
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            }
        }
        return orders;
    }

    int holdHours() throws SQLException {
        int hours = settingDao.getInt(HOLD_HOURS_SETTING, DEFAULT_HOLD_HOURS);
        return hours > 0 ? hours : DEFAULT_HOLD_HOURS;
    }

    // ----- Màn danh sách đơn đang giữ chỗ và chi tiết -----

    // Phạm vi xem theo ORDER_VIEW: ALL xem mọi đơn; ASSIGNED (nhân viên kinh doanh) chỉ đơn của đại lý mình phụ trách
    // (salesRepId là người cần lọc, null khi xem mọi đơn); không có quyền thì không thấy đơn nào
    private record Scope(boolean allowed, Long salesRepId) {
    }

    private Scope scopeOf(long userId) throws SQLException {
        String scope = permissionDao.findScope(userId, Permission.ORDER_VIEW);
        return new Scope("ALL".equals(scope) || "ASSIGNED".equals(scope), "ASSIGNED".equals(scope) ? userId : null);
    }

    public PageResult<ReservationOrderRow> search(long userId, ReservationFilter filter, int requestedPage)
            throws SQLException {
        Scope scope = scopeOf(userId);
        if (!scope.allowed()) {
            return new PageResult<>(List.of(), 1, PAGE_SIZE, 0);
        }
        long total = reservationDao.countOrders(filter, scope.salesRepId());
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        return new PageResult<>(reservationDao.findOrders(filter, scope.salesRepId(), (page - 1) * PAGE_SIZE,
                PAGE_SIZE), page, PAGE_SIZE, total);
    }

    public Summary summary(long userId, ReservationFilter filter) throws SQLException {
        Scope scope = scopeOf(userId);
        if (!scope.allowed()) {
            return new Summary(0, BigDecimal.ZERO);
        }
        BigDecimal[] values = reservationDao.summarize(filter, scope.salesRepId());
        return new Summary(values[0].longValue(), values[1]);
    }

    // null nếu không có đơn hoặc người dùng không được xem đơn này (trả 404 như nhau)
    public ReservationOrderDetail findVisible(long userId, long orderId) throws SQLException {
        Scope scope = scopeOf(userId);
        StockReservationDao.OrderOwner owner = reservationDao.findOrderOwner(orderId);
        if (!scope.allowed() || owner == null
                || (scope.salesRepId() != null && !scope.salesRepId().equals(owner.salesRepId()))) {
            return null;
        }
        return reservationDao.findDetail(orderId);
    }

    public String getProductWarehouseLabel(ReservationFilter filter) throws SQLException {
        return filter.getProductId() == null ? null
                : reservationDao.findProductWarehouseLabel(filter.getProductId(), filter.getWarehouseId());
    }

    public int getHoldHours() throws SQLException {
        return holdHours();
    }
}
