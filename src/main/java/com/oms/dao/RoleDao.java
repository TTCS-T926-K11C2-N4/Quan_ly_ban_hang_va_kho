package com.oms.dao;

import com.oms.model.Role;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoleDao {

    private static final String FIND_ALL_SQL = "SELECT code, name FROM roles ORDER BY id";

    public List<Role> findAll() throws SQLException {
        List<Role> roles = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                roles.add(new Role(resultSet.getString("code"), resultSet.getString("name")));
            }
        }
        return roles;
    }
}
