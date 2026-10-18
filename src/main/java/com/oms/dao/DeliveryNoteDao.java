package com.oms.dao;

import com.oms.model.DeliveryIssueDraft;
import com.oms.model.DeliveryNoteDetail;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Phiếu xuất kho (delivery_notes, delivery_note_items, delivery_note_shortages, S6-05): dữ liệu màn xác nhận
// xuất kho của một đơn trong phiếu soạn, các bước ghi khi xác nhận (trừ tồn theo lô, ghi thẻ kho, chuyển giữ chỗ
// thành xuất thực, cộng số đã xuất của dòng đơn) và chi tiết phiếu đã xác nhận. Các hàm nhận Connection chạy trong
// transaction do DeliveryNoteService mở.
public class DeliveryNoteDao {

    private static final String DOC_TYPE = "DELIVERY";
    private static final String NO_PREFIX = "PX-";
    private static final String REF_TYPE = "DELIVERY_NOTE";

    // Lô đã soạn của một dòng đơn (FEFO trước)
    public record PickedLot(long orderItemId, long productId, long lotId, String lotNo, BigDecimal qtyPicked) {
    }

    public record Reservation(long id, BigDecimal qty) {
    }

    // Phiếu xuất của đơn (nhân viên kinh doanh chỉ xem đơn của đại lý mình phụ trách)
    public record NoteOwner(long orderId, Long salesRepId) {
    }

    // null nếu phiếu soạn không có đơn này
    public DeliveryIssueDraft findDraft(long pickListId, long orderId) throws SQLException {
        String headerSql = "SELECT pl.id, pl.pick_no, pl.status, pl.warehouse_id, w.name AS warehouse_name,"
                + " au.full_name AS picker_name, o.id AS order_id, o.order_no, o.status AS order_status,"
                + " c.name AS customer_name, o.requested_delivery_date,"
                + " (SELECT d.id FROM delivery_notes d WHERE d.pick_list_id = pl.id AND d.order_id = o.id) AS note_id"
                + " FROM pick_lists pl JOIN warehouses w ON w.id = pl.warehouse_id"
                + " LEFT JOIN users au ON au.id = pl.assigned_to JOIN sales_orders o ON o.id = ?"
                + " JOIN customers c ON c.id = o.customer_id WHERE pl.id = ? AND EXISTS (SELECT 1 FROM pick_list_items pi"
                + " JOIN sales_order_items oi ON oi.id = pi.order_item_id WHERE pi.pick_list_id = pl.id AND oi.order_id = o.id)";
        String lineSql = "SELECT oi.id, oi.product_id, p.sku, p.name AS product_name, bu.name AS unit_name,"
                + " SUM(pi.qty_required_base) AS required_qty, SUM(COALESCE(pi.qty_picked_base, 0)) AS picked_qty,"
                + " MAX(pi.short_reason) AS short_reason FROM pick_list_items pi"
                + " JOIN sales_order_items oi ON oi.id = pi.order_item_id JOIN products p ON p.id = oi.product_id"
                + " JOIN units bu ON bu.id = p.base_unit_id WHERE pi.pick_list_id = ? AND oi.order_id = ?"
                + " GROUP BY oi.id, oi.product_id, p.sku, p.name, bu.name, oi.line_no ORDER BY oi.line_no, oi.id";
        try (Connection connection = DbConnection.getConnection()) {
            List<DeliveryIssueDraft.Line> lines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(lineSql)) {
                statement.setLong(1, pickListId);
                statement.setLong(2, orderId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        lines.add(new DeliveryIssueDraft.Line(resultSet.getLong("id"), resultSet.getLong("product_id"),
                                resultSet.getString("sku"), resultSet.getString("product_name"),
                                resultSet.getString("unit_name"), resultSet.getBigDecimal("required_qty"),
                                resultSet.getBigDecimal("picked_qty"), resultSet.getString("short_reason")));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
                statement.setLong(1, orderId);
                statement.setLong(2, pickListId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return new DeliveryIssueDraft(resultSet.getLong("id"), resultSet.getString("pick_no"),
                            resultSet.getString("status"), resultSet.getLong("warehouse_id"),
                            resultSet.getString("warehouse_name"), resultSet.getString("picker_name"),
                            resultSet.getLong("order_id"), resultSet.getString("order_no"),
                            resultSet.getString("order_status"), resultSet.getString("customer_name"),
                            resultSet.getObject("requested_delivery_date", LocalDate.class),
                            resultSet.getObject("note_id", Long.class), lines);
                }
            }
        }
    }

