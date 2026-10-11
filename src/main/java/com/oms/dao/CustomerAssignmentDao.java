package com.oms.dao;

import com.oms.model.AssignmentHistoryEntry;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Phân công nhân viên kinh doanh phụ trách đại lý (S3-06): đổi customers.sales_rep_id, ghi
// customer_assignment_history; chuyển giao hàng loạt kèm địa bàn (user_regions) khi nhân viên nghỉ.
public class CustomerAssignmentDao {

    // Người nhận phân công: nhân viên kinh doanh đang hoạt động; code = địa bàn đang phụ trách (vd "Hà Nội, Bắc Ninh")
    // để chọn đúng người theo khu vực
    public List<SelectOption> findActiveSalesReps() throws SQLException {
        String sql = "SELECT u.id, u.full_name, (SELECT GROUP_CONCAT(g.name ORDER BY g.name SEPARATOR ', ')"
                + " FROM user_regions ur2 JOIN regions g ON g.id = ur2.region_id WHERE ur2.user_id = u.id) AS regions"
                + " FROM users u WHERE u.status = 'ACTIVE' AND u.id IN (SELECT ur.user_id FROM user_roles ur"
                + " JOIN roles r ON r.id = ur.role_id WHERE r.code = 'SALES_REP') ORDER BY u.full_name";
        List<SelectOption> reps = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                reps.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("regions"),
                        resultSet.getString("full_name")));
            }
        }
        return reps;
    }

    // Lịch sử của một đại lý, mới nhất trước
    public List<AssignmentHistoryEntry> findHistory(long customerId) throws SQLException {
        String sql = "SELECT f.full_name AS from_name, t.full_name AS to_name, h.reason, h.created_at,"
                + " a.full_name AS actor_name FROM customer_assignment_history h"
                + " LEFT JOIN users f ON f.id = h.from_sales_rep_id JOIN users t ON t.id = h.to_sales_rep_id"
                + " LEFT JOIN users a ON a.id = h.created_by WHERE h.customer_id = ? ORDER BY h.created_at DESC, h.id DESC";
        List<AssignmentHistoryEntry> entries = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new AssignmentHistoryEntry(resultSet.getString("from_name"),
                            resultSet.getString("to_name"), resultSet.getString("reason"),
                            resultSet.getObject("created_at", LocalDateTime.class), resultSet.getString("actor_name")));
                }
            }
        }
        return entries;
    }

    // Đại lý sẽ chuyển giao: của người giao, trong khu vực đã chọn (regionId null = mọi khu vực)
    public List<SelectOption> findCustomersOf(Connection connection, long salesRepId, Long regionId)
            throws SQLException {
        String sql = "SELECT id, code, name FROM customers WHERE sales_rep_id = ?"
                + (regionId == null ? "" : " AND region_id = ?") + " ORDER BY code";
        List<SelectOption> customers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, salesRepId);
            if (regionId != null) {
                statement.setLong(2, regionId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    customers.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("code"),
                            resultSet.getString("name")));
                }
            }
        }
        return customers;
    }

    public List<SelectOption> findCustomersOf(long salesRepId, Long regionId) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            return findCustomersOf(connection, salesRepId, regionId);
        }
    }

    // Địa bàn người giao đang phụ trách (trong khu vực đã chọn)
    public List<String> findRegionNames(long userId, Long regionId) throws SQLException {
        String sql = "SELECT g.name FROM user_regions ur JOIN regions g ON g.id = ur.region_id WHERE ur.user_id = ?"
                + (regionId == null ? "" : " AND ur.region_id = ?") + " ORDER BY g.name";
        List<String> names = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            if (regionId != null) {
                statement.setLong(2, regionId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
            }
        }
        return names;
    }

    // Chỉ đổi khi người phụ trách vẫn là expectedSalesRepId (null = chưa có ai): false nếu người khác vừa đổi
    public boolean updateSalesRep(Connection connection, long customerId, Long expectedSalesRepId, long toSalesRepId,
                                  long actorUserId) throws SQLException {
        String sql = "UPDATE customers SET sales_rep_id = ?, version = version + 1, updated_at = UTC_TIMESTAMP(),"
                + " updated_by = ? WHERE id = ? AND sales_rep_id <=> ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, toSalesRepId);
            statement.setLong(2, actorUserId);
            statement.setLong(3, customerId);
            setNullableLong(statement, 4, expectedSalesRepId);
            return statement.executeUpdate() == 1;
        }
    }

    public void insertHistory(Connection connection, long customerId, Long fromSalesRepId, long toSalesRepId,
                              String reason, long actorUserId) throws SQLException {
        String sql = "INSERT INTO customer_assignment_history (customer_id, from_sales_rep_id, to_sales_rep_id, reason,"
                + " created_at, created_by) VALUES (?, ?, ?, ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            setNullableLong(statement, 2, fromSalesRepId);
            statement.setLong(3, toSalesRepId);
            statement.setString(4, reason);
            statement.setLong(5, actorUserId);
            statement.executeUpdate();
        }
    }

    // INSERT IGNORE: người nhận có thể đã phụ trách sẵn địa bàn đó
    public void transferRegions(Connection connection, long fromUserId, long toUserId, Long regionId)
            throws SQLException {
        String where = " WHERE user_id = ?" + (regionId == null ? "" : " AND region_id = ?");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO user_regions (user_id, region_id) SELECT ?, region_id FROM user_regions" + where)) {
            statement.setLong(1, toUserId);
            statement.setLong(2, fromUserId);
            if (regionId != null) {
                statement.setLong(3, regionId);
            }
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM user_regions" + where)) {
            statement.setLong(1, fromUserId);
            if (regionId != null) {
                statement.setLong(2, regionId);
            }
            statement.executeUpdate();
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
