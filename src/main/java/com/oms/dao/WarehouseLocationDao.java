package com.oms.dao;

import com.oms.model.WarehouseLocation;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// Vị trí lưu trong kho (warehouse_locations, S5-03): khu và kệ
public class WarehouseLocationDao {

    private static final String COLUMNS = "l.id, l.warehouse_id, l.parent_id, l.code, l.name, l.location_type,"
            + " l.pick_sequence, l.is_active,"
            + " (SELECT COUNT(*) FROM stock_balances b WHERE b.location_id = l.id) AS sku_count";

    // Theo thứ tự đi soạn hàng rồi theo mã
    public List<WarehouseLocation> findByWarehouse(long warehouseId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM warehouse_locations l WHERE l.warehouse_id = ?"
                + " ORDER BY l.pick_sequence, l.code";
        List<WarehouseLocation> locations = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    locations.add(map(resultSet));
                }
            }
        }
        return locations;
    }

    public WarehouseLocation findById(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + COLUMNS + " FROM warehouse_locations l WHERE l.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    // Mã vị trí chỉ cần duy nhất trong một kho (ux_warehouse_locations_warehouse_id_code)
    public boolean existsCode(long warehouseId, String code, Long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM warehouse_locations WHERE warehouse_id = ? AND code = ?"
                + (excludeId == null ? "" : " AND id <> ?") + " LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setString(2, code);
            if (excludeId != null) {
                statement.setLong(3, excludeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public long insert(Connection connection, long warehouseId, Long parentId, String type, String code, String name,
                       int pickSequence, long actorUserId) throws SQLException {
        String sql = "INSERT INTO warehouse_locations (warehouse_id, parent_id, location_type, code, name, pick_sequence,"
                + " is_active, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, true, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, warehouseId);
            setNullableLong(statement, 2, parentId);
            statement.setString(3, type);
            statement.setString(4, code);
            statement.setString(5, name);
            statement.setInt(6, pickSequence);
            statement.setLong(7, actorUserId);
            statement.setLong(8, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public boolean update(Connection connection, long id, Long parentId, String type, String code, String name,
                          int pickSequence, long actorUserId) throws SQLException {
        String sql = "UPDATE warehouse_locations SET parent_id = ?, location_type = ?, code = ?, name = ?,"
                + " pick_sequence = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableLong(statement, 1, parentId);
            statement.setString(2, type);
            statement.setString(3, code);
            statement.setString(4, name);
            statement.setInt(5, pickSequence);
            statement.setLong(6, actorUserId);
            statement.setLong(7, id);
            return statement.executeUpdate() == 1;
        }
    }

    // false nếu đã ở trạng thái đó (vd bấm hai lần)
    public boolean updateActive(Connection connection, long id, boolean active, long actorUserId) throws SQLException {
        String sql = "UPDATE warehouse_locations SET is_active = ?, updated_at = UTC_TIMESTAMP(), updated_by = ?"
                + " WHERE id = ? AND is_active <> ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.setBoolean(4, active);
            return statement.executeUpdate() == 1;
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static WarehouseLocation map(ResultSet resultSet) throws SQLException {
        return new WarehouseLocation(resultSet.getLong("id"), resultSet.getLong("warehouse_id"),
                resultSet.getObject("parent_id", Long.class), resultSet.getString("code"), resultSet.getString("name"),
                resultSet.getString("location_type"), resultSet.getInt("pick_sequence"),
                resultSet.getBoolean("is_active"), resultSet.getInt("sku_count"));
    }
}
