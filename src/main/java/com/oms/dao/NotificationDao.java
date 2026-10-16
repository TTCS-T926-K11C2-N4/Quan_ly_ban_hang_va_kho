package com.oms.dao;

import com.oms.model.Notification;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Thông báo trong ứng dụng (bảng notifications)
public class NotificationDao {

    // Trong transaction của người gọi; emailed: đã đưa thư vào hàng đợi gửi
    public void insert(Connection connection, long userId, String type, String title, String body, String refType,
                       Long refId, boolean emailed) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO notifications (user_id, type, title, body, ref_type, ref_id, emailed_at, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, " + (emailed ? "UTC_TIMESTAMP()" : "NULL")
                        + ", UTC_TIMESTAMP())")) {
            statement.setLong(1, userId);
            statement.setString(2, type);
            statement.setString(3, title);
            statement.setString(4, body);
            statement.setString(5, refType);
            if (refId == null) {
                statement.setNull(6, Types.BIGINT);
            } else {
                statement.setLong(6, refId);
            }
            statement.executeUpdate();
        }
    }

    public int countUnread(long userId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_at IS NULL")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    // Mới nhất trước
    public List<Notification> findByUser(long userId, int limit) throws SQLException {
        List<Notification> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, type, title, body, ref_type, ref_id, read_at, created_at FROM notifications"
                             + " WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ?")) {
            statement.setLong(1, userId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new Notification(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(3), resultSet.getString(4), resultSet.getString(5),
                            resultSet.getObject(6, Long.class), resultSet.getObject(7, LocalDateTime.class),
                            resultSet.getObject(8, LocalDateTime.class)));
                }
            }
        }
        return result;
    }

    public void markAllRead(long userId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE notifications SET read_at = UTC_TIMESTAMP() WHERE user_id = ? AND read_at IS NULL")) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }
}
