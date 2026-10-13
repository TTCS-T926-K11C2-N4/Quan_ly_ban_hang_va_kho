package com.oms.dao;

import com.oms.model.SelectOption;
import com.oms.model.Warehouse;
import com.oms.model.WarehouseFilter;
import com.oms.model.WarehouseForm;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class WarehouseDao {

    private static final String FIND_ACTIVE_SQL =
            "SELECT id, code, name FROM warehouses WHERE status = 'ACTIVE' ORDER BY name";

    // Danh sách kho (S5-03): kèm tên người phụ trách, số vị trí đã khai báo và còn hàng hay không
    private static final String COLUMNS = "w.id, w.code, w.name, w.description, w.address, w.manager_user_id,"
            + " u.full_name AS manager_name, w.warehouse_type, w.status,"
            + " (SELECT COUNT(*) FROM warehouse_locations l WHERE l.warehouse_id = w.id) AS location_count,"
            + " EXISTS (SELECT 1 FROM stock_balances b WHERE b.warehouse_id = w.id AND b.qty_on_hand_base > 0)"
            + " AS has_stock";
    private static final String FROM = " FROM warehouses w LEFT JOIN users u ON u.id = w.manager_user_id";
    private static final String FILTER_WHERE = " WHERE (? = '' OR w.code LIKE ? OR w.name LIKE ? OR w.address LIKE ?)"
            + " AND (? IS NULL OR w.warehouse_type = ?) AND (? IS NULL OR w.status = ?)";

    public List<SelectOption> findActive() throws SQLException {
        return findOptions(FIND_ACTIVE_SQL);
    }

    // Kho bán được đang hoạt động: chỉ những kho này làm kho phục vụ mặc định của đại lý (S5-03), kho hàng lỗi thì không
    public List<SelectOption> findActiveSellable() throws SQLException {
        return findOptions("SELECT id, code, name FROM warehouses WHERE status = 'ACTIVE'"
                + " AND warehouse_type = 'SELLABLE' ORDER BY name");
    }

    public long count(WarehouseFilter filter) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*)" + FROM + FILTER_WHERE)) {
            bindFilter(statement, filter);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // limit <= 0: lấy hết (xuất Excel)
    public List<Warehouse> findPage(WarehouseFilter filter, int offset, int limit) throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM + FILTER_WHERE + " ORDER BY w.code"
                + (limit > 0 ? " LIMIT ? OFFSET ?" : "");
        List<Warehouse> warehouses = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindFilter(statement, filter);
            if (limit > 0) {
                statement.setInt(next++, limit);
                statement.setInt(next, offset);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    warehouses.add(map(resultSet));
                }
            }
        }
        return warehouses;
    }

    // Ba thẻ số liệu đầu trang: tổng số kho, kho đang hoạt động, kho hàng lỗi (không theo bộ lọc)
    public long[] countSummary() throws SQLException {
        String sql = "SELECT COUNT(*), COALESCE(SUM(status = 'ACTIVE'), 0), COALESCE(SUM(warehouse_type = 'DEFECT'), 0)"
                + " FROM warehouses";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return new long[] {resultSet.getLong(1), resultSet.getLong(2), resultSet.getLong(3)};
        }
    }

    public Warehouse findById(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE w.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    // Tên kho đang dùng mã này (không phân biệt hoa thường theo collation); null nếu chưa kho nào dùng
    public String findNameUsingCode(String code, Long excludeId) throws SQLException {
        String sql = "SELECT name FROM warehouses WHERE code = ?" + (excludeId == null ? "" : " AND id <> ?") + " LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    // Người phụ trách kho chọn được: tài khoản đang hoạt động có vai trò Quản lý kho hoặc Nhân viên kho
    public List<SelectOption> findManagerCandidates() throws SQLException {
        return findOptions("SELECT DISTINCT u.id, u.username AS code, u.full_name AS name FROM users u"
                + " JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id"
                + " WHERE u.status = 'ACTIVE' AND r.code IN ('WH_MANAGER', 'WAREHOUSE') ORDER BY u.full_name");
    }

    // Đại lý đang lấy kho này làm kho phục vụ mặc định (không cho đổi sang kho hàng lỗi hoặc tạm ngừng)
    public int countCustomersServed(long warehouseId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM customers WHERE default_warehouse_id = ?")) {
            statement.setLong(1, warehouseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public long insert(Connection connection, WarehouseForm form, long actorUserId) throws SQLException {
        String sql = "INSERT INTO warehouses (code, name, description, address, manager_user_id, warehouse_type, status,"
                + " created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int next = bindForm(statement, form);
            statement.setLong(next++, actorUserId);
            statement.setLong(next, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public boolean update(Connection connection, long id, WarehouseForm form, long actorUserId) throws SQLException {
        String sql = "UPDATE warehouses SET code = ?, name = ?, description = ?, address = ?, manager_user_id = ?,"
                + " warehouse_type = ?, status = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindForm(statement, form);
            statement.setLong(next++, actorUserId);
            statement.setLong(next, id);
            return statement.executeUpdate() == 1;
        }
    }

    private static List<SelectOption> findOptions(String sql) throws SQLException {
        List<SelectOption> options = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                options.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("code"),
                        resultSet.getString("name")));
            }
        }
        return options;
    }

    private static int bindFilter(PreparedStatement statement, WarehouseFilter filter) throws SQLException {
        String keyword = filter.getKeyword();
        String like = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        statement.setString(1, keyword);
        statement.setString(2, like);
        statement.setString(3, like);
        statement.setString(4, like);
        statement.setString(5, filter.getType());
        statement.setString(6, filter.getType());
        statement.setString(7, filter.getStatus());
        statement.setString(8, filter.getStatus());
        return 9;
    }

    private static int bindForm(PreparedStatement statement, WarehouseForm form) throws SQLException {
        statement.setString(1, form.getCode());
        statement.setString(2, form.getName());
        statement.setString(3, form.getDescription());
        statement.setString(4, form.getAddress());
        if (form.getManagerId() == null) {
            statement.setNull(5, Types.BIGINT);
        } else {
            statement.setLong(5, form.getManagerId());
        }
        statement.setString(6, form.getType());
        statement.setString(7, form.getStatus());
        return 8;
    }

    private static Warehouse map(ResultSet resultSet) throws SQLException {
        return new Warehouse(resultSet.getLong("id"), resultSet.getString("code"), resultSet.getString("name"),
                resultSet.getString("description"), resultSet.getString("address"),
                resultSet.getObject("manager_user_id", Long.class), resultSet.getString("manager_name"),
                resultSet.getString("warehouse_type"), resultSet.getString("status"),
                resultSet.getInt("location_count"), resultSet.getBoolean("has_stock"));
    }
}
