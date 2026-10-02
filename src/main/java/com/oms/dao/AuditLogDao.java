package com.oms.dao;

import com.oms.model.AuditLogEntry;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;

// audit_logs chỉ INSERT (xem COMMENT của bảng); ghi chung transaction với thao tác được ghi lại
public class AuditLogDao {

    private static final String USER_ENTITY = "USER";

    // actorUserId: người thực hiện; null khi chưa đăng nhập (vd tự đăng ký tài khoản)
    public void insertUserAction(Connection connection, Long actorUserId, long userId, String action,
                                 String oldValuesJson, String newValuesJson, String reason, String ipAddress)
            throws SQLException {
        insert(connection, actorUserId, action, USER_ENTITY, userId, oldValuesJson, newValuesJson, reason, ipAddress);
    }

    // Ghi một thao tác trên đối tượng bất kỳ (nhóm hàng, giá...); old/new là JSON giá trị trước và sau (S2-04)
    public void insert(Connection connection, Long actorUserId, String action, String entityType, Long entityId,
                       String oldValuesJson, String newValuesJson, String reason, String ipAddress) throws SQLException {
        String sql = "INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_values, new_values,"
                + " reason, ip_address, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableLong(statement, 1, actorUserId);
            statement.setString(2, action);
            statement.setString(3, entityType);
            setNullableLong(statement, 4, entityId);
            statement.setString(5, oldValuesJson);
            statement.setString(6, newValuesJson);
            statement.setString(7, reason);
            statement.setString(8, ipAddress);
            statement.executeUpdate();
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    public AuditLogEntry findLatestForUser(long userId) throws SQLException {
        String sql = "SELECT action, reason, occurred_at FROM audit_logs WHERE entity_type = ? AND entity_id = ?"
                + " ORDER BY occurred_at DESC, id DESC LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, USER_ENTITY);
            statement.setLong(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? new AuditLogEntry(resultSet.getString("action"), resultSet.getString("reason"),
                                resultSet.getObject("occurred_at", LocalDateTime.class))
                        : null;
            }
        }
    }

    // Lần khóa gần nhất có bàn giao địa bàn/đại lý không (new_values có handoverToUserId, xem AccountService.lock)
    public boolean lastLockHadHandover(long userId) throws SQLException {
        String sql = "SELECT JSON_EXTRACT(new_values, '$.handoverToUserId') IS NOT NULL FROM audit_logs"
                + " WHERE entity_type = ? AND entity_id = ? AND action = ? ORDER BY occurred_at DESC, id DESC LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, USER_ENTITY);
            statement.setLong(2, userId);
            statement.setString(3, AuditLogEntry.USER_LOCK);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }
}
