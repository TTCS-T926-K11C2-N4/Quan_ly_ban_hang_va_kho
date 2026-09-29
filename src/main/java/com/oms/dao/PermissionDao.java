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
}
