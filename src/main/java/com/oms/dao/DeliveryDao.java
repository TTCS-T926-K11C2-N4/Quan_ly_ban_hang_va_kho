package com.oms.dao;

import com.oms.model.DeliveryPoint;
import com.oms.model.DeliveryStop;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Xác nhận giao hàng (S6-08): phiếu xuất của một ngày gom theo điểm giao, ghi nhận giao thành công (người nhận,
// thời điểm, ảnh biên bản) hoặc không thành công (lý do, hàng quay về kho). Các hàm nhận Connection chạy trong
// transaction do DeliveryConfirmService mở.
public class DeliveryDao {

    // Phiếu xuất đã khoá để ghi nhận giao
    public record NoteLock(long id, String deliveryNo, String status, long orderId, String orderNo,
                           String orderStatus, long warehouseId, long addressId, LocalDateTime issuedAt) {
    }

    // Dòng phiếu xuất (để đưa hàng quay về kho khi giao không thành công)
    public record NoteItem(long orderItemId, long productId, String sku, long lotId, BigDecimal qty) {
    }

    // Ảnh chứng từ: tệp và người được xem (đại lý của đơn)
    public record ProofFile(String storageKey, String contentType, String originalName, Long salesRepId) {
    }

    // Phiếu xuất của ngày [fromUtc, toUtc) gom theo điểm giao; carryOver = true thì kèm phiếu chưa giao của các ngày
    // trước (xem ở hôm nay). addressId khác null: chỉ một điểm giao. salesRepId khác null: chỉ đại lý người đó phụ trách.
    public List<DeliveryPoint> findPoints(LocalDateTime fromUtc, LocalDateTime toUtc, boolean carryOver, Long addressId,
                                          Long salesRepId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT d.id, d.delivery_no, d.status, d.warehouse_id, d.issued_at,"
                + " d.recipient_name, d.delivered_at, d.failure_reason, d.delivery_remark, o.id AS order_id, o.order_no,"
                + " o.requested_delivery_date, a.id AS address_id, a.label, a.address, a.receiver_name, a.receiver_phone,"
                + " a.route_note, c.name AS customer_name, c.code AS customer_code,"
                + " (SELECT ROUND(SUM(i.qty_issued_base * oi.line_total / oi.qty_base), 0) FROM delivery_note_items i"
                + " JOIN sales_order_items oi ON oi.id = i.order_item_id WHERE i.delivery_note_id = d.id) AS note_value"
                + " FROM delivery_notes d JOIN sales_orders o ON o.id = d.order_id"
                + " JOIN customer_delivery_addresses a ON a.id = o.delivery_address_id"
                + " JOIN customers c ON c.id = o.customer_id"
                + " WHERE ((d.issued_at >= ? AND d.issued_at < ?)");
        if (carryOver) {
            sql.append(" OR (d.status IN ('ISSUED', 'IN_TRANSIT') AND d.issued_at < ?)");
        }
        sql.append(")");
        if (addressId != null) {
            sql.append(" AND a.id = ?");
        }
        if (salesRepId != null) {
            sql.append(" AND c.sales_rep_id = ?");
        }
        sql.append(" ORDER BY c.name, a.id, o.requested_delivery_date, d.id");
        Map<Long, DeliveryPoint> points = new LinkedHashMap<>();
        Map<Long, DeliveryStop> stops = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                int index = 1;
                // LocalDateTime gửi nguyên giá trị UTC (Timestamp sẽ bị driver đổi theo múi giờ của JVM)
                statement.setObject(index++, fromUtc, Types.TIMESTAMP);
                statement.setObject(index++, toUtc, Types.TIMESTAMP);
                if (carryOver) {
                    statement.setObject(index++, fromUtc, Types.TIMESTAMP);
                }
                if (addressId != null) {
                    statement.setLong(index++, addressId);
                }
                if (salesRepId != null) {
                    statement.setLong(index, salesRepId);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        long pointId = resultSet.getLong("address_id");
                        DeliveryPoint point = points.get(pointId);
                        if (point == null) {
                            point = new DeliveryPoint(pointId, resultSet.getString("customer_name"),
                                    resultSet.getString("customer_code"), resultSet.getString("label"),
                                    resultSet.getString("address"), resultSet.getString("receiver_name"),
                                    resultSet.getString("receiver_phone"), resultSet.getString("route_note"));
                            points.put(pointId, point);
                        }
                        DeliveryStop stop = new DeliveryStop(resultSet.getLong("id"), resultSet.getString("delivery_no"),
                                resultSet.getString("status"), resultSet.getLong("warehouse_id"),
                                resultSet.getLong("order_id"), resultSet.getString("order_no"),
                                resultSet.getObject("requested_delivery_date", LocalDate.class),
                                resultSet.getObject("issued_at", LocalDateTime.class), resultSet.getBigDecimal("note_value"),
                                resultSet.getString("recipient_name"),
                                resultSet.getObject("delivered_at", LocalDateTime.class),
                                resultSet.getString("failure_reason"), resultSet.getString("delivery_remark"));
                        point.getStops().add(stop);
                        stops.put(stop.getNoteId(), stop);
                    }
                }
            }
            if (!stops.isEmpty()) {
                loadItems(connection, stops);
                loadProofs(connection, stops);
            }
        }
        return new ArrayList<>(points.values());
    }

    private static void loadItems(Connection connection, Map<Long, DeliveryStop> stops) throws SQLException {
        String sql = "SELECT i.delivery_note_id, p.sku, p.name, SUM(i.qty_issued_base) AS qty, u.name AS unit_name"
                + " FROM delivery_note_items i JOIN products p ON p.id = i.product_id JOIN units u ON u.id = p.base_unit_id"
                + " WHERE i.delivery_note_id IN (" + placeholders(stops.size()) + ")"
                + " GROUP BY i.delivery_note_id, i.order_item_id, p.sku, p.name, u.name ORDER BY i.delivery_note_id, p.sku";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindIds(statement, stops.keySet());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    stops.get(resultSet.getLong(1)).getItems().add(new DeliveryStop.Item(resultSet.getString(2),
                            resultSet.getString(3), resultSet.getBigDecimal(4), resultSet.getString(5)));
                }
            }
        }
    }

    private static void loadProofs(Connection connection, Map<Long, DeliveryStop> stops) throws SQLException {
        String sql = "SELECT delivery_note_id, id FROM delivery_proofs WHERE delivery_note_id IN ("
                + placeholders(stops.size()) + ") ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindIds(statement, stops.keySet());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    stops.get(resultSet.getLong(1)).getProofIds().add(resultSet.getLong(2));
                }
            }
        }
    }

    public ProofFile findProof(long proofId) throws SQLException {
        String sql = "SELECT f.storage_key, f.content_type, f.original_name, c.sales_rep_id FROM delivery_proofs pr"
                + " JOIN file_objects f ON f.id = pr.file_id JOIN delivery_notes d ON d.id = pr.delivery_note_id"
                + " JOIN sales_orders o ON o.id = d.order_id JOIN customers c ON c.id = o.customer_id WHERE pr.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, proofId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new ProofFile(resultSet.getString(1), resultSet.getString(2),
                        resultSet.getString(3), resultSet.getObject(4, Long.class)) : null;
            }
        }
    }

    // ----- Ghi nhận giao -----

    // Khoá các phiếu theo thứ tự id
    public List<NoteLock> lockNotes(Connection connection, Collection<Long> noteIds) throws SQLException {
        String sql = "SELECT d.id, d.delivery_no, d.status, d.order_id, o.order_no, o.status, d.warehouse_id,"
                + " o.delivery_address_id, d.issued_at FROM delivery_notes d JOIN sales_orders o ON o.id = d.order_id"
                + " WHERE d.id IN (" + placeholders(noteIds.size()) + ") ORDER BY d.id FOR UPDATE";
        List<NoteLock> notes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindIds(statement, noteIds);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notes.add(new NoteLock(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
                            resultSet.getLong(4), resultSet.getString(5), resultSet.getString(6), resultSet.getLong(7),
                            resultSet.getLong(8), resultSet.getObject(9, LocalDateTime.class)));
                }
            }
        }
        return notes;
    }

    public void markDelivered(Connection connection, long noteId, String recipientName, LocalDateTime deliveredAtUtc,
                              String remark, long actorUserId) throws SQLException {
        String sql = "UPDATE delivery_notes SET status = 'DELIVERED', recipient_name = ?, delivered_at = ?,"
                + " delivery_remark = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, recipientName);
            statement.setObject(2, deliveredAtUtc, Types.TIMESTAMP);
            statement.setString(3, remark);
            statement.setLong(4, actorUserId);
            statement.setLong(5, noteId);
            statement.executeUpdate();
        }
        // Giao đủ số đã xuất; hoá đơn (S7-01) lập theo số thực giao
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE delivery_note_items SET qty_delivered_base = qty_issued_base WHERE delivery_note_id = ?")) {
            statement.setLong(1, noteId);
            statement.executeUpdate();
        }
    }

    public void insertProof(Connection connection, long noteId, long fileId, long actorUserId) throws SQLException {
        String sql = "INSERT INTO delivery_proofs (delivery_note_id, file_id, proof_type, created_at, created_by)"
                + " VALUES (?, ?, 'SIGNED_RECEIPT', UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            statement.setLong(2, fileId);
            statement.setLong(3, actorUserId);
            statement.executeUpdate();
        }
    }

    public void markFailed(Connection connection, long noteId, String reason, String remark, long actorUserId)
            throws SQLException {
        String sql = "UPDATE delivery_notes SET status = 'FAILED', failure_reason = ?, delivery_remark = ?,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reason);
            statement.setString(2, remark);
            statement.setLong(3, actorUserId);
            statement.setLong(4, noteId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE delivery_note_items SET qty_delivered_base = 0 WHERE delivery_note_id = ?")) {
            statement.setLong(1, noteId);
            statement.executeUpdate();
        }
    }

    // Theo thứ tự SKU rồi lô (mọi giao dịch khoá sổ tồn cùng một thứ tự)
    public List<NoteItem> findNoteItems(Connection connection, long noteId) throws SQLException {
        String sql = "SELECT i.order_item_id, i.product_id, p.sku, i.lot_id, i.qty_issued_base FROM delivery_note_items i"
                + " JOIN products p ON p.id = i.product_id WHERE i.delivery_note_id = ? ORDER BY i.product_id, i.lot_id";
        List<NoteItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, noteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new NoteItem(resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3),
                            resultSet.getLong(4), resultSet.getBigDecimal(5)));
                }
            }
        }
        return items;
    }

    // Cộng thêm vào giữ chỗ đang hiệu lực của dòng đơn (hàng giao không thành công quay về giữ chỗ cho đơn)
    public void addToReservation(Connection connection, long reservationId, BigDecimal qty) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_reservations SET qty_base = qty_base + ? WHERE id = ? AND status = 'ACTIVE'")) {
            statement.setBigDecimal(1, qty);
            statement.setLong(2, reservationId);
            statement.executeUpdate();
        }
    }

    // Hàng còn phải giao của đơn (chưa xuất, chưa đóng)
    public BigDecimal findRemaining(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(SUM(qty_base - qty_shipped_base"
                + " - qty_closed_base), 0) FROM sales_order_items WHERE order_id = ?")) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    // Số phiếu xuất của đơn theo trạng thái
    public int countNotes(Connection connection, long orderId, String... statuses) throws SQLException {
        String sql = "SELECT COUNT(*) FROM delivery_notes WHERE order_id = ? AND status IN ("
                + placeholders(statuses.length) + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            for (int i = 0; i < statuses.length; i++) {
                statement.setString(i + 2, statuses[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private static void bindIds(PreparedStatement statement, Collection<Long> ids) throws SQLException {
        int index = 1;
        for (Long id : ids) {
            statement.setLong(index++, id);
        }
    }
}
