package com.oms.dao;

import com.oms.model.StoredFile;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class FileObjectDao {

    public static final String PURPOSE_AVATAR = "AVATAR";

    public long insert(Connection connection, String storageKey, String originalName, String contentType,
                       long sizeBytes, String purpose, long createdBy) throws SQLException {
        String sql = "INSERT INTO file_objects (storage_key, original_name, content_type, size_bytes, purpose,"
                + " created_at, created_by) VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, storageKey);
            statement.setString(2, originalName);
            statement.setString(3, contentType);
            statement.setLong(4, sizeBytes);
            statement.setString(5, purpose);
            statement.setLong(6, createdBy);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // Chỉ trả tệp đúng mục đích, để đường dẫn xem ảnh đại diện không mở được các loại tệp khác
    public StoredFile findByIdAndPurpose(long id, String purpose) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            return find(connection, id, purpose);
        }
    }

    public StoredFile find(Connection connection, long id, String purpose) throws SQLException {
        String sql = "SELECT id, storage_key, content_type FROM file_objects WHERE id = ? AND purpose = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, purpose);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? new StoredFile(resultSet.getLong("id"), resultSet.getString("storage_key"),
                                resultSet.getString("content_type"))
                        : null;
            }
        }
    }

    public void delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM file_objects WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