    // Đại lý của đơn (để lọc theo nhân viên kinh doanh phụ trách)
    public Long findSalesRepOfOrder(long orderId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT c.sales_rep_id FROM sales_orders o"
                     + " JOIN customers c ON c.id = o.customer_id WHERE o.id = ?")) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject(1, Long.class) : null;
            }
        }
    }

    // ----- Xác nhận xuất kho -----

    public Long findNoteId(Connection connection, long pickListId, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM delivery_notes WHERE pick_list_id = ? AND order_id = ?")) {
            statement.setLong(1, pickListId);
            statement.setLong(2, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    public List<PickedLot> findPickedLots(Connection connection, long pickListId, long orderId) throws SQLException {
        String sql = "SELECT pi.order_item_id, pi.product_id, pi.lot_id, l.lot_no, COALESCE(pi.qty_picked_base, 0)"
                + " FROM pick_list_items pi JOIN sales_order_items oi ON oi.id = pi.order_item_id"
                + " JOIN lots l ON l.id = pi.lot_id WHERE pi.pick_list_id = ? AND oi.order_id = ?"
                + " ORDER BY pi.order_item_id, l.expiry_date IS NULL, l.expiry_date, pi.id";
        List<PickedLot> lots = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pickListId);
            statement.setLong(2, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lots.add(new PickedLot(resultSet.getLong(1), resultSet.getLong(2), resultSet.getLong(3),
                            resultSet.getString(4), resultSet.getBigDecimal(5)));
                }
            }
        }
        return lots;
    }

    // Cấp mã phiếu xuất liên tục PX-001, PX-002... trong transaction xác nhận xuất kho
    public String nextDeliveryNo(Connection connection) throws SQLException {
        String initSql = "INSERT IGNORE INTO document_sequences (doc_type, period_key, prefix, last_number)"
                + " SELECT ?, '', ?, COALESCE(MAX(CAST(SUBSTRING(delivery_no, 4) AS UNSIGNED)), 0) FROM delivery_notes"
                + " WHERE delivery_no REGEXP '^PX-[0-9]+$'";
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

    public long insertNote(Connection connection, String deliveryNo, long orderId, long warehouseId, long pickListId,
                           String shortageAction, LocalDateTime backorderDueUtc, String shortageNote, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO delivery_notes (delivery_no, order_id, warehouse_id, pick_list_id, status, issued_at,"
                + " issued_by, shortage_action, backorder_due_at, shortage_note, created_at, created_by, updated_at,"
                + " updated_by) VALUES (?, ?, ?, ?, 'ISSUED', UTC_TIMESTAMP(), ?, ?, ?, ?, UTC_TIMESTAMP(), ?,"
                + " UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, deliveryNo);
            statement.setLong(2, orderId);
            statement.setLong(3, warehouseId);
            statement.setLong(4, pickListId);
            statement.setLong(5, actorUserId);
            statement.setString(6, shortageAction);
            // LocalDateTime gửi nguyên giá trị (đã là giờ UTC); Timestamp sẽ bị driver đổi theo múi giờ của JVM
            statement.setObject(7, backorderDueUtc, Types.TIMESTAMP);
            statement.setString(8, shortageNote);
            statement.setLong(9, actorUserId);
            statement.setLong(10, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertItem(Connection connection, long noteId, long orderItemId, long productId, long lotId,
                           BigDecimal qty) throws SQLException {
        String sql = "INSERT INTO delivery_note_items (delivery_note_id, order_item_id, product_id, lot_id,"
                + " qty_issued_base) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setLong(2, orderItemId);
            statement.setLong(3, productId);
            statement.setLong(4, lotId);
            statement.setBigDecimal(5, qty);
            statement.executeUpdate();
        }
    }

    public void insertShortage(Connection connection, long noteId, long orderItemId, BigDecimal required,
                               BigDecimal issued, String reason) throws SQLException {
        String sql = "INSERT INTO delivery_note_shortages (delivery_note_id, order_item_id, qty_required_base,"
                + " qty_issued_base, short_reason, created_at) VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setLong(2, orderItemId);
            statement.setBigDecimal(3, required);
            statement.setBigDecimal(4, issued);
            statement.setString(5, reason);
            statement.executeUpdate();
        }
    }

    // Giữ chỗ đang hiệu lực của dòng đơn (khoá); null nếu không có
    public Reservation lockReservation(Connection connection, long orderItemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id, qty_base FROM stock_reservations"
                + " WHERE order_item_id = ? AND status = 'ACTIVE' FOR UPDATE")) {
            statement.setLong(1, orderItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new Reservation(resultSet.getLong(1), resultSet.getBigDecimal(2)) : null;
            }
        }
    }

    // Giữ chỗ đã thành xuất thực
    public void markFulfilled(Connection connection, long reservationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_reservations SET status = 'FULFILLED' WHERE id = ? AND status = 'ACTIVE'")) {
            statement.setLong(1, reservationId);
            statement.executeUpdate();
        }
    }

    // Đặt tồn thực tế và đang giữ chỗ mới trong một câu lệnh (CHECK giữ chỗ <= tồn thực tế xét trên kết quả cuối)
    public void setBalance(Connection connection, long warehouseId, long productId, BigDecimal onHand,
                           BigDecimal reserved) throws SQLException {
        String sql = "UPDATE stock_balances SET qty_on_hand_base = ?, qty_reserved_base = ?, version = version + 1,"
                + " updated_at = UTC_TIMESTAMP() WHERE warehouse_id = ? AND product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, onHand);
            statement.setBigDecimal(2, reserved);
            statement.setLong(3, warehouseId);
            statement.setLong(4, productId);
            statement.executeUpdate();
        }
    }

    // Thẻ kho: qtyChange có dấu (- xuất, + nhập lại)
    public void insertMovement(Connection connection, long warehouseId, long productId, long lotId, String movementType,
                               BigDecimal qtyChange, BigDecimal balanceAfter, long noteId, String note,
                               long actorUserId) throws SQLException {
        String sql = "INSERT INTO stock_movements (warehouse_id, product_id, lot_id, movement_type, qty_change_base,"
                + " balance_after_base, ref_type, ref_id, occurred_at, note, created_at, created_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            statement.setLong(3, lotId);
            statement.setString(4, movementType);
            statement.setBigDecimal(5, qtyChange);
            statement.setBigDecimal(6, balanceAfter);
            statement.setString(7, REF_TYPE);
            statement.setLong(8, noteId);
            statement.setString(9, note);
            statement.setLong(10, actorUserId);
            statement.executeUpdate();
        }
    }

    // Cộng số đã xuất (âm khi hàng quay về kho) và phần thiếu đã đóng của dòng đơn
    public void addShipped(Connection connection, long orderItemId, BigDecimal shippedDelta, BigDecimal closedDelta,
                           long actorUserId) throws SQLException {
        String sql = "UPDATE sales_order_items SET qty_shipped_base = qty_shipped_base + ?,"
                + " qty_closed_base = qty_closed_base + ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, shippedDelta);
            statement.setBigDecimal(2, closedDelta);
            statement.setLong(3, actorUserId);
            statement.setLong(4, orderItemId);
            statement.executeUpdate();
        }
    }

    // ----- Xem phiếu xuất -----

    public NoteOwner findOwner(long noteId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT d.order_id, c.sales_rep_id"
                     + " FROM delivery_notes d JOIN sales_orders o ON o.id = d.order_id"
                     + " JOIN customers c ON c.id = o.customer_id WHERE d.id = ?")) {
            statement.setLong(1, noteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new NoteOwner(resultSet.getLong(1), resultSet.getObject(2, Long.class)) : null;
            }
        }
    }

    public DeliveryNoteDetail findDetail(long noteId) throws SQLException {
        String headerSql = "SELECT d.id, d.delivery_no, d.status, d.issued_at, iu.full_name AS issued_by_name,"
                + " d.pick_list_id, pl.pick_no, o.id AS order_id, o.order_no, c.name AS customer_name,"
                + " w.name AS warehouse_name, o.requested_delivery_date, d.shortage_action, d.backorder_due_at,"
                + " d.shortage_note, d.recipient_name, d.delivered_at, d.failure_reason, d.delivery_remark"
                + " FROM delivery_notes d JOIN sales_orders o ON o.id = d.order_id"
                + " JOIN customers c ON c.id = o.customer_id JOIN warehouses w ON w.id = d.warehouse_id"
                + " LEFT JOIN pick_lists pl ON pl.id = d.pick_list_id LEFT JOIN users iu ON iu.id = d.issued_by"
                + " WHERE d.id = ?";
        String lineSql = "SELECT oi.id, p.sku, p.name AS product_name, bu.name AS unit_name,"
                + " COALESCE((SELECT SUM(i.qty_issued_base) FROM delivery_note_items i WHERE i.delivery_note_id = d.id"
                + " AND i.order_item_id = oi.id), 0) AS issued_qty,"
                + " (SELECT SUM(i.qty_delivered_base) FROM delivery_note_items i WHERE i.delivery_note_id = d.id"
                + " AND i.order_item_id = oi.id) AS delivered_qty, s.qty_required_base, s.short_reason,"
                + " (SELECT GROUP_CONCAT(CONCAT(l.lot_no, ': ', TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM"
                + " i.qty_issued_base))) ORDER BY i.id SEPARATOR ', ') FROM delivery_note_items i"
                + " JOIN lots l ON l.id = i.lot_id WHERE i.delivery_note_id = d.id AND i.order_item_id = oi.id) AS lots"
                + " FROM delivery_notes d JOIN sales_order_items oi ON oi.order_id = d.order_id"
                + " JOIN products p ON p.id = oi.product_id JOIN units bu ON bu.id = p.base_unit_id"
                + " LEFT JOIN delivery_note_shortages s ON s.delivery_note_id = d.id AND s.order_item_id = oi.id"
                + " WHERE d.id = ? AND (s.id IS NOT NULL OR EXISTS (SELECT 1 FROM delivery_note_items i"
                + " WHERE i.delivery_note_id = d.id AND i.order_item_id = oi.id)) ORDER BY oi.line_no, oi.id";
        try (Connection connection = DbConnection.getConnection()) {
            List<DeliveryNoteDetail.Line> lines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(lineSql)) {
                statement.setLong(1, noteId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        BigDecimal issued = resultSet.getBigDecimal("issued_qty");
                        BigDecimal required = resultSet.getBigDecimal("qty_required_base");
                        lines.add(new DeliveryNoteDetail.Line(resultSet.getLong("id"), resultSet.getString("sku"),
                                resultSet.getString("product_name"), resultSet.getString("unit_name"),
                                required == null ? issued : required, issued, resultSet.getBigDecimal("delivered_qty"),
                                resultSet.getString("short_reason"), resultSet.getString("lots")));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
                statement.setLong(1, noteId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return new DeliveryNoteDetail(resultSet.getLong("id"), resultSet.getString("delivery_no"),
                            resultSet.getString("status"), resultSet.getObject("issued_at", LocalDateTime.class),
                            resultSet.getString("issued_by_name"), resultSet.getObject("pick_list_id", Long.class),
                            resultSet.getString("pick_no"), resultSet.getLong("order_id"),
                            resultSet.getString("order_no"), resultSet.getString("customer_name"),
                            resultSet.getString("warehouse_name"),
                            resultSet.getObject("requested_delivery_date", LocalDate.class),
                            resultSet.getString("shortage_action"),
                            resultSet.getObject("backorder_due_at", LocalDateTime.class),
                            resultSet.getString("shortage_note"), resultSet.getString("recipient_name"),
                            resultSet.getObject("delivered_at", LocalDateTime.class),
                            resultSet.getString("failure_reason"), resultSet.getString("delivery_remark"), lines);
                }
            }
        }
    }
}
