package com.oms.dao;

import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Token đặt lại mật khẩu (user_tokens.purpose = PASSWORD_RESET). Chỉ lưu SHA-256 của token, không lưu token gốc.
public class UserTokenDao {

    private static final String PASSWORD_RESET = "PASSWORD_RESET";

    // Còn hiệu lực: chưa dùng, chưa hết hạn và tài khoản vẫn đang hoạt động
    private static final String VALID_RESET_CONDITION = "t.token_hash = ? AND t.purpose = '" + PASSWORD_RESET + "'"
            + " AND t.used_at IS NULL AND t.expires_at > UTC_TIMESTAMP() AND u.status = 'ACTIVE'";

    public void insertPasswordReset(Connection connection, long userId, String tokenHash, int validMinutes)
            throws SQLException {
        String sql = "INSERT INTO user_tokens (user_id, purpose, token_hash, expires_at, created_at)"
                + " VALUES (?, ?, ?, UTC_TIMESTAMP() + INTERVAL ? MINUTE, UTC_TIMESTAMP())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, PASSWORD_RESET);
            statement.setString(3, tokenHash);
            statement.setInt(4, validMinutes);
            statement.executeUpdate();
        }
    }

    // Gửi liên kết mới thì các liên kết cũ chưa dùng hết hiệu lực luôn
    public void expireUnusedPasswordResets(Connection connection, long userId) throws SQLException {
        String sql = "UPDATE user_tokens SET used_at = UTC_TIMESTAMP()"
                + " WHERE user_id = ? AND purpose = ? AND used_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, PASSWORD_RESET);
            statement.executeUpdate();
        }
    }

    public boolean isValidPasswordReset(String tokenHash) throws SQLException {
        String sql = "SELECT 1 FROM user_tokens t JOIN users u ON u.id = t.user_id WHERE " + VALID_RESET_CONDITION;
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Đánh dấu đã dùng và trả về user_id; null nếu token không còn hiệu lực.
    // FOR UPDATE để hai lần gửi form cùng lúc không dùng được cùng một liên kết (S1-03: chỉ dùng một lần).
    public Long consumePasswordReset(Connection connection, String tokenHash) throws SQLException {
        String selectSql = "SELECT t.id, t.user_id FROM user_tokens t JOIN users u ON u.id = t.user_id WHERE "
                + VALID_RESET_CONDITION + " FOR UPDATE";
        long tokenId;
        long userId;
        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setString(1, tokenHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                tokenId = resultSet.getLong("id");
                userId = resultSet.getLong("user_id");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE user_tokens SET used_at = UTC_TIMESTAMP() WHERE id = ?")) {
            statement.setLong(1, tokenId);
            statement.executeUpdate();
        }
        return userId;
    }
}
