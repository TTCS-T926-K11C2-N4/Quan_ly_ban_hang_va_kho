package com.oms.dao;

import com.oms.model.Customer;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Đại lý (customers): đọc thông tin và danh sách đại lý. Hạn mức (S3-05), khoá giao dịch (S3-07),
// điểm giao (S3-09) ghi/đọc ở DAO riêng của từng chức năng.
public class CustomerDao {

    private static final String CONTACT_SQL = "(SELECT a.receiver_name FROM customer_delivery_addresses a"
            + " WHERE a.customer_id = c.id AND a.is_active ORDER BY a.is_default DESC, a.id LIMIT 1)";
    private static final String COLUMNS = "c.id, c.code, c.name, c.phone, c.address, " + CONTACT_SQL
            + " AS contact_name, c.sales_rep_id, c.customer_group_id, c.default_warehouse_id, c.credit_limit,"
            + " c.max_debt_days, c.is_blocked, c.block_reason, c.status, c.version";

    public Customer findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM customers c WHERE c.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    // Đại lý để chọn (kèm trạng thái khoá); salesRepId = null: mọi đại lý, ngược lại chỉ đại lý người đó phụ trách
    public List<Customer> findAll(Long salesRepId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM customers c"
                + (salesRepId == null ? "" : " WHERE c.sales_rep_id = ?") + " ORDER BY c.name, c.code";
        List<Customer> customers = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (salesRepId != null) {
                statement.setLong(1, salesRepId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    customers.add(map(resultSet));
                }
            }
        }
        return customers;
    }

    private static Customer map(ResultSet resultSet) throws SQLException {
        return new Customer(resultSet.getLong("id"), resultSet.getString("code"), resultSet.getString("name"),
                resultSet.getString("phone"), resultSet.getString("address"), resultSet.getString("contact_name"),
                resultSet.getObject("sales_rep_id", Long.class), resultSet.getLong("customer_group_id"),
                resultSet.getObject("default_warehouse_id", Long.class), resultSet.getBigDecimal("credit_limit"),
                resultSet.getInt("max_debt_days"), resultSet.getBoolean("is_blocked"),
                resultSet.getString("block_reason"), resultSet.getString("status"), resultSet.getLong("version"));
    }
}
