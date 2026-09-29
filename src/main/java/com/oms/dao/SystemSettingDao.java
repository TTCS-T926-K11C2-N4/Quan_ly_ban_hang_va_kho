package com.oms.dao;

import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SystemSettingDao {

    // Trả về defaultValue nếu chưa khai báo hoặc giá trị không phải số, để thiếu cấu hình không làm hỏng đăng nhập
    public int getInt(String key, int defaultValue) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT `value` FROM system_settings WHERE `key` = ?")) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return defaultValue;
                }
                try {
                    return Integer.parseInt(resultSet.getString(1).trim());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
    }
}
