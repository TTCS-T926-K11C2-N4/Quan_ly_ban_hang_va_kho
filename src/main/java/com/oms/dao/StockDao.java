package com.oms.dao;

import com.oms.model.OrderStock;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Tồn kho cho màn tạo đơn (S4-03): đọc tồn theo kho (stock_balances) và giữ chỗ khi gửi đơn (stock_reservations).
// Tổng giữ chỗ ACTIVE của một (kho, SKU) luôn bằng stock_balances.qty_reserved_base; CHECK của bảng chặn giữ quá tồn.
public class StockDao {

    // Tồn của mọi SKU trong kho; SKU chưa có dòng stock_balances (chưa từng nhập kho) không có trong map
    public Map<Long, OrderStock.Level> findLevels(long warehouseId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT product_id, qty_on_hand_base, qty_reserved_base FROM stock_balances WHERE warehouse_id = ?")) {
            statement.setLong(1, warehouseId);
            return readLevels(statement);
        }
    }

    // AC4: khoá các dòng tồn của đơn trong transaction gửi đơn, theo thứ tự product_id để hai đơn cùng SKU
    // không khoá chéo nhau (deadlock); đơn gửi sau chờ đơn trước rồi mới đọc số tồn
    public Map<Long, OrderStock.Level> lockLevels(Connection connection, long warehouseId, Collection<Long> productIds)
            throws SQLException {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = productIds.stream().distinct().sorted().toList();
        String sql = "SELECT product_id, qty_on_hand_base, qty_reserved_base FROM stock_balances WHERE warehouse_id = ?"
                + " AND product_id IN (" + ids.stream().map(id -> "?").collect(Collectors.joining(", ")) + ")"
                + " ORDER BY product_id FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            for (int i = 0; i < ids.size(); i++) {
                statement.setLong(i + 2, ids.get(i));
            }
            return readLevels(statement);
        }
    }

    public String findWarehouseName(long warehouseId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT name FROM warehouses WHERE id = ?")) {
            statement.setLong(1, warehouseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    // Giữ chỗ cho một dòng đơn vừa gửi: ghi stock_reservations và cộng qty_reserved_base trong cùng transaction
    public void reserve(Connection connection, long orderItemId, long warehouseId, long productId, BigDecimal qtyBase,
                        long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO stock_reservations (order_item_id, warehouse_id, product_id, qty_base, status, reserved_at,"
                        + " created_at, created_by) VALUES (?, ?, ?, ?, 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP(), ?)")) {
            statement.setLong(1, orderItemId);
            statement.setLong(2, warehouseId);
            statement.setLong(3, productId);
            statement.setBigDecimal(4, qtyBase);
            statement.setLong(5, actorUserId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE stock_balances SET qty_reserved_base = qty_reserved_base + ?, version = version + 1,"
                        + " updated_at = UTC_TIMESTAMP() WHERE warehouse_id = ? AND product_id = ?")) {
            statement.setBigDecimal(1, qtyBase);
            statement.setLong(2, warehouseId);
            statement.setLong(3, productId);
            statement.executeUpdate();
        }
    }

    private static Map<Long, OrderStock.Level> readLevels(PreparedStatement statement) throws SQLException {
        Map<Long, OrderStock.Level> levels = new HashMap<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                levels.put(resultSet.getLong("product_id"), new OrderStock.Level(
                        resultSet.getBigDecimal("qty_on_hand_base"), resultSet.getBigDecimal("qty_reserved_base")));
            }
        }
        return levels;
    }
}
