package com.oms.dao;

import com.oms.model.PickListDetail;
import com.oms.model.PickListFilter;
import com.oms.model.PickListRow;
import com.oms.model.PickPendingOrder;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Phiếu soạn hàng (pick_lists, pick_list_items, S6-03): danh sách đơn chờ soạn, đơn còn nợ hàng, phiếu soạn;
// lập phiếu (cấp mã, giữ chỗ còn thiếu, chọn lô hạn gần nhất), ghi nhận số thực soạn. Các hàm nhận Connection
// chạy trong transaction do PickListService mở.
public class PickListDao {

    private static final String DOC_TYPE = "PICK_LIST";
    private static final String NO_PREFIX = "PSH-";

    // Hàng còn phải giao của một dòng đơn (đơn vị cơ sở)
    static final String REMAINING = "(i.qty_base - i.qty_shipped_base - i.qty_closed_base)";

    // Đơn o đang nằm trong một phiếu soạn chưa huỷ mà chưa xác nhận xuất kho
    static final String OPEN_PICK = "SELECT 1 FROM pick_list_items opi JOIN pick_lists op ON op.id = opi.pick_list_id"
            + " JOIN sales_order_items ooi ON ooi.id = opi.order_item_id WHERE ooi.order_id = o.id"
            + " AND op.status <> 'CANCELLED'"
            + " AND NOT EXISTS (SELECT 1 FROM delivery_notes od WHERE od.pick_list_id = op.id AND od.order_id = o.id)";

    private static final String ORDER_FROM = " FROM sales_orders o JOIN customers c ON c.id = o.customer_id"
            + " JOIN warehouses w ON w.id = o.warehouse_id";

    // Đơn trong lượt lập phiếu (đã khoá)
    public record OrderLock(long id, String orderNo, String status, long warehouseId, String warehouseCode,
                            Long salesRepId) {
    }

    // Dòng đơn còn hàng phải giao, kèm giữ chỗ đang hiệu lực (nếu có)
    public record OpenItem(long id, long productId, String sku, BigDecimal remaining, Long reservationId,
                           BigDecimal reservedQty) {
    }

    // Dòng sổ tồn (kho, SKU) đã khoá
    public record Balance(BigDecimal onHand, BigDecimal reserved, Long locationId) {
        public BigDecimal available() {
            return onHand.subtract(reserved);
        }
    }

    // Lô còn lấy được: tồn của lô trừ phần đã chia cho phiếu soạn khác chưa xuất kho
    public record LotStock(long lotId, String lotNo, LocalDate expiryDate, BigDecimal free) {
    }

    public record PickHead(String pickNo, String status, long warehouseId) {
    }

    public record PickLine(long id, BigDecimal qtyRequired) {
    }

    // ----- Màn danh sách -----

