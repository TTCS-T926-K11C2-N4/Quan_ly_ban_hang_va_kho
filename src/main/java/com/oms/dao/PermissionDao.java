package com.oms.dao;

import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class PermissionDao {

    // Gộp quyền của mọi vai trò mà người dùng đang giữ
    public Set<String> findCodesByUserId(long userId) throws SQLException {
        String sql = "SELECT DISTINCT p.code FROM user_roles ur"
                + " JOIN role_permissions rp ON rp.role_id = ur.role_id"
                + " JOIN permissions p ON p.id = rp.permission_id WHERE ur.user_id = ?";
        Set<String> codes = new HashSet<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    codes.add(resultSet.getString(1));
                }
            }
        }
        return codes;
    }

    // Phạm vi dữ liệu rộng nhất của quyền qua mọi vai trò: ALL > ASSIGNED > OWN; null nếu không có quyền
    public String findScope(long userId, String permissionCode) throws SQLException {
        String sql = "SELECT rp.data_scope FROM user_roles ur"
                + " JOIN role_permissions rp ON rp.role_id = ur.role_id"
                + " JOIN permissions p ON p.id = rp.permission_id WHERE ur.user_id = ? AND p.code = ?"
                + " ORDER BY FIELD(rp.data_scope, 'ALL', 'ASSIGNED', 'OWN') LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }
}
