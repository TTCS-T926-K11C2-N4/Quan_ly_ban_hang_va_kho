package com.oms.dao;

import com.oms.model.Stocktake;
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

// Phiếu kiểm kê (stocktakes, stocktake_items) và các thao tác điều chỉnh tồn khi chốt phiếu (S5-08).
// Các hàm nhận Connection chạy trong transaction của service.
public class StocktakeDao {

    private static final String HEADER_SELECT = "SELECT s.id, s.stocktake_no, s.warehouse_id, w.name AS warehouse_name,"
            + " s.scope_type, c.name AS category_name, s.status, s.snapshot_at, fb.full_name AS finalized_by_name,"
            + " s.finalized_at, s.reason, cb.full_name AS created_by_name,"
            + " (SELECT COUNT(*) FROM stocktake_items i WHERE i.stocktake_id = s.id) AS item_count,"
            + " (SELECT COUNT(*) FROM stocktake_items i WHERE i.stocktake_id = s.id AND i.counted_qty_base IS NOT NULL)"
            + " AS counted_count,"
            + " (SELECT COUNT(*) FROM stocktake_items i WHERE i.stocktake_id = s.id AND i.variance_base <> 0)"
            + " AS variance_count FROM stocktakes s JOIN warehouses w ON w.id = s.warehouse_id"
            + " LEFT JOIN product_categories c ON c.id = s.category_id LEFT JOIN users fb ON fb.id = s.finalized_by"
            + " LEFT JOIN users cb ON cb.id = s.created_by";