    // Đơn đã duyệt còn hàng phải giao, chưa nằm trong phiếu soạn nào đang mở; ngày giao gần nhất trước
    public List<PickPendingOrder> findPendingOrders(PickListFilter filter, Long salesRepId, LocalDate today)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT o.id, o.order_no, c.name AS customer_name, o.warehouse_id,"
                + " w.name AS warehouse_name, o.requested_delivery_date" + ORDER_FROM
                + " WHERE o.status = 'APPROVED' AND EXISTS (SELECT 1 FROM sales_order_items i WHERE i.order_id = o.id"
                + " AND " + REMAINING + " > 0) AND NOT EXISTS (" + OPEN_PICK + ")");
        appendOrderFilter(sql, params, filter, salesRepId);
        if (filter.getDue() != null) {
            switch (filter.getDue()) {
                case PickListFilter.DUE_OVERDUE -> sql.append(" AND o.requested_delivery_date < ?");
                case PickListFilter.DUE_TODAY -> sql.append(" AND o.requested_delivery_date = ?");
                default -> sql.append(" AND (o.requested_delivery_date > ? OR o.requested_delivery_date IS NULL)");
            }
            params.add(Date.valueOf(today));
        }
        sql.append(" ORDER BY o.requested_delivery_date IS NULL, o.requested_delivery_date, o.approved_at, o.id");
        List<PickPendingOrder> orders = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(new PickPendingOrder(resultSet.getLong("id"), resultSet.getString("order_no"),
                            resultSet.getString("customer_name"), resultSet.getLong("warehouse_id"),
                            resultSet.getString("warehouse_name"),
                            resultSet.getObject("requested_delivery_date", LocalDate.class), false, null, null, null,
                            null));
                }
            }
        }
        return orders;
    }

    // Đơn đã xuất kho nhưng còn nợ hàng (xuất thiếu, chọn giao bổ sung sau): hiện riêng, lịch giao bổ sung gần nhất trước
    public List<PickPendingOrder> findBackorders(PickListFilter filter, Long salesRepId) throws SQLException {
        String lastNote = "FROM delivery_notes d WHERE d.order_id = o.id AND d.shortage_action = 'BACKORDER'"
                + " ORDER BY d.id DESC LIMIT 1";
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT o.id, o.order_no, c.name AS customer_name, o.warehouse_id,"
                + " w.name AS warehouse_name, o.requested_delivery_date,"
                + " (SELECT SUM(" + REMAINING + ") FROM sales_order_items i WHERE i.order_id = o.id) AS remaining,"
                + " (SELECT d.backorder_due_at " + lastNote + ") AS due_at,"
                + " (SELECT d.delivery_no " + lastNote + ") AS delivery_no,"
                + " (SELECT op.pick_no FROM pick_list_items opi JOIN pick_lists op ON op.id = opi.pick_list_id"
                + " JOIN sales_order_items ooi ON ooi.id = opi.order_item_id WHERE ooi.order_id = o.id"
                + " AND op.status <> 'CANCELLED' AND NOT EXISTS (SELECT 1 FROM delivery_notes od"
                + " WHERE od.pick_list_id = op.id AND od.order_id = o.id) LIMIT 1) AS open_pick_no" + ORDER_FROM
                + " WHERE o.status IN ('SHIPPED', 'DELIVERED') AND EXISTS (SELECT 1 FROM sales_order_items i"
                + " WHERE i.order_id = o.id AND " + REMAINING + " > 0)");
        appendOrderFilter(sql, params, filter, salesRepId);
        sql.append(" ORDER BY due_at IS NULL, due_at, o.id");
        List<PickPendingOrder> orders = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(new PickPendingOrder(resultSet.getLong("id"), resultSet.getString("order_no"),
                            resultSet.getString("customer_name"), resultSet.getLong("warehouse_id"),
                            resultSet.getString("warehouse_name"),
                            resultSet.getObject("requested_delivery_date", LocalDate.class), true,
                            resultSet.getBigDecimal("remaining"), resultSet.getObject("due_at", LocalDateTime.class),
                            resultSet.getString("delivery_no"), resultSet.getString("open_pick_no")));
                }
            }
        }
        return orders;
    }

    private static void appendOrderFilter(StringBuilder sql, List<Object> params, PickListFilter filter,
                                          Long salesRepId) {
        if (!filter.getKeyword().isEmpty()) {
            sql.append(" AND (o.order_no LIKE ? OR c.name LIKE ? OR c.code LIKE ? OR w.name LIKE ? OR w.code LIKE ?)");
            addLike(params, filter.getKeyword(), 5);
        }
        if (filter.getWarehouseId() != null) {
            sql.append(" AND o.warehouse_id = ?");
            params.add(filter.getWarehouseId());
        }
        if (filter.getDeliveryDate() != null) {
            sql.append(" AND o.requested_delivery_date = ?");
            params.add(Date.valueOf(filter.getDeliveryDate()));
        }
        if (salesRepId != null) {
            sql.append(" AND c.sales_rep_id = ?");
            params.add(salesRepId);
        }
    }

    public long countPickLists(PickListFilter filter, Long salesRepId) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM pick_lists p JOIN warehouses w ON w.id = p.warehouse_id"
                + pickWhere(filter, salesRepId, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // Phiếu mới nhất trước
    public List<PickListRow> findPickLists(PickListFilter filter, Long salesRepId, int offset, int limit)
            throws SQLException {
        String itemOrders = " FROM pick_list_items si JOIN sales_order_items soi ON soi.id = si.order_item_id"
                + " JOIN sales_orders so ON so.id = soi.order_id WHERE si.pick_list_id = p.id";
        List<Object> params = new ArrayList<>();
        String sql = "SELECT p.id, p.pick_no, w.name AS warehouse_name, p.status, p.created_at,"
                + " (SELECT GROUP_CONCAT(DISTINCT so.order_no ORDER BY so.order_no SEPARATOR ', ')" + itemOrders
                + ") AS order_nos, (SELECT MIN(so.requested_delivery_date)" + itemOrders + ") AS delivery_date,"
                + " (SELECT COUNT(DISTINCT so.id)" + itemOrders + ") AS order_count,"
                + " (SELECT COUNT(*) FROM delivery_notes d WHERE d.pick_list_id = p.id) AS issued_count"
                + " FROM pick_lists p JOIN warehouses w ON w.id = p.warehouse_id" + pickWhere(filter, salesRepId, params)
                + " ORDER BY p.id DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        List<PickListRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new PickListRow(resultSet.getLong("id"), resultSet.getString("pick_no"),
                            resultSet.getString("order_nos"), resultSet.getString("warehouse_name"),
                            resultSet.getObject("delivery_date", LocalDate.class), resultSet.getString("status"),
                            resultSet.getInt("order_count"), resultSet.getInt("issued_count"),
                            resultSet.getObject("created_at", LocalDateTime.class)));
                }
            }
        }
        return rows;
    }

    private static String pickWhere(PickListFilter filter, Long salesRepId, List<Object> params) {
        String orderOf = "SELECT 1 FROM pick_list_items fi JOIN sales_order_items foi ON foi.id = fi.order_item_id"
                + " JOIN sales_orders fo ON fo.id = foi.order_id JOIN customers fc ON fc.id = fo.customer_id"
                + " WHERE fi.pick_list_id = p.id AND ";
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (!filter.getKeyword().isEmpty()) {
            where.append(" AND (p.pick_no LIKE ? OR w.name LIKE ? OR w.code LIKE ? OR EXISTS (").append(orderOf)
                    .append("(fo.order_no LIKE ? OR fc.name LIKE ?)))");
            addLike(params, filter.getKeyword(), 5);
        }
        if (filter.getWarehouseId() != null) {
            where.append(" AND p.warehouse_id = ?");
            params.add(filter.getWarehouseId());
        }
        if (filter.getStatus() != null) {
            where.append(" AND p.status = ?");
            params.add(filter.getStatus());
        }
        if (filter.getDeliveryDate() != null) {
            where.append(" AND EXISTS (").append(orderOf).append("fo.requested_delivery_date = ?)");
            params.add(Date.valueOf(filter.getDeliveryDate()));
        }
        if (salesRepId != null) {
            where.append(" AND EXISTS (").append(orderOf).append("fc.sales_rep_id = ?)");
            params.add(salesRepId);
        }
        return where.toString();
    }

    // Nhân viên kinh doanh chỉ xem phiếu có đơn của đại lý mình phụ trách
    public boolean hasOrderOfSalesRep(long pickListId, long salesRepId) throws SQLException {
        String sql = "SELECT 1 FROM pick_list_items pi JOIN sales_order_items oi ON oi.id = pi.order_item_id"
                + " JOIN sales_orders o ON o.id = oi.order_id JOIN customers c ON c.id = o.customer_id"
                + " WHERE pi.pick_list_id = ? AND c.sales_rep_id = ? LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pickListId);
            statement.setLong(2, salesRepId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public PickListDetail findDetail(long id) throws SQLException {
        String headerSql = "SELECT p.id, p.pick_no, p.warehouse_id, w.code AS warehouse_code, w.name AS warehouse_name,"
                + " p.status, au.full_name AS assigned_name, cu.full_name AS created_by_name, p.created_at,"
                + " p.completed_at FROM pick_lists p JOIN warehouses w ON w.id = p.warehouse_id"
                + " LEFT JOIN users au ON au.id = p.assigned_to LEFT JOIN users cu ON cu.id = p.created_by WHERE p.id = ?";
        String orderSql = "SELECT DISTINCT o.id, o.order_no, c.name AS customer_name, o.requested_delivery_date,"
                + " d.id AS note_id, d.delivery_no FROM pick_list_items pi"
                + " JOIN sales_order_items oi ON oi.id = pi.order_item_id JOIN sales_orders o ON o.id = oi.order_id"
                + " JOIN customers c ON c.id = o.customer_id"
                + " LEFT JOIN delivery_notes d ON d.pick_list_id = pi.pick_list_id AND d.order_id = o.id"
                + " WHERE pi.pick_list_id = ? ORDER BY o.order_no";
        // Sắp theo đường đi: thứ tự đi soạn của vị trí, dòng chưa có vị trí để cuối
        String lineSql = "SELECT pi.id, o.id AS order_id, o.order_no, p.sku, p.name AS product_name,"
                + " bu.name AS unit_name, lo.lot_no, lo.is_default, lo.expiry_date, l.code AS location_code,"
                + " l.name AS location_name, pi.qty_required_base, pi.qty_picked_base, pi.short_reason"
                + " FROM pick_list_items pi JOIN products p ON p.id = pi.product_id JOIN units bu ON bu.id = p.base_unit_id"
                + " JOIN lots lo ON lo.id = pi.lot_id LEFT JOIN warehouse_locations l ON l.id = pi.location_id"
                + " JOIN sales_order_items oi ON oi.id = pi.order_item_id JOIN sales_orders o ON o.id = oi.order_id"
                + " WHERE pi.pick_list_id = ? ORDER BY l.id IS NULL, l.pick_sequence, l.code, p.sku,"
                + " lo.expiry_date IS NULL, lo.expiry_date, o.order_no, pi.id";
        try (Connection connection = DbConnection.getConnection()) {
            List<PickListDetail.Order> orders = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(orderSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        orders.add(new PickListDetail.Order(resultSet.getLong("id"), resultSet.getString("order_no"),
                                resultSet.getString("customer_name"),
                                resultSet.getObject("requested_delivery_date", LocalDate.class),
                                resultSet.getObject("note_id", Long.class), resultSet.getString("delivery_no")));
                    }
                }
            }
            List<PickListDetail.Line> lines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(lineSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        lines.add(new PickListDetail.Line(resultSet.getLong("id"), resultSet.getLong("order_id"),
                                resultSet.getString("order_no"), resultSet.getString("sku"),
                                resultSet.getString("product_name"), resultSet.getString("unit_name"),
                                resultSet.getString("lot_no"), resultSet.getBoolean("is_default"),
                                resultSet.getObject("expiry_date", LocalDate.class), resultSet.getString("location_code"),
                                resultSet.getString("location_name"), resultSet.getBigDecimal("qty_required_base"),
                                resultSet.getBigDecimal("qty_picked_base"), resultSet.getString("short_reason")));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return new PickListDetail(resultSet.getLong("id"), resultSet.getString("pick_no"),
                            resultSet.getLong("warehouse_id"), resultSet.getString("warehouse_code"),
                            resultSet.getString("warehouse_name"), resultSet.getString("status"),
                            resultSet.getString("assigned_name"), resultSet.getString("created_by_name"),
                            resultSet.getObject("created_at", LocalDateTime.class),
                            resultSet.getObject("completed_at", LocalDateTime.class), orders, lines);
                }
            }
        }
    }

    // ----- Lập phiếu soạn -----

    // Khoá các đơn theo thứ tự id (tránh khoá chéo khi hai người cùng lập phiếu)
    public List<OrderLock> lockOrders(Connection connection, List<Long> orderIds) throws SQLException {
        if (orderIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT o.id, o.order_no, o.status, o.warehouse_id, w.code, c.sales_rep_id" + ORDER_FROM
                + " WHERE o.id IN (" + String.join(", ", Collections.nCopies(orderIds.size(), "?"))
                + ") ORDER BY o.id FOR UPDATE";
        List<OrderLock> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < orderIds.size(); i++) {
                statement.setLong(i + 1, orderIds.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(new OrderLock(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
                            resultSet.getLong(4), resultSet.getString(5), resultSet.getObject(6, Long.class)));
                }
            }
        }
        return orders;
    }

    public boolean hasOpenPick(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT EXISTS (" + OPEN_PICK.replace("o.id", "?") + ")")) {
            statement.setLong(1, orderId);
            statement.setLong(2, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    // Dòng đơn còn hàng phải giao, theo thứ tự SKU (mọi giao dịch khoá sổ tồn cùng một thứ tự)
    public List<OpenItem> findOpenItems(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT i.id, i.product_id, p.sku, " + REMAINING + " AS remaining, r.id AS reservation_id,"
                + " r.qty_base AS reserved_qty FROM sales_order_items i JOIN products p ON p.id = i.product_id"
                + " LEFT JOIN stock_reservations r ON r.order_item_id = i.id AND r.status = 'ACTIVE'"
                + " WHERE i.order_id = ? AND " + REMAINING + " > 0 ORDER BY i.product_id, i.id";
        List<OpenItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new OpenItem(resultSet.getLong("id"), resultSet.getLong("product_id"),
                            resultSet.getString("sku"), resultSet.getBigDecimal("remaining"),
                            resultSet.getObject("reservation_id", Long.class), resultSet.getBigDecimal("reserved_qty")));
                }
            }
        }
        return items;
    }

    // Khoá dòng sổ tồn (kho, SKU); null nếu SKU chưa từng có ở kho này
    public Balance lockBalance(Connection connection, long warehouseId, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT qty_on_hand_base, qty_reserved_base,"
                + " location_id FROM stock_balances WHERE warehouse_id = ? AND product_id = ? FOR UPDATE")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new Balance(resultSet.getBigDecimal(1), resultSet.getBigDecimal(2),
                        resultSet.getObject(3, Long.class)) : null;
            }
        }
    }

    // Giữ chỗ không hạn: hàng đã đưa vào soạn / còn nợ hàng thì không tự nhả theo hạn giữ chỗ (S5-06)
    public void insertReservation(Connection connection, long orderItemId, long warehouseId, long productId,
                                  BigDecimal qty, long actorUserId) throws SQLException {
        String sql = "INSERT INTO stock_reservations (order_item_id, warehouse_id, product_id, qty_base, status,"
                + " reserved_at, expires_at, created_at, created_by)"
                + " VALUES (?, ?, ?, ?, 'ACTIVE', UTC_TIMESTAMP(), NULL, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderItemId);
            statement.setLong(2, warehouseId);
            statement.setLong(3, productId);
            statement.setBigDecimal(4, qty);
            statement.setLong(5, actorUserId);
            statement.executeUpdate();
        }
    }

    // Đơn đã vào soạn hàng: bỏ hạn giữ chỗ để việc nhả giữ chỗ quá hạn không lấy mất hàng đang soạn
    public void clearHoldClock(Connection connection, long orderId) throws SQLException {
        String sql = "UPDATE stock_reservations r JOIN sales_order_items i ON i.id = r.order_item_id"
                + " SET r.expires_at = NULL WHERE i.order_id = ? AND r.status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.executeUpdate();
        }
    }

    // Lô còn hàng của SKU tại kho, hạn sử dụng gần nhất trước (FEFO), bỏ lô đã hết hạn; lô không hạn để cuối.
    // Phần đã chia cho phiếu soạn khác chưa xuất kho được trừ ra (trừ đơn đã huỷ giữa chừng).
    public List<LotStock> findPickableLots(Connection connection, long warehouseId, long productId, LocalDate today)
            throws SQLException {
        String sql = "SELECT l.id, l.lot_no, l.expiry_date, b.qty_on_hand_base - COALESCE((SELECT"
                + " SUM(COALESCE(pi.qty_picked_base, pi.qty_required_base)) FROM pick_list_items pi"
                + " JOIN pick_lists p ON p.id = pi.pick_list_id JOIN sales_order_items oi ON oi.id = pi.order_item_id"
                + " JOIN sales_orders so ON so.id = oi.order_id"
                + " WHERE pi.lot_id = l.id AND p.warehouse_id = b.warehouse_id AND p.status <> 'CANCELLED'"
                + " AND so.status <> 'CANCELLED'"
                + " AND NOT EXISTS (SELECT 1 FROM delivery_notes d WHERE d.pick_list_id = p.id"
                + " AND d.order_id = oi.order_id)), 0) AS free_qty"
                + " FROM stock_lot_balances b JOIN lots l ON l.id = b.lot_id"
                + " WHERE b.warehouse_id = ? AND l.product_id = ? AND b.qty_on_hand_base > 0"
                + " AND (l.expiry_date IS NULL OR l.expiry_date >= ?)"
                + " ORDER BY l.expiry_date IS NULL, l.expiry_date, l.id";
        List<LotStock> lots = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            statement.setDate(3, Date.valueOf(today));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lots.add(new LotStock(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getObject(3, LocalDate.class), resultSet.getBigDecimal(4)));
                }
            }
        }
        return lots;
    }

    // Cấp mã phiếu liên tục PSH-001, PSH-002... bằng khoá dòng document_sequences trong transaction lập phiếu
    public String nextPickNo(Connection connection) throws SQLException {
        String initSql = "INSERT IGNORE INTO document_sequences (doc_type, period_key, prefix, last_number)"
                + " SELECT ?, '', ?, COALESCE(MAX(CAST(SUBSTRING(pick_no, 5) AS UNSIGNED)), 0) FROM pick_lists"
                + " WHERE pick_no REGEXP '^PSH-[0-9]+$'";
        try (PreparedStatement statement = connection.prepareStatement(initSql)) {
            statement.setString(1, DOC_TYPE);
            statement.setString(2, NO_PREFIX);
            statement.executeUpdate();
        }
        long next;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT last_number FROM document_sequences WHERE doc_type = ? AND period_key = '' FOR UPDATE")) {
            statement.setString(1, DOC_TYPE);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                next = resultSet.getLong(1) + 1;
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE document_sequences SET last_number = ? WHERE doc_type = ? AND period_key = ''")) {
            statement.setLong(1, next);
            statement.setString(2, DOC_TYPE);
            statement.executeUpdate();
        }
        return NO_PREFIX + String.format("%03d", next);
    }

    public long insertPickList(Connection connection, String pickNo, long warehouseId, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO pick_lists (pick_no, warehouse_id, status, created_at, created_by, updated_at,"
                + " updated_by) VALUES (?, ?, 'OPEN', UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, pickNo);
            statement.setLong(2, warehouseId);
            statement.setLong(3, actorUserId);
            statement.setLong(4, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertItem(Connection connection, long pickListId, long orderItemId, long productId, long lotId,
                           Long locationId, BigDecimal qtyRequired) throws SQLException {
        String sql = "INSERT INTO pick_list_items (pick_list_id, order_item_id, product_id, lot_id, location_id,"
                + " qty_required_base) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pickListId);
            statement.setLong(2, orderItemId);
            statement.setLong(3, productId);
            statement.setLong(4, lotId);
            setNullableLong(statement, 5, locationId);
            statement.setBigDecimal(6, qtyRequired);
            statement.executeUpdate();
        }
    }

    // Đổi trạng thái đơn khi còn đúng trạng thái cũ; không đụng approved_at. false nếu đơn vừa đổi ở nơi khác.
    public boolean changeOrderStatus(Connection connection, long orderId, String fromStatus, String toStatus,
                                     long actorUserId) throws SQLException {
        String sql = "UPDATE sales_orders SET status = ?, version = version + 1, updated_at = UTC_TIMESTAMP(),"
                + " updated_by = ? WHERE id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, toStatus);
            statement.setLong(2, actorUserId);
            statement.setLong(3, orderId);
            statement.setString(4, fromStatus);
            return statement.executeUpdate() == 1;
        }
    }

    // ----- Ghi nhận soạn hàng -----

    // Khoá phiếu; null nếu không có phiếu
    public PickHead lockPickList(Connection connection, long pickListId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pick_no, status, warehouse_id FROM pick_lists WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, pickListId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new PickHead(resultSet.getString(1), resultSet.getString(2),
                        resultSet.getLong(3)) : null;
            }
        }
    }

    public List<PickLine> findLines(Connection connection, long pickListId) throws SQLException {
        List<PickLine> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, qty_required_base FROM pick_list_items WHERE pick_list_id = ? ORDER BY id")) {
            statement.setLong(1, pickListId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lines.add(new PickLine(resultSet.getLong(1), resultSet.getBigDecimal(2)));
                }
            }
        }
        return lines;
    }

    // qtyPicked = null: bỏ đánh dấu đã soạn
    public void updateLine(Connection connection, long lineId, BigDecimal qtyPicked, String shortReason)
            throws SQLException {
        String sql = "UPDATE pick_list_items SET qty_picked_base = ?, short_reason = ?,"
                + " picked_at = CASE WHEN ? IS NULL THEN NULL ELSE COALESCE(picked_at, UTC_TIMESTAMP()) END WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, qtyPicked);
            statement.setString(2, shortReason);
            statement.setBigDecimal(3, qtyPicked);
            statement.setLong(4, lineId);
            statement.executeUpdate();
        }
    }

    // Người ghi nhận soạn đầu tiên thành người soạn của phiếu
    public void updatePickStatus(Connection connection, long pickListId, String status, long actorUserId)
            throws SQLException {
        String sql = "UPDATE pick_lists SET status = ?, assigned_to = COALESCE(assigned_to, ?),"
                + " completed_at = CASE WHEN ? = 'DONE' THEN UTC_TIMESTAMP() ELSE completed_at END,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, actorUserId);
            statement.setString(3, status);
            statement.setLong(4, actorUserId);
            statement.setLong(5, pickListId);
            statement.executeUpdate();
        }
    }

    // Số phiếu xuất đã lập từ phiếu soạn (S6-05)
    public int countDeliveryNotes(Connection connection, long pickListId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM delivery_notes WHERE pick_list_id = ?")) {
            statement.setLong(1, pickListId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public void cancelPickList(Connection connection, long pickListId, long actorUserId) throws SQLException {
        String sql = "UPDATE pick_lists SET status = 'CANCELLED', updated_at = UTC_TIMESTAMP(), updated_by = ?"
                + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, pickListId);
            statement.executeUpdate();
        }
    }

    // Đơn trong phiếu: {id, mã đơn, trạng thái}
    public List<String[]> findOrders(Connection connection, long pickListId) throws SQLException {
        String sql = "SELECT DISTINCT o.id, o.order_no, o.status FROM pick_list_items pi"
                + " JOIN sales_order_items oi ON oi.id = pi.order_item_id JOIN sales_orders o ON o.id = oi.order_id"
                + " WHERE pi.pick_list_id = ? ORDER BY o.id";
        List<String[]> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pickListId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(new String[] {resultSet.getString(1), resultSet.getString(2), resultSet.getString(3)});
                }
            }
        }
        return orders;
    }

    private static void addLike(List<Object> params, String keyword, int times) {
        String like = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        for (int i = 0; i < times; i++) {
            params.add(like);
        }
    }

    private static void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            if (value instanceof Long number) {
                statement.setLong(i + 1, number);
            } else if (value instanceof Integer number) {
                statement.setInt(i + 1, number);
            } else if (value instanceof Date date) {
                statement.setDate(i + 1, date);
            } else {
                statement.setString(i + 1, (String) value);
            }
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
