package com.oms.dao;

import com.oms.model.StockTransfer;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Phiếu chuyển kho (stock_transfers, stock_transfer_items) và các thao tác tồn khi xuất / nhận (S5-07).
// Các hàm nhận Connection chạy trong transaction của service.
public class StockTransferDao {

    private static final String HEADER_SELECT = "SELECT t.id, t.transfer_no, t.from_warehouse_id,"
            + " fw.name AS from_name, t.to_warehouse_id, tw.name AS to_name, t.status, t.shipped_at,"
            + " sb.full_name AS shipped_by_name, t.received_at, rb.full_name AS received_by_name, t.note,"
            + " cb.full_name AS created_by_name, t.created_at FROM stock_transfers t"
            + " JOIN warehouses fw ON fw.id = t.from_warehouse_id JOIN warehouses tw ON tw.id = t.to_warehouse_id"
            + " LEFT JOIN users sb ON sb.id = t.shipped_by LEFT JOIN users rb ON rb.id = t.received_by"
            + " LEFT JOIN users cb ON cb.id = t.created_by";

    public int findMaxNumber(String prefix) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(transfer_no, ?) AS UNSIGNED)), 0) FROM stock_transfers"
                + " WHERE transfer_no LIKE ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, prefix.length() + 1);
            statement.setString(2, prefix + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    // Ném SQLIntegrityConstraintViolationException nếu mã phiếu vừa bị phiếu khác dùng
    public long insert(Connection connection, String transferNo, long fromWarehouseId, long toWarehouseId,
                       String note, long actorUserId) throws SQLException {
        String sql = "INSERT INTO stock_transfers (transfer_no, from_warehouse_id, to_warehouse_id, status, note,"
                + " created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, 'DRAFT', ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transferNo);
            statement.setLong(2, fromWarehouseId);
            statement.setLong(3, toWarehouseId);
            if (note == null) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, note);
            }
            statement.setLong(5, actorUserId);
            statement.setLong(6, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public List<StockTransfer> findAll(int limit) throws SQLException {
        String sql = HEADER_SELECT + " ORDER BY t.created_at DESC, t.id DESC LIMIT ?";
        List<StockTransfer> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(readHeader(resultSet, List.of()));
                }
            }
        }
        return result;
    }

    // null nếu không có phiếu
    public StockTransfer findDetail(long id) throws SQLException {
        String itemSql = "SELECT i.id, i.product_id, i.lot_id, p.sku, p.name, lo.lot_no, u.name AS unit_name,"
                + " i.qty_sent_base, i.qty_received_base, i.variance_reason FROM stock_transfer_items i"
                + " JOIN products p ON p.id = i.product_id JOIN lots lo ON lo.id = i.lot_id"
                + " JOIN units u ON u.id = p.base_unit_id WHERE i.transfer_id = ? ORDER BY i.id";
        try (Connection connection = DbConnection.getConnection()) {
            List<StockTransfer.Item> items = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        items.add(new StockTransfer.Item(resultSet.getLong("id"), resultSet.getLong("product_id"),
                                resultSet.getLong("lot_id"), resultSet.getString("sku"), resultSet.getString("name"),
                                resultSet.getString("lot_no"), resultSet.getString("unit_name"),
                                resultSet.getBigDecimal("qty_sent_base"), resultSet.getBigDecimal("qty_received_base"),
                                resultSet.getString("variance_reason")));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(HEADER_SELECT + " WHERE t.id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? readHeader(resultSet, items) : null;
                }
            }
        }
    }

    // Lô còn hàng ở kho (đã trừ phần đang giữ chỗ không tách theo lô, nên chỉ hiện tồn thực tế theo lô)
    public List<StockTransfer.LotOption> findLotOptions(long warehouseId) throws SQLException {
        String sql = "SELECT b.lot_id, p.sku, p.name, lo.lot_no, b.qty_on_hand_base FROM stock_lot_balances b"
                + " JOIN lots lo ON lo.id = b.lot_id JOIN products p ON p.id = lo.product_id"
                + " WHERE b.warehouse_id = ? AND b.qty_on_hand_base > 0 AND p.status = 'ACTIVE'"
                + " ORDER BY p.sku, lo.lot_no";
        List<StockTransfer.LotOption> options = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    options.add(new StockTransfer.LotOption(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(3), resultSet.getString(4), resultSet.getBigDecimal(5)));
                }
            }
        }
        return options;
    }

    // Kho còn hoạt động; false nếu không có hoặc ngừng
    public boolean isActiveWarehouse(long warehouseId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM warehouses WHERE id = ? AND status = 'ACTIVE'")) {
            statement.setLong(1, warehouseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Locked lock(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT transfer_no, from_warehouse_id, to_warehouse_id, status FROM stock_transfers WHERE id = ?"
                        + " FOR UPDATE")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new Locked(resultSet.getString(1), resultSet.getLong(2),
                        resultSet.getLong(3), resultSet.getString(4)) : null;
            }
        }
    }

    // Lô của dòng phải thuộc đúng SKU có tồn ở kho đi; trả về product_id của lô, null nếu lô không có ở kho
    public Long findProductOfLot(Connection connection, long warehouseId, long lotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT lo.product_id FROM stock_lot_balances b JOIN lots lo ON lo.id = b.lot_id"
                        + " WHERE b.warehouse_id = ? AND b.lot_id = ?")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, lotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    public boolean hasItemForLot(Connection connection, long transferId, long lotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM stock_transfer_items WHERE transfer_id = ? AND lot_id = ?")) {
            statement.setLong(1, transferId);
            statement.setLong(2, lotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void insertItem(Connection connection, long transferId, long productId, long lotId, BigDecimal qty)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO stock_transfer_items (transfer_id, product_id, lot_id, qty_sent_base) VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, transferId);
            statement.setLong(2, productId);
            statement.setLong(3, lotId);
            statement.setBigDecimal(4, qty);
            statement.executeUpdate();
        }
    }

    public int deleteItem(Connection connection, long transferId, long itemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM stock_transfer_items WHERE id = ? AND transfer_id = ?")) {
            statement.setLong(1, itemId);
            statement.setLong(2, transferId);
            return statement.executeUpdate();
        }
    }

    public List<StockTransfer.Item> findItems(Connection connection, long transferId) throws SQLException {
        String sql = "SELECT i.id, i.product_id, i.lot_id, p.sku, p.name, lo.lot_no, u.name AS unit_name,"
                + " i.qty_sent_base, i.qty_received_base, i.variance_reason FROM stock_transfer_items i"
                + " JOIN products p ON p.id = i.product_id JOIN lots lo ON lo.id = i.lot_id"
                + " JOIN units u ON u.id = p.base_unit_id WHERE i.transfer_id = ? ORDER BY i.product_id, i.lot_id";
        List<StockTransfer.Item> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transferId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new StockTransfer.Item(resultSet.getLong("id"), resultSet.getLong("product_id"),
                            resultSet.getLong("lot_id"), resultSet.getString("sku"), resultSet.getString("name"),
                            resultSet.getString("lot_no"), resultSet.getString("unit_name"),
                            resultSet.getBigDecimal("qty_sent_base"), resultSet.getBigDecimal("qty_received_base"),
                            resultSet.getString("variance_reason")));
                }
            }
        }
        return items;
    }

    public void setReceived(Connection connection, long itemId, BigDecimal received, String reason)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_transfer_items SET qty_received_base = ?, variance_reason = ? WHERE id = ?")) {
            statement.setBigDecimal(1, received);
            if (reason == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, reason);
            }
            statement.setLong(3, itemId);
            statement.executeUpdate();
        }
    }

    public void markShipped(Connection connection, long id, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_transfers SET status = 'IN_TRANSIT', shipped_at = UTC_TIMESTAMP(), shipped_by = ?,"
                        + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    public void markReceived(Connection connection, long id, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_transfers SET status = 'RECEIVED', received_at = UTC_TIMESTAMP(), received_by = ?,"
                        + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    public void setStatus(Connection connection, long id, String status, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_transfers SET status = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    // Khoá và đọc {tồn thực tế, giữ chỗ} của (kho, SKU); null nếu SKU chưa có dòng tồn ở kho
    public BigDecimal[] lockBalance(Connection connection, long warehouseId, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT qty_on_hand_base, qty_reserved_base FROM stock_balances WHERE warehouse_id = ?"
                        + " AND product_id = ? FOR UPDATE")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? new BigDecimal[] {resultSet.getBigDecimal(1), resultSet.getBigDecimal(2)} : null;
            }
        }
    }

    public void updateBalance(Connection connection, long warehouseId, long productId, BigDecimal onHand)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_balances SET qty_on_hand_base = ?, version = version + 1, updated_at = UTC_TIMESTAMP()"
                        + " WHERE warehouse_id = ? AND product_id = ?")) {
            statement.setBigDecimal(1, onHand);
            statement.setLong(2, warehouseId);
            statement.setLong(3, productId);
            statement.executeUpdate();
        }
    }

    // Kho đến chưa từng có SKU này thì tạo dòng tồn (0) trước khi cộng
    public void ensureBalance(Connection connection, long warehouseId, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO stock_balances (warehouse_id, product_id, updated_at) VALUES (?, ?, UTC_TIMESTAMP())"
                        + " ON DUPLICATE KEY UPDATE warehouse_id = warehouse_id")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            statement.executeUpdate();
        }
    }

    public BigDecimal lockLotBalance(Connection connection, long warehouseId, long lotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO stock_lot_balances (warehouse_id, lot_id, updated_at) VALUES (?, ?, UTC_TIMESTAMP())"
                        + " ON DUPLICATE KEY UPDATE warehouse_id = warehouse_id")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, lotId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT qty_on_hand_base FROM stock_lot_balances WHERE warehouse_id = ? AND lot_id = ? FOR UPDATE")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, lotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    public void updateLotBalance(Connection connection, long warehouseId, long lotId, BigDecimal onHand)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_lot_balances SET qty_on_hand_base = ?, updated_at = UTC_TIMESTAMP()"
                        + " WHERE warehouse_id = ? AND lot_id = ?")) {
            statement.setBigDecimal(1, onHand);
            statement.setLong(2, warehouseId);
            statement.setLong(3, lotId);
            statement.executeUpdate();
        }
    }

    // Thẻ kho: TRANSFER_OUT (âm) ở kho đi lúc xuất, TRANSFER_IN (dương) ở kho đến lúc nhận
    public void insertMovement(Connection connection, long warehouseId, long productId, long lotId, String type,
                               BigDecimal change, BigDecimal balanceAfter, long transferId, String note,
                               long actorUserId) throws SQLException {
        String sql = "INSERT INTO stock_movements (warehouse_id, product_id, lot_id, movement_type, qty_change_base,"
                + " balance_after_base, ref_type, ref_id, occurred_at, note, created_at, created_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, 'STOCK_TRANSFER', ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            statement.setLong(3, lotId);
            statement.setString(4, type);
            statement.setBigDecimal(5, change);
            statement.setBigDecimal(6, balanceAfter);
            statement.setLong(7, transferId);
            if (note == null) {
                statement.setNull(8, Types.VARCHAR);
            } else {
                statement.setString(8, note);
            }
            statement.setLong(9, actorUserId);
            statement.executeUpdate();
        }
    }

    private static StockTransfer readHeader(ResultSet resultSet, List<StockTransfer.Item> items) throws SQLException {
        return new StockTransfer(resultSet.getLong("id"), resultSet.getString("transfer_no"),
                resultSet.getLong("from_warehouse_id"), resultSet.getString("from_name"),
                resultSet.getLong("to_warehouse_id"), resultSet.getString("to_name"), resultSet.getString("status"),
                resultSet.getObject("shipped_at", LocalDateTime.class), resultSet.getString("shipped_by_name"),
                resultSet.getObject("received_at", LocalDateTime.class), resultSet.getString("received_by_name"),
                resultSet.getString("note"), resultSet.getString("created_by_name"),
                resultSet.getObject("created_at", LocalDateTime.class), items);
    }

    // Phiếu đang khoá trong transaction
    public static class Locked {
        private final String transferNo;
        private final long fromWarehouseId;
        private final long toWarehouseId;
        private final String status;

        Locked(String transferNo, long fromWarehouseId, long toWarehouseId, String status) {
            this.transferNo = transferNo;
            this.fromWarehouseId = fromWarehouseId;
            this.toWarehouseId = toWarehouseId;
            this.status = status;
        }

        public String getTransferNo() {
            return transferNo;
        }

        public long getFromWarehouseId() {
            return fromWarehouseId;
        }

        public long getToWarehouseId() {
            return toWarehouseId;
        }

        public String getStatus() {
            return status;
        }
    }
}
