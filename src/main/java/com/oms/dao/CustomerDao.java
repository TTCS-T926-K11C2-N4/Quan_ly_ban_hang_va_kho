package com.oms.dao;

import com.oms.model.Customer;
import com.oms.model.CustomerFilter;
import com.oms.model.CustomerListItem;
import com.oms.model.CustomerListStatus;
import com.oms.model.SelectOption;
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

    // S3-08: tìm kiếm, lọc danh sách đại lý. scopeSalesRepId khác null: chỉ đại lý người đó phụ trách (data_scope ASSIGNED)
    public long count(CustomerFilter filter, Long scopeSalesRepId) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM customers c" + buildWhere(filter, scopeSalesRepId, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public List<CustomerListItem> findPage(CustomerFilter filter, Long scopeSalesRepId, int offset, int limit)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        // Đại lý đang giao dịch lên trước, rồi theo mã để thứ tự không đổi giữa các trang
        String sql = "SELECT c.id, c.code, c.name, c.phone, c.is_blocked, c.status, r.name AS region_name,"
                + " g.name AS group_name, u.full_name AS sales_rep_name FROM customers c"
                + " JOIN regions r ON r.id = c.region_id JOIN customer_groups g ON g.id = c.customer_group_id"
                + " LEFT JOIN users u ON u.id = c.sales_rep_id" + buildWhere(filter, scopeSalesRepId, params)
                + " ORDER BY c.status = 'ACTIVE' AND NOT c.is_blocked DESC, c.code LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        List<CustomerListItem> customers = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    customers.add(new CustomerListItem(resultSet.getLong("id"), resultSet.getString("code"),
                            resultSet.getString("name"), resultSet.getString("phone"),
                            resultSet.getString("region_name"), resultSet.getString("group_name"),
                            resultSet.getString("sales_rep_name"),
                            CustomerListStatus.of(resultSet.getBoolean("is_blocked"),
                                    Customer.ACTIVE.equals(resultSet.getString("status")))));
                }
            }
        }
        return customers;
    }

    // Ô lọc "Người phụ trách": nhân viên kinh doanh, cùng người đang phụ trách đại lý dù đã đổi vai trò hay bị khoá,
    // để vẫn lọc được đại lý của họ
    public List<SelectOption> findSalesRepOptions() throws SQLException {
        String sql = "SELECT u.id, u.full_name FROM users u WHERE u.id IN (SELECT c.sales_rep_id FROM customers c)"
                + " OR u.id IN (SELECT ur.user_id FROM user_roles ur JOIN roles r ON r.id = ur.role_id"
                + " WHERE r.code = 'SALES_REP') ORDER BY u.full_name";
        List<SelectOption> options = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                options.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("full_name")));
            }
        }
        return options;
    }

    private static String buildWhere(CustomerFilter filter, Long scopeSalesRepId, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (scopeSalesRepId != null) {
            conditions.add("c.sales_rep_id = ?");
            params.add(scopeSalesRepId);
        }
        if (filter.getKeyword() != null) {
            String pattern = "%" + escapeLike(filter.getKeyword()) + "%";
            // Số điện thoại lưu có thể kèm dấu cách, chấm, gạch: so sánh phần chữ số để gõ liền vẫn tìm ra
            conditions.add("(c.code LIKE ? OR c.name LIKE ?"
                    + " OR REPLACE(REPLACE(REPLACE(c.phone, ' ', ''), '.', ''), '-', '') LIKE ?)");
            String digits = filter.getKeyword().replaceAll("[\\s.-]", "");
            params.add(pattern);
            params.add(pattern);
            params.add(digits.isEmpty() ? pattern : "%" + escapeLike(digits) + "%");
        }
        if (filter.getRegionId() != null) {
            conditions.add("c.region_id = ?");
            params.add(filter.getRegionId());
        }
        if (filter.getCustomerGroupId() != null) {
            conditions.add("c.customer_group_id = ?");
            params.add(filter.getCustomerGroupId());
        }
        if (filter.getSalesRepId() != null) {
            conditions.add("c.sales_rep_id = ?");
            params.add(filter.getSalesRepId());
        }
        if (filter.getStatus() != null) {
            conditions.add(switch (filter.getStatus()) {
                case BLOCKED -> "c.is_blocked";
                case ACTIVE -> "NOT c.is_blocked AND c.status = 'ACTIVE'";
                case INACTIVE -> "NOT c.is_blocked AND c.status = 'INACTIVE'";
            });
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    // Người dùng gõ % hoặc _ thì tìm đúng ký tự đó, không để thành ký tự đại diện của LIKE
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void bind(PreparedStatement statement, List<?> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
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
