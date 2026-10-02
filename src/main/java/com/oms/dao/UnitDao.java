package com.oms.dao;

import com.oms.model.SelectOption;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Danh mục đơn vị tính (units). Thêm/sửa đơn vị làm ở S2-07; S2-05 chỉ cần chọn đơn vị cơ sở.
public class UnitDao {

    public List<SelectOption> findAll() throws SQLException {
        List<SelectOption> units = new ArrayList<>();
        String sql = "SELECT id, code, name FROM units ORDER BY name";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                units.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("code"),
                        resultSet.getString("name")));
            }
        }
        return units;
    }

    public boolean exists(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM units WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
