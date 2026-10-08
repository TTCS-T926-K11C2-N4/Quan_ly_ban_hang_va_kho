package com.oms.dao;

import com.oms.model.CustomerProfile;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

// Hồ sơ đại lý (S3-03): thêm, sửa, ngừng / giao dịch lại, xoá đại lý chưa phát sinh giao dịch.
// Hạn mức (S3-05), khoá giao dịch (S3-07), điểm giao (S3-04), phân công (S3-06) ghi ở DAO riêng.
public class CustomerProfileDao {

    // Bảng có khoá ngoại tới customers ngoài điểm giao: có dòng nào là đại lý đã phát sinh giao dịch (hoặc đã có
    // lịch sử không xoá được), nên không xoá đại lý được
    private static final String HAS_TRANSACTIONS_SQL = "(EXISTS (SELECT 1 FROM sales_orders x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM invoices x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM payments x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM ar_ledger_entries x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM customer_balances x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM credit_notes x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM sales_returns x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM reconciliation_statements x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM credit_limit_history x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM customer_block_history x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM customer_assignment_history x WHERE x.customer_id = c.id)"
            + " OR EXISTS (SELECT 1 FROM users x WHERE x.customer_id = c.id))";

    public CustomerProfile findProfile(long id) throws SQLException {
        String sql = "SELECT c.id, c.code, c.name, c.tax_code, c.phone, c.email, c.address, c.customer_group_id,"
                + " g.name AS group_name, c.region_id, r.name AS region_name, c.sales_rep_id, u.full_name AS rep_name,"
                + " c.default_warehouse_id, w.name AS warehouse_name, c.status, c.is_blocked, c.version, "
                + HAS_TRANSACTIONS_SQL + " AS has_transactions FROM customers c"
                + " JOIN customer_groups g ON g.id = c.customer_group_id JOIN regions r ON r.id = c.region_id"
                + " LEFT JOIN users u ON u.id = c.sales_rep_id LEFT JOIN warehouses w ON w.id = c.default_warehouse_id"
                + " WHERE c.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new CustomerProfile(resultSet.getLong("id"), resultSet.getString("code"),
                        resultSet.getString("name"), resultSet.getString("tax_code"), resultSet.getString("phone"),
                        resultSet.getString("email"), resultSet.getString("address"),
                        resultSet.getLong("customer_group_id"), resultSet.getString("group_name"),
                        resultSet.getLong("region_id"), resultSet.getString("region_name"),
                        resultSet.getObject("sales_rep_id", Long.class), resultSet.getString("rep_name"),
                        resultSet.getObject("default_warehouse_id", Long.class), resultSet.getString("warehouse_name"),
                        resultSet.getString("status"), resultSet.getBoolean("is_blocked"),
                        resultSet.getBoolean("has_transactions"), resultSet.getLong("version"));
            }
        }
    }

    // Mã đại lý đang dùng (excludeId: đại lý đang sửa); so không phân biệt hoa thường theo collation của bảng
    public boolean existsCode(String code, Long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM customers WHERE code = ?" + (excludeId == null ? "" : " AND id <> ?");
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Số lớn nhất của mã dạng DL00128 -> 128, để gợi ý mã kế tiếp; 0 nếu chưa có
    public int findMaxCodeNumber(String prefix) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(code, ?) AS UNSIGNED)), 0) FROM customers"
                + " WHERE code REGEXP ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, prefix.length() + 1);
            statement.setString(2, "^" + prefix + "[0-9]+$");
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    // Giá trị đã kiểm tra của form, ghi vào customers
    public record Values(String code, String name, String taxCode, String phone, String email, String address,
                         long customerGroupId, long regionId, Long salesRepId, long defaultWarehouseId,
                         String status) {
    }

    public long insert(Connection connection, Values values, long actorUserId) throws SQLException {
        String sql = "INSERT INTO customers (code, name, tax_code, phone, email, address, customer_group_id, region_id,"
                + " sales_rep_id, default_warehouse_id, status, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, values.code());
            int index = bindValues(statement, 2, values);
            statement.setLong(index++, actorUserId);
            statement.setLong(index, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // Mã đại lý không đổi sau khi tạo. false nếu người khác vừa sửa (version đã khác).
    public boolean update(Connection connection, long id, long version, Values values, long actorUserId)
            throws SQLException {
        String sql = "UPDATE customers SET name = ?, tax_code = ?, phone = ?, email = ?, address = ?,"
                + " customer_group_id = ?, region_id = ?, sales_rep_id = ?, default_warehouse_id = ?, status = ?,"
                + " version = version + 1, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ? AND version = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindValues(statement, 1, values);
            statement.setLong(index++, actorUserId);
            statement.setLong(index++, id);
            statement.setLong(index, version);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean updateStatus(Connection connection, long id, String status, long actorUserId) throws SQLException {
        String sql = "UPDATE customers SET status = ?, version = version + 1, updated_at = UTC_TIMESTAMP(),"
                + " updated_by = ? WHERE id = ? AND status <> ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.setString(4, status);
            return statement.executeUpdate() == 1;
        }
    }

    // Chỉ gọi khi đại lý chưa phát sinh giao dịch: xoá điểm giao rồi xoá đại lý
    public void delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM customer_delivery_addresses WHERE customer_id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM customers WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private static int bindValues(PreparedStatement statement, int start, Values values) throws SQLException {
        int index = start;
        statement.setString(index++, values.name());
        statement.setString(index++, values.taxCode());
        statement.setString(index++, values.phone());
        statement.setString(index++, values.email());
        statement.setString(index++, values.address());
        statement.setLong(index++, values.customerGroupId());
        statement.setLong(index++, values.regionId());
        if (values.salesRepId() == null) {
            statement.setNull(index++, Types.BIGINT);
        } else {
            statement.setLong(index++, values.salesRepId());
        }
        statement.setLong(index++, values.defaultWarehouseId());
        statement.setString(index++, values.status());
        return index;
    }
}
