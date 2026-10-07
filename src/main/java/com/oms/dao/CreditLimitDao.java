package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Hạn mức công nợ của đại lý (customers.credit_limit, max_debt_days) và credit_limit_history (S3-05)
public class CreditLimitDao {

    // Khoá lạc quan theo version; false nếu đại lý vừa được người khác sửa (hoặc đã bị xoá)
    public boolean updateCreditLimit(Connection connection, long id, long version, BigDecimal creditLimit,
                                     int maxDebtDays, long actorUserId) throws SQLException {
        String sql = "UPDATE customers SET credit_limit = ?, max_debt_days = ?, version = version + 1,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ? AND version = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, creditLimit);
            statement.setInt(2, maxDebtDays);
            statement.setLong(3, actorUserId);
            statement.setLong(4, id);
            statement.setLong(5, version);
            return statement.executeUpdate() == 1;
        }
    }

    public void insertCreditLimitHistory(Connection connection, long customerId, BigDecimal oldLimit, int oldDays,
                                         BigDecimal newLimit, int newDays, String reason, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO credit_limit_history (customer_id, old_limit, new_limit, old_debt_days,"
                + " new_debt_days, reason, created_at, created_by) VALUES (?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setBigDecimal(2, oldLimit);
            statement.setBigDecimal(3, newLimit);
            statement.setInt(4, oldDays);
            statement.setInt(5, newDays);
            statement.setString(6, reason);
            statement.setLong(7, actorUserId);
            statement.executeUpdate();
        }
    }
}
