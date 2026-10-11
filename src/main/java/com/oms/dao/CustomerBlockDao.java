package com.oms.dao;

import com.oms.model.CustomerBlockEntry;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

// Khoá / mở giao dịch đại lý (customers.is_blocked, block_reason) và customer_block_history (S3-07)
public class CustomerBlockDao {

    // Khoá (blocked = true, kèm lý do) hoặc mở giao dịch; false nếu đại lý đã ở trạng thái đó (người khác vừa làm,
    // hoặc bấm hai lần). Không so version để việc sửa hạn mức cùng lúc không làm hỏng thao tác khoá.
    public boolean updateBlocked(Connection connection, long id, boolean blocked, String reason, long actorUserId)
            throws SQLException {
        String sql = "UPDATE customers SET is_blocked = ?, block_reason = ?, version = version + 1,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ? AND is_blocked <> ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, blocked);
            statement.setString(2, blocked ? reason : null);
            statement.setLong(3, actorUserId);
            statement.setLong(4, id);
            statement.setBoolean(5, blocked);
            return statement.executeUpdate() == 1;
        }
    }

    public void insertBlockHistory(Connection connection, long customerId, String action, String reason,
                                   long actorUserId) throws SQLException {
        String sql = "INSERT INTO customer_block_history (customer_id, action, reason, created_at, created_by)"
                + " VALUES (?, ?, ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setString(2, action);
            statement.setString(3, reason);
            statement.setLong(4, actorUserId);
            statement.executeUpdate();
        }
    }

    // Lần khoá/mở gần nhất; null nếu đại lý chưa từng bị khoá
    public CustomerBlockEntry findLatestBlock(long customerId) throws SQLException {
        String sql = "SELECT h.action, h.reason, h.created_at, u.full_name FROM customer_block_history h"
                + " LEFT JOIN users u ON u.id = h.created_by WHERE h.customer_id = ? ORDER BY h.id DESC LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new CustomerBlockEntry(resultSet.getString("action"),
                        resultSet.getString("reason"), resultSet.getObject("created_at", LocalDateTime.class),
                        resultSet.getString("full_name")) : null;
            }
        }
    }

    // Đơn đang dở: đã tạo nhưng chưa xuất kho và chưa đóng/huỷ/từ chối (vẫn xử lý tiếp khi đại lý bị khoá)
    public int countOpenOrders(long customerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales_orders WHERE customer_id = ?"
                + " AND status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'PICKING')";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
