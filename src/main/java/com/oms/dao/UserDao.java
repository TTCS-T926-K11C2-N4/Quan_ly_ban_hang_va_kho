package com.oms.dao;

import com.oms.model.LoginAccount;
import com.oms.model.SessionUser;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UserDao {

    // Đăng nhập bằng tên đăng nhập hoặc email (tên đăng nhập không chứa @ nên không trùng với email của người khác)
    public LoginAccount findLoginAccount(String identifier) throws SQLException {
        String sql = "SELECT id, password_hash, status, failed_login_count,"
                + " GREATEST(COALESCE(TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(), locked_until), 0), 0) AS lock_seconds"
                + " FROM users WHERE username = ? OR LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? new LoginAccount(resultSet.getLong("id"), resultSet.getString("password_hash"),
                                resultSet.getString("status"), resultSet.getInt("failed_login_count"),
                                resultSet.getLong("lock_seconds"))
                        : null;
            }
        }
    }

    // Tăng số lần sai; đủ maxFailed lần thì khóa tạm lockMinutes phút và đếm lại từ đầu.
    // MySQL gán SET từ trái sang phải nên phải tính locked_until trước khi đổi failed_login_count.
    public void recordFailedLogin(long userId, int maxFailed, int lockMinutes) throws SQLException {
        String sql = "UPDATE users SET"
                + " locked_until = IF(failed_login_count + 1 >= ?, UTC_TIMESTAMP() + INTERVAL ? MINUTE, locked_until),"
                + " failed_login_count = IF(failed_login_count + 1 >= ?, 0, failed_login_count + 1)"
                + " WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, maxFailed);
            statement.setInt(2, lockMinutes);
            statement.setInt(3, maxFailed);
            statement.setLong(4, userId);
            statement.executeUpdate();
        }
    }

    public void recordSuccessfulLogin(long userId) throws SQLException {
        String sql = "UPDATE users SET failed_login_count = 0, locked_until = NULL, last_login_at = UTC_TIMESTAMP()"
                + " WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    public Long findActiveIdByEmail(String email) throws SQLException {
        String sql = "SELECT id FROM users WHERE LOWER(email) = LOWER(?) AND status = 'ACTIVE'";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    public String findPasswordHash(long userId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT password_hash FROM users WHERE id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    // Đổi hoặc đặt lại mật khẩu: bỏ luôn yêu cầu đổi mật khẩu tạm và khóa tạm do nhập sai
    public void updatePassword(Connection connection, long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, password_changed_at = UTC_TIMESTAMP(),"
                + " must_change_password = false, failed_login_count = 0, locked_until = NULL,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.executeUpdate();
        }
    }

    // Thông tin hiển thị ở sidebar: tên, các vai trò, kho và địa bàn đang phụ trách
    public SessionUser findSessionUser(long userId, Set<String> permissions) throws SQLException {
        String sql = "SELECT username, full_name, must_change_password FROM users WHERE id = ?";
        try (Connection connection = DbConnection.getConnection()) {
            String username;
            String fullName;
            boolean mustChangePassword;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    username = resultSet.getString("username");
                    fullName = resultSet.getString("full_name");
                    mustChangePassword = resultSet.getBoolean("must_change_password");
                }
            }
            List<String> roleNames = findNames(connection, "SELECT r.name FROM user_roles ur"
                    + " JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? ORDER BY r.id", userId);
            List<String> roleCodes = findNames(connection, "SELECT r.code FROM user_roles ur"
                    + " JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ?", userId);
            List<String> scopes = new ArrayList<>(findNames(connection, "SELECT w.name FROM user_warehouses uw"
                    + " JOIN warehouses w ON w.id = uw.warehouse_id WHERE uw.user_id = ? ORDER BY w.name", userId));
            scopes.addAll(findNames(connection, "SELECT g.name FROM user_regions ug"
                    + " JOIN regions g ON g.id = ug.region_id WHERE ug.user_id = ? ORDER BY g.name", userId));
            return new SessionUser(userId, username, fullName, String.join(", ", roleNames),
                    String.join(" • ", scopes), mustChangePassword, Set.copyOf(roleCodes), permissions);
        }
    }

    private List<String> findNames(Connection connection, String sql, long userId) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
            }
        }
        return names;
    }
}
