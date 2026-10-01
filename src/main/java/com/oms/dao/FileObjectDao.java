package com.oms.dao;

import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Thông tin tệp đã lưu (file_objects); nội dung tệp nằm trên đĩa theo storage_key (xem FileStorage)
public class FileObjectDao {

    public long insert(Connection connection, String storageKey, String originalName, String contentType,
                       long sizeBytes, String purpose, long actorUserId) throws SQLException {
        String sql = "INSERT INTO file_objects (storage_key, original_name, content_type, size_bytes, purpose,"
                + " created_at, created_by) VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, storageKey);
            statement.setString(2, originalName);
            statement.setString(3, contentType);
            statement.setLong(4, sizeBytes);
            statement.setString(5, purpose);
            statement.setLong(6, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // storage_key ảnh đại diện hiện tại của người dùng; null nếu chưa có ảnh
    public String findAvatarKey(long userId) throws SQLException {
        String sql = "SELECT f.storage_key FROM users u JOIN file_objects f ON f.id = u.avatar_file_id WHERE u.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }
}
