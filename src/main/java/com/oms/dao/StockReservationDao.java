package com.oms.dao;

import com.oms.model.ReservationFilter;
import com.oms.model.ReservationOrderDetail;
import com.oms.model.ReservationOrderRow;
import com.oms.model.ReservationState;
import com.oms.util.DbConnection;
import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Giữ chỗ tồn cho đơn đã duyệt (stock_reservations, S5-06). Tổng các dòng ACTIVE theo (kho, SKU) luôn bằng
// stock_balances.qty_reserved_base: mỗi lần giữ / nhả cập nhật cả hai trong cùng transaction do service mở.
public class StockReservationDao {

    // Dòng hàng cần giữ chỗ của một đơn (chưa có giữ chỗ ACTIVE)
    public record PendingItem(long itemId, long productId, String sku, BigDecimal qtyBase) {
    }

    // Một giữ chỗ đang ACTIVE (để nhả)
    public record ActiveReservation(long id, long warehouseId, long productId, BigDecimal qtyBase) {
    }

    // Kho phục vụ và mã đơn; null nếu không có đơn
    public record OrderHead(long warehouseId, String warehouseCode, String orderNo, String status) {
    }

    public OrderHead lockOrder(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT o.warehouse_id, w.code, o.order_no, o.status FROM sales_orders o"
                + " JOIN warehouses w ON w.id = o.warehouse_id WHERE o.id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new OrderHead(resultSet.getLong(1), resultSet.getString(2),
                        resultSet.getString(3), resultSet.getString(4)) : null;
            }
        }
    }

    // Theo thứ tự SKU để mọi giao dịch khoá sổ tồn cùng một thứ tự (tránh khoá chéo)
    public List<PendingItem> findPendingItems(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT i.id, i.product_id, p.sku, i.qty_base FROM sales_order_items i"
                + " JOIN products p ON p.id = i.product_id WHERE i.order_id = ?"
                + " AND NOT EXISTS (SELECT 1 FROM stock_reservations r WHERE r.order_item_id = i.id AND r.status = 'ACTIVE')"
                + " ORDER BY i.product_id, i.id";
        List<PendingItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new PendingItem(resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3),
                            resultSet.getBigDecimal(4)));
                }
            }
        }
        return items;
    }

    // Khoá dòng sổ tồn (kho, SKU); trả về tồn khả dụng, null nếu SKU chưa từng có tồn ở kho này
    public BigDecimal lockAvailable(Connection connection, long warehouseId, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT qty_available_base FROM stock_balances"
                + " WHERE warehouse_id = ? AND product_id = ? FOR UPDATE")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal(1) : null;
            }
        }
    }

    // delta dương: giữ thêm; âm: nhả. CHECK của bảng chặn giữ chỗ vượt tồn thực tế hoặc âm.
    public void addReserved(Connection connection, long warehouseId, long productId, BigDecimal delta)
            throws SQLException {
        String sql = "UPDATE stock_balances SET qty_reserved_base = qty_reserved_base + ?, version = version + 1,"
                + " updated_at = UTC_TIMESTAMP() WHERE warehouse_id = ? AND product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, delta);
            statement.setLong(2, warehouseId);
            statement.setLong(3, productId);
            statement.executeUpdate();
        }
    }

    public void insertReservation(Connection connection, PendingItem item, long warehouseId, int holdHours,
                                  Long actorUserId) throws SQLException {
        String sql = "INSERT INTO stock_reservations (order_item_id, warehouse_id, product_id, qty_base, status,"
                + " reserved_at, expires_at, created_at, created_by)"
                + " VALUES (?, ?, ?, ?, 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP() + INTERVAL ? HOUR, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, item.itemId());
            statement.setLong(2, warehouseId);
            statement.setLong(3, item.productId());
            statement.setBigDecimal(4, item.qtyBase());
            statement.setInt(5, holdHours);
            if (actorUserId == null) {
                statement.setNull(6, Types.BIGINT);
            } else {
                statement.setLong(6, actorUserId);
            }
            statement.executeUpdate();
        }
    }

    // Giữ chỗ tạo lúc gửi đơn (S4-03) chưa có hạn: bắt đầu tính hạn từ lúc duyệt. Trả về số dòng được đặt hạn.
    public int startHoldClock(Connection connection, long orderId, int holdHours) throws SQLException {
        String sql = "UPDATE stock_reservations r JOIN sales_order_items i ON i.id = r.order_item_id"
                + " SET r.expires_at = UTC_TIMESTAMP() + INTERVAL ? HOUR"
                + " WHERE i.order_id = ? AND r.status = 'ACTIVE' AND r.expires_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, holdHours);
            statement.setLong(2, orderId);
            return statement.executeUpdate();
        }
    }

    // Khoá các giữ chỗ ACTIVE đã quá hạn của đơn
    public List<ActiveReservation> lockExpired(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT r.id, r.warehouse_id, r.product_id, r.qty_base FROM stock_reservations r"
                + " JOIN sales_order_items i ON i.id = r.order_item_id WHERE i.order_id = ? AND r.status = 'ACTIVE'"
                + " AND r.expires_at <= UTC_TIMESTAMP() ORDER BY r.warehouse_id, r.product_id, r.id FOR UPDATE";
        List<ActiveReservation> reservations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reservations.add(new ActiveReservation(resultSet.getLong(1), resultSet.getLong(2),
                            resultSet.getLong(3), resultSet.getBigDecimal(4)));
                }
            }
        }
        return reservations;
    }

    // false nếu giữ chỗ không còn ACTIVE (đã được nhả ở nơi khác)
    public boolean markReleased(Connection connection, long reservationId, String reason) throws SQLException {
        String sql = "UPDATE stock_reservations SET status = 'RELEASED', released_at = UTC_TIMESTAMP(),"
                + " release_reason = ? WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reason);
            statement.setLong(2, reservationId);
            return statement.executeUpdate() == 1;
        }
    }

    // Đơn có giữ chỗ đã quá hạn mà chưa nhả
    public List<Long> findOrdersWithExpired() throws SQLException {
        String sql = "SELECT DISTINCT i.order_id FROM stock_reservations r JOIN sales_order_items i ON i.id = r.order_item_id"
                + " WHERE r.status = 'ACTIVE' AND r.expires_at <= UTC_TIMESTAMP() ORDER BY i.order_id";
        List<Long> ids = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getLong(1));
            }
        }
        return ids;
    }

    // ----- Màn danh sách đơn đang giữ chỗ và chi tiết đơn -----

    // Mỗi đơn một dòng, gộp các giữ chỗ của đơn (hoặc chỉ của SKU tại kho đang lọc)
    private static final String ORDER_ROWS_SQL = "SELECT o.id, o.order_no, c.name AS customer_name,"
            + " c.code AS customer_code, o.approved_at, o.status,"
            + " COALESCE(SUM(CASE WHEN r.status = 'ACTIVE' THEN r.qty_base END), 0) AS reserved_qty,"
            + " COUNT(DISTINCT i.id) AS line_count,"
            + " COUNT(DISTINCT CASE WHEN r.status = 'ACTIVE' THEN i.id END) AS active_lines,"
            + " COUNT(DISTINCT CASE WHEN r.status = 'RELEASED' AND r.release_reason = 'EXPIRED' THEN i.id END)"
            + " AS expired_lines,"
            + " COUNT(DISTINCT CASE WHEN r.status = 'RELEASED' THEN i.id END) AS released_lines,"
            + " COUNT(DISTINCT CASE WHEN r.status = 'FULFILLED' THEN i.id END) AS fulfilled_lines"
            + " FROM sales_orders o JOIN customers c ON c.id = o.customer_id"
            + " JOIN sales_order_items i ON i.order_id = o.id JOIN stock_reservations r ON r.order_item_id = i.id"
            + " WHERE (? = '' OR o.order_no LIKE ? OR c.name LIKE ? OR c.code LIKE ?)"
            + " AND (? IS NULL OR o.status = ?)"
            + " AND (? IS NULL OR (o.approved_at >= ? AND o.approved_at < ?))"
            + " AND (? IS NULL OR r.product_id = ?) AND (? IS NULL OR r.warehouse_id = ?)"
            + " AND (? IS NULL OR c.sales_rep_id = ?)"
            + " GROUP BY o.id, o.order_no, c.name, c.code, o.approved_at, o.status";

    // salesRepId: chỉ đơn của đại lý người này phụ trách (phạm vi ASSIGNED); null là mọi đơn
    public long countOrders(ReservationFilter filter, Long salesRepId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM (" + ORDER_ROWS_SQL + ") t")) {
            bindFilter(statement, filter, salesRepId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // {số đơn đang giữ chỗ, tổng số lượng đang giữ} theo bộ lọc
    public BigDecimal[] summarize(ReservationFilter filter, Long salesRepId) throws SQLException {
        String sql = "SELECT COUNT(*), COALESCE(SUM(reserved_qty), 0) FROM (" + ORDER_ROWS_SQL + ") t WHERE active_lines > 0";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindFilter(statement, filter, salesRepId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new BigDecimal[] {resultSet.getBigDecimal(1), resultSet.getBigDecimal(2)};
            }
        }
    }

    // Đơn duyệt mới nhất trước
    public List<ReservationOrderRow> findOrders(ReservationFilter filter, Long salesRepId, int offset, int limit)
            throws SQLException {
        String sql = ORDER_ROWS_SQL + " ORDER BY o.approved_at DESC, o.id DESC LIMIT ? OFFSET ?";
        List<ReservationOrderRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindFilter(statement, filter, salesRepId);
            statement.setInt(next++, limit);
            statement.setInt(next, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ReservationState state = ReservationState.of(resultSet.getInt("line_count"),
                            resultSet.getInt("active_lines"), resultSet.getInt("expired_lines"),
                            resultSet.getInt("released_lines"), resultSet.getInt("fulfilled_lines"));
                    rows.add(new ReservationOrderRow(resultSet.getLong("id"), resultSet.getString("order_no"),
                            resultSet.getString("customer_name"), resultSet.getString("customer_code"),
                            resultSet.getBigDecimal("reserved_qty"),
                            resultSet.getObject("approved_at", LocalDateTime.class), resultSet.getString("status"),
                            state));
                }
            }
        }
        return rows;
    }

    // Nhân viên kinh doanh phụ trách đại lý của đơn (null nếu đại lý chưa có người phụ trách)
    public record OrderOwner(Long salesRepId) {
    }

    // Để kiểm phạm vi xem đơn; null nếu không có đơn
    public OrderOwner findOrderOwner(long orderId) throws SQLException {
        String sql = "SELECT c.sales_rep_id FROM sales_orders o JOIN customers c ON c.id = o.customer_id WHERE o.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new OrderOwner(resultSet.getObject(1, Long.class)) : null;
            }
        }
    }

    // Mỗi dòng hàng lấy giữ chỗ mới nhất (ACTIVE trước) của dòng đó; dòng chưa giữ chỗ hiện kho phục vụ của đơn
    public ReservationOrderDetail findDetail(long orderId) throws SQLException {
        String headerSql = "SELECT o.id, o.order_no, o.status, o.approved_at, c.name, c.code FROM sales_orders o"
                + " JOIN customers c ON c.id = o.customer_id WHERE o.id = ?";
        String lineSql = "SELECT p.sku, p.name AS product_name, u.name AS unit_name, w.code AS warehouse_code,"
                + " w.name AS warehouse_name, i.qty_base, r.qty_base AS reserved_qty, r.status, r.release_reason,"
                + " r.expires_at FROM sales_order_items i JOIN sales_orders o ON o.id = i.order_id"
                + " JOIN products p ON p.id = i.product_id JOIN units u ON u.id = p.base_unit_id"
                + " LEFT JOIN stock_reservations r ON r.id = (SELECT r2.id FROM stock_reservations r2"
                + " WHERE r2.order_item_id = i.id ORDER BY r2.status = 'ACTIVE' DESC, r2.id DESC LIMIT 1)"
                + " JOIN warehouses w ON w.id = COALESCE(r.warehouse_id, o.warehouse_id)"
                + " WHERE i.order_id = ? ORDER BY i.line_no";
        try (Connection connection = DbConnection.getConnection()) {
            List<ReservationOrderDetail.Line> lines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(lineSql)) {
                statement.setLong(1, orderId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String status = resultSet.getString("status");
                        BigDecimal reserved = resultSet.getBigDecimal("reserved_qty");
                        lines.add(new ReservationOrderDetail.Line(resultSet.getString("sku"),
                                resultSet.getString("product_name"), resultSet.getString("unit_name"),
                                resultSet.getString("warehouse_code"), resultSet.getString("warehouse_name"),
                                resultSet.getBigDecimal("qty_base"), reserved == null ? BigDecimal.ZERO : reserved,
                                ReservationState.ofLine(status, resultSet.getString("release_reason")),
                                resultSet.getObject("expires_at", LocalDateTime.class)));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
                statement.setLong(1, orderId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return new ReservationOrderDetail(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(3), resultSet.getObject(4, LocalDateTime.class), resultSet.getString(5),
                            resultSet.getString(6), lines);
                }
            }
        }
    }

    // Tên SKU và kho đang lọc (hiện ở thẻ lọc khi mở từ sổ tồn kho); null nếu không có
    public String findProductWarehouseLabel(long productId, Long warehouseId) throws SQLException {
        String sql = "SELECT CONCAT(p.sku, ' - ', p.name, COALESCE(CONCAT(' tại ', w.name), ''))"
                + " FROM products p LEFT JOIN warehouses w ON w.id = ? WHERE p.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableLong(statement, 1, warehouseId);
            statement.setLong(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private static int bindFilter(PreparedStatement statement, ReservationFilter filter, Long salesRepId)
            throws SQLException {
        String keyword = filter.getKeyword();
        String like = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        statement.setString(1, keyword);
        statement.setString(2, like);
        statement.setString(3, like);
        statement.setString(4, like);
        statement.setString(5, filter.getOrderStatus());
        statement.setString(6, filter.getOrderStatus());
        // Ngày duyệt theo giờ Việt Nam, so với approved_at lưu UTC
        if (filter.getApprovedDate() == null) {
            statement.setNull(7, Types.TIMESTAMP);
            statement.setNull(8, Types.TIMESTAMP);
            statement.setNull(9, Types.TIMESTAMP);
        } else {
            Timestamp from = Timestamp.valueOf(DateTimeUtil.startOfDayUtc(filter.getApprovedDate()));
            statement.setTimestamp(7, from);
            statement.setTimestamp(8, from);
            statement.setTimestamp(9, Timestamp.valueOf(DateTimeUtil.startOfDayUtc(filter.getApprovedDate().plusDays(1))));
        }
        setNullableLong(statement, 10, filter.getProductId());
        setNullableLong(statement, 11, filter.getProductId());
        setNullableLong(statement, 12, filter.getWarehouseId());
        setNullableLong(statement, 13, filter.getWarehouseId());
        setNullableLong(statement, 14, salesRepId);
        setNullableLong(statement, 15, salesRepId);
        return 16;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
