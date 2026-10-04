package com.oms.dao;

import com.oms.model.SelectOption;
import com.oms.model.UnitRow;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Danh mục đơn vị tính (units): lon, lốc, thùng, kg... Quy đổi theo từng SKU nằm ở product_units (S2-07).
public class UnitDao {

    // Số SKU dùng đơn vị (làm đơn vị cơ sở hoặc đơn vị quy đổi) để biết có xoá được không
    private static final String USAGE_SQL = "(SELECT COUNT(DISTINCT p.id) FROM products p"
            + " LEFT JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id"
            + " WHERE p.base_unit_id = u.id OR pu.unit_id IS NOT NULL)";

    public List<SelectOption> findAll() throws SQLException {
        List<SelectOption> units = new ArrayList<>();
        String sql = "SELECT id, code, name FROM units ORDER BY name";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                units.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("code"),
                        resultSet.getString("name")));
            }
        }
        return units;
    }

    public List<UnitRow> findAllWithUsage() throws SQLException {
        List<UnitRow> units = new ArrayList<>();
        String sql = "SELECT u.id, u.code, u.name, " + USAGE_SQL + " AS product_count FROM units u ORDER BY u.name";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                units.add(map(resultSet));
            }
        }
        return units;
    }

    public UnitRow findById(long id) throws SQLException {
        String sql = "SELECT u.id, u.code, u.name, " + USAGE_SQL + " AS product_count FROM units u WHERE u.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public boolean exists(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM units WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // column: code | name. So khớp không phân biệt hoa thường (collation của bảng).
    public boolean existsValue(String column, String value, Long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM units WHERE " + ("code".equals(column) ? "code" : "name") + " = ?"
                + (excludeId == null ? "" : " AND id <> ?");
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public long insert(Connection connection, String code, String name, long actorUserId) throws SQLException {
        String sql = "INSERT INTO units (code, name, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, code);
            statement.setString(2, name);
            statement.setLong(3, actorUserId);
            statement.setLong(4, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, long id, String code, String name, long actorUserId)
            throws SQLException {
        String sql = "UPDATE units SET code = ?, name = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.setString(2, name);
            statement.setLong(3, actorUserId);
            statement.setLong(4, id);
            statement.executeUpdate();
        }
    }

    // Đơn vị còn được dùng (sản phẩm, bảng giá, chứng từ) thì khoá ngoại chặn: SQLIntegrityConstraintViolationException
    public void delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM units WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private static UnitRow map(ResultSet resultSet) throws SQLException {
        return new UnitRow(resultSet.getLong("id"), resultSet.getString("code"), resultSet.getString("name"),
                resultSet.getInt("product_count"));
    }
}
