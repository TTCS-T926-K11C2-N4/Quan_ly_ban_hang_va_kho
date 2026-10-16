package com.oms.dao;

import com.oms.model.StockAdjustment;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Phiếu điều chỉnh tồn kho (S8-03). Các hàm nhận Connection chạy trong transaction của AdjustmentService.
public class AdjustmentDao {

    private static final String DOC_TYPE = "ADJUSTMENT";
    private static final String NO_PREFIX = "DT";
    private static final String HEADER = "SELECT a.id, a.adjustment_no, w.name, a.reason_type, a.status,"
            + " a.total_loss_value, a.note, cu.full_name, a.created_by, a.created_at, au.full_name, a.approved_at"
            + " FROM stock_adjustments a JOIN warehouses w ON w.id = a.warehouse_id"
            + " LEFT JOIN users cu ON cu.id = a.created_by LEFT JOIN users au ON au.id = a.approved_by";

    // Lô còn hàng ở kho kèm giá vốn của SKU; null nếu lô không có hàng ở kho
    public LotInfo findLotInfo(Connection connection, long warehouseId, long lotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT l.product_id, p.sku, l.lot_no, p.cost_price, b.qty_on_hand_base FROM stock_lot_balances b"
                        + " JOIN lots l ON l.id = b.lot_id JOIN products p ON p.id = l.product_id"
                        + " WHERE b.warehouse_id = ? AND b.lot_id = ?")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, lotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new LotInfo(resultSet.getLong(1), resultSet.getString(2),
                        resultSet.getString(3), resultSet.getBigDecimal(4), resultSet.getBigDecimal(5)) : null;
            }
        }
    }

    public String nextAdjustmentNo(Connection connection, int year) throws SQLException {
        String period = String.valueOf(year);
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO document_sequences (doc_type, period_key, prefix, last_number) VALUES (?, ?, ?, 0)")) {
            statement.setString(1, DOC_TYPE);
            statement.setString(2, period);
            statement.setString(3, NO_PREFIX);
            statement.executeUpdate();
        }
        long next;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT last_number FROM document_sequences WHERE doc_type = ? AND period_key = ? FOR UPDATE")) {
            statement.setString(1, DOC_TYPE);
            statement.setString(2, period);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                next = resultSet.getLong(1) + 1;
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE document_sequences SET last_number = ? WHERE doc_type = ? AND period_key = ?")) {
            statement.setLong(1, next);
            statement.setString(2, DOC_TYPE);
            statement.setString(3, period);
            statement.executeUpdate();
        }
        return NO_PREFIX + period + "-" + String.format("%06d", next);
    }

    public long insert(Connection connection, String no, long warehouseId, String reason, BigDecimal totalLoss,
                       String note, long actorUserId) throws SQLException {
        String sql = "INSERT INTO stock_adjustments (adjustment_no, warehouse_id, reason_type, status,"
                + " total_loss_value, note, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, 'PENDING_APPROVAL', ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, no);
            statement.setLong(2, warehouseId);
            statement.setString(3, reason);
            statement.setBigDecimal(4, totalLoss);
            statement.setString(5, note);
            statement.setLong(6, actorUserId);
            statement.setLong(7, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertItem(Connection connection, long adjustmentId, long productId, long lotId, BigDecimal qtyChange,
                           BigDecimal unitCost, BigDecimal loss) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO stock_adjustment_items (adjustment_id, product_id, lot_id, qty_change_base,"
                        + " unit_cost_snapshot, loss_value) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, adjustmentId);
            statement.setLong(2, productId);
            statement.setLong(3, lotId);
            statement.setBigDecimal(4, qtyChange);
            statement.setBigDecimal(5, unitCost);
            statement.setBigDecimal(6, loss);
            statement.executeUpdate();
        }
    }

    public List<StockAdjustment> findAll(int limit) throws SQLException {
        List<StockAdjustment> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(HEADER + " ORDER BY a.id DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(read(resultSet));
                }
            }
        }
        return result;
    }

    // Kèm dòng điều chỉnh; null nếu không có
    public StockAdjustment find(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            StockAdjustment header;
            try (PreparedStatement statement = connection.prepareStatement(HEADER + " WHERE a.id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    header = read(resultSet);
                }
            }
            List<StockAdjustment.Item> items = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT p.sku, p.name, l.lot_no, i.qty_change_base, i.loss_value FROM stock_adjustment_items i"
                            + " JOIN products p ON p.id = i.product_id JOIN lots l ON l.id = i.lot_id"
                            + " WHERE i.adjustment_id = ? ORDER BY i.id")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        items.add(new StockAdjustment.Item(resultSet.getString(1), resultSet.getString(2),
                                resultSet.getString(3), resultSet.getBigDecimal(4), resultSet.getBigDecimal(5)));
                    }
                }
            }
            return header.withItems(items);
        }
    }

    // Khoá phiếu; null nếu không có
    public Lock lock(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, adjustment_no, warehouse_id, status, created_by FROM stock_adjustments WHERE id = ?"
                        + " FOR UPDATE")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new Lock(resultSet.getLong(1), resultSet.getString(2),
                        resultSet.getLong(3), resultSet.getString(4), resultSet.getLong(5)) : null;
            }
        }
    }

    public List<ApplyItem> findApplyItems(Connection connection, long adjustmentId) throws SQLException {
        List<ApplyItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT product_id, lot_id, qty_change_base FROM stock_adjustment_items WHERE adjustment_id = ?"
                        + " ORDER BY id")) {
            statement.setLong(1, adjustmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new ApplyItem(resultSet.getLong(1), resultSet.getLong(2), resultSet.getBigDecimal(3)));
                }
            }
        }
        return items;
    }

    public void decide(Connection connection, long id, String status, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_adjustments SET status = ?, approved_by = ?, approved_at = UTC_TIMESTAMP(),"
                        + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setLong(2, actorUserId);
            statement.setLong(3, actorUserId);
            statement.setLong(4, id);
            statement.executeUpdate();
        }
    }

    // Thẻ kho: ADJUSTMENT (âm) ở kho điều chỉnh
    public void insertMovement(Connection connection, long warehouseId, long productId, long lotId,
                               BigDecimal change, BigDecimal balanceAfter, long adjustmentId, String note,
                               long actorUserId) throws SQLException {
        String sql = "INSERT INTO stock_movements (warehouse_id, product_id, lot_id, movement_type, qty_change_base,"
                + " balance_after_base, ref_type, ref_id, occurred_at, note, created_at, created_by)"
                + " VALUES (?, ?, ?, 'ADJUSTMENT', ?, ?, 'STOCK_ADJUSTMENT', ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            statement.setLong(3, lotId);
            statement.setBigDecimal(4, change);
            statement.setBigDecimal(5, balanceAfter);
            statement.setLong(6, adjustmentId);
            statement.setString(7, note);
            statement.setLong(8, actorUserId);
            statement.executeUpdate();
        }
    }

    private static StockAdjustment read(ResultSet resultSet) throws SQLException {
        return new StockAdjustment(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
                resultSet.getString(4), resultSet.getString(5), resultSet.getBigDecimal(6), resultSet.getString(7),
                resultSet.getString(8), resultSet.getLong(9), resultSet.getObject(10, LocalDateTime.class),
                resultSet.getString(11), resultSet.getObject(12, LocalDateTime.class), List.of());
    }

    public record LotInfo(long productId, String sku, String lotNo, BigDecimal costPrice, BigDecimal onHand) {
    }

    public record Lock(long id, String adjustmentNo, long warehouseId, String status, long createdBy) {
    }

    public record ApplyItem(long productId, long lotId, BigDecimal qtyChange) {
    }
}