    // Số thứ tự lớn nhất của các mã phiếu bắt đầu bằng prefix (vd KK261015-0003 -> 3); 0 nếu chưa có
    public int findMaxNumber(String prefix) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(stocktake_no, ?) AS UNSIGNED)), 0) FROM stocktakes"
                + " WHERE stocktake_no LIKE ?";
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
    public long insert(Connection connection, String stocktakeNo, long warehouseId, Long categoryId,
                       long actorUserId) throws SQLException {
        String sql = "INSERT INTO stocktakes (stocktake_no, warehouse_id, scope_type, category_id, status, snapshot_at,"
                + " created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, 'DRAFT', UTC_TIMESTAMP(), UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, stocktakeNo);
            statement.setLong(2, warehouseId);
            statement.setString(3, categoryId == null ? "WAREHOUSE" : "CATEGORY");
            if (categoryId == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, categoryId);
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

    // Chụp số tồn sổ theo lô của kho (kèm nhóm hàng và nhóm con nếu có); trả về số dòng đã chụp
    public int insertSnapshot(Connection connection, long stocktakeId, long warehouseId, Long categoryId)
            throws SQLException {
        String sql = "INSERT INTO stocktake_items (stocktake_id, product_id, lot_id, system_qty_base)"
                + " SELECT ?, l.product_id, b.lot_id, b.qty_on_hand_base FROM stock_lot_balances b"
                + " JOIN lots l ON l.id = b.lot_id JOIN products p ON p.id = l.product_id"
                + " JOIN product_categories c ON c.id = p.category_id WHERE b.warehouse_id = ?"
                + " AND (? IS NULL OR c.path LIKE CONCAT((SELECT pc.path FROM product_categories pc WHERE pc.id = ?), '%'))"
                + " ORDER BY p.sku, l.lot_no";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, stocktakeId);
            statement.setLong(2, warehouseId);
            if (categoryId == null) {
                statement.setNull(3, Types.BIGINT);
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(3, categoryId);
                statement.setLong(4, categoryId);
            }
            return statement.executeUpdate();
        }
    }

    public List<Stocktake> findAll(int limit) throws SQLException {
        String sql = HEADER_SELECT + " ORDER BY s.created_at DESC, s.id DESC LIMIT ?";
        List<Stocktake> result = new ArrayList<>();
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

    // null nếu không có phiếu này
    public Stocktake findDetail(long id) throws SQLException {
        String itemSql = "SELECT i.id, i.product_id, p.sku, p.name, lo.lot_no, u.name AS unit_name, i.system_qty_base,"
                + " i.counted_qty_base FROM stocktake_items i JOIN products p ON p.id = i.product_id"
                + " JOIN lots lo ON lo.id = i.lot_id JOIN units u ON u.id = p.base_unit_id WHERE i.stocktake_id = ?"
                + " ORDER BY p.sku, lo.lot_no";
        try (Connection connection = DbConnection.getConnection()) {
            List<Stocktake.Item> items = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        items.add(new Stocktake.Item(resultSet.getLong("id"), resultSet.getLong("product_id"),
                                resultSet.getString("sku"), resultSet.getString("name"), resultSet.getString("lot_no"),
                                resultSet.getString("unit_name"), resultSet.getBigDecimal("system_qty_base"),
                                resultSet.getBigDecimal("counted_qty_base")));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(HEADER_SELECT + " WHERE s.id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? readHeader(resultSet, items) : null;
                }
            }
        }
    }

    // Trạng thái hiện tại của phiếu, khoá dòng; null nếu không có phiếu
    public Locked lock(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stocktake_no, warehouse_id, status FROM stocktakes WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new Locked(resultSet.getString(1), resultSet.getLong(2),
                        resultSet.getString(3)) : null;
            }
        }
    }

    // Số đếm null = bỏ số đã nhập. Chỉ sửa dòng thuộc phiếu; trả về số dòng được ghi.
    public int saveCount(Connection connection, long stocktakeId, long itemId, BigDecimal counted)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stocktake_items SET counted_qty_base = ? WHERE id = ? AND stocktake_id = ?")) {
            if (counted == null) {
                statement.setNull(1, Types.DECIMAL);
            } else {
                statement.setBigDecimal(1, counted);
            }
            statement.setLong(2, itemId);
            statement.setLong(3, stocktakeId);
            return statement.executeUpdate();
        }
    }

    public void setStatus(Connection connection, long id, String status, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stocktakes SET status = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    public void complete(Connection connection, long id, String reason, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stocktakes SET status = 'COMPLETED', reason = ?, finalized_by = ?,"
                        + " finalized_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setString(1, reason);
            statement.setLong(2, actorUserId);
            statement.setLong(3, actorUserId);
            statement.setLong(4, id);
            statement.executeUpdate();
        }
    }

    // Các dòng đã đếm của phiếu để chốt, theo thứ tự SKU, lô để các phiên chốt khoá dòng cùng một thứ tự
    public List<FinalizeItem> findFinalizeItems(Connection connection, long stocktakeId) throws SQLException {
        String sql = "SELECT i.product_id, i.lot_id, p.sku, lo.lot_no, i.system_qty_base, i.counted_qty_base"
                + " FROM stocktake_items i JOIN products p ON p.id = i.product_id JOIN lots lo ON lo.id = i.lot_id"
                + " WHERE i.stocktake_id = ? ORDER BY i.product_id, i.lot_id";
        List<FinalizeItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, stocktakeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new FinalizeItem(resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3),
                            resultSet.getString(4), resultSet.getBigDecimal(5), resultSet.getBigDecimal(6)));
                }
            }
        }
        return items;
    }

    // Khoá và đọc tồn thực tế, giữ chỗ của (kho, SKU); null nếu SKU chưa có dòng tồn ở kho
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

    // Tồn của lô, khoá dòng; 0 nếu lô chưa có dòng tồn ở kho (dòng được tạo)
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

    // Thẻ kho: mỗi dòng chênh lệch một bút toán STOCKTAKE trỏ về phiếu kiểm kê
    public void insertMovement(Connection connection, long warehouseId, long productId, long lotId, BigDecimal change,
                               BigDecimal balanceAfter, long stocktakeId, String note, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO stock_movements (warehouse_id, product_id, lot_id, movement_type, qty_change_base,"
                + " balance_after_base, ref_type, ref_id, occurred_at, note, created_at, created_by)"
                + " VALUES (?, ?, ?, 'STOCKTAKE', ?, ?, 'STOCKTAKE', ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            statement.setLong(3, lotId);
            statement.setBigDecimal(4, change);
            statement.setBigDecimal(5, balanceAfter);
            statement.setLong(6, stocktakeId);
            statement.setString(7, note);
            statement.setLong(8, actorUserId);
            statement.executeUpdate();
        }
    }

    private static Stocktake readHeader(ResultSet resultSet, List<Stocktake.Item> items) throws SQLException {
        return new Stocktake(resultSet.getLong("id"), resultSet.getString("stocktake_no"),
                resultSet.getLong("warehouse_id"), resultSet.getString("warehouse_name"),
                resultSet.getString("scope_type"), resultSet.getString("category_name"), resultSet.getString("status"),
                resultSet.getObject("snapshot_at", LocalDateTime.class), resultSet.getString("finalized_by_name"),
                resultSet.getObject("finalized_at", LocalDateTime.class), resultSet.getString("reason"),
                resultSet.getString("created_by_name"), resultSet.getInt("item_count"),
                resultSet.getInt("counted_count"), resultSet.getInt("variance_count"), items);
    }

    // Phiếu đang khoá trong transaction
    public static class Locked {
        private final String stocktakeNo;
        private final long warehouseId;
        private final String status;

        Locked(String stocktakeNo, long warehouseId, String status) {
            this.stocktakeNo = stocktakeNo;
            this.warehouseId = warehouseId;
            this.status = status;
        }

        public String getStocktakeNo() {
            return stocktakeNo;
        }

        public long getWarehouseId() {
            return warehouseId;
        }

        public String getStatus() {
            return status;
        }
    }

    // Dòng của phiếu khi chốt; countedQty null = chưa đếm
    public static class FinalizeItem {
        private final long productId;
        private final long lotId;
        private final String sku;
        private final String lotNo;
        private final BigDecimal systemQty;
        private final BigDecimal countedQty;

        FinalizeItem(long productId, long lotId, String sku, String lotNo, BigDecimal systemQty,
                     BigDecimal countedQty) {
            this.productId = productId;
            this.lotId = lotId;
            this.sku = sku;
            this.lotNo = lotNo;
            this.systemQty = systemQty;
            this.countedQty = countedQty;
        }

        public long getProductId() {
            return productId;
        }

        public long getLotId() {
            return lotId;
        }

        public String getSku() {
            return sku;
        }

        public String getLotNo() {
            return lotNo;
        }

        public BigDecimal getSystemQty() {
            return systemQty;
        }

        public BigDecimal getCountedQty() {
            return countedQty;
        }
    }
}
