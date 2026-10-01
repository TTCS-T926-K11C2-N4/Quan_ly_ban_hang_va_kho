package com.oms.dao;

import com.oms.model.SelectOption;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RegionDao {

    private static final String FIND_ACTIVE_SQL =
            "SELECT id, code, name FROM regions WHERE is_active = true ORDER BY name";

    public List<SelectOption> findActive() throws SQLException {
        List<SelectOption> regions = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                regions.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("code"),
                        resultSet.getString("name")));
            }
        }
        return regions;
    }
}
