package com.oms.dao;

import com.oms.model.AuditEntityType;
import com.oms.model.AuditLogEntry;
import com.oms.model.AuditLogFilter;
import com.oms.model.AuditLogRecord;
import com.oms.model.SelectOption;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// audit_logs chỉ INSERT (xem COMMENT của bảng); ghi chung transaction với thao tác được ghi lại
public class AuditLogDao {

    private static final String USER_ENTITY = "USER";

    // actorUserId: người thực hiện; null khi chưa đăng nhập (vd tự đăng ký tài khoản)
    public void insertUserAction(Connection connection, Long actorUserId, long userId, String action,
                                 String newValuesJson, String reason, String ipAddress) throws SQLException {
        String sql = "INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, new_values, reason,"
                + " ip_address, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (actorUserId == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, actorUserId);
            }
            statement.setString(2, action);
            statement.setString(3, USER_ENTITY);
            statement.setLong(4, userId);
            if (newValuesJson == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, newValuesJson);
            }
            statement.setString(6, reason);
            statement.setString(7, ipAddress);
            statement.executeUpdate();
        }
    }

    // Ghi nhật ký cho mọi loại đối tượng (S2-04). Gọi trong cùng transaction với thao tác được ghi lại để
    // không có thay đổi nào thiếu nhật ký. oldValuesJson/newValuesJson tạo bằng JsonUtil.toJson.
    public void insert(Connection connection, long actorUserId, String action, AuditEntityType entityType,
                       Long entityId, String oldValuesJson, String newValuesJson, String reason, String ipAddress)
            throws SQLException {
        String sql = "INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_values, new_values,"
                + " reason, ip_address, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actorUserId);
            statement.setString(2, action);
            statement.setString(3, entityType.getCode());
            if (entityId == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, entityId);
            }
            statement.setString(5, oldValuesJson);
            statement.setString(6, newValuesJson);
            statement.setString(7, reason);
            statement.setString(8, ipAddress);
            statement.executeUpdate();
        }
    }

    public long count(AuditLogFilter filter) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM audit_logs a" + buildWhere(filter, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // Mới nhất lên đầu
    public List<AuditLogRecord> findPage(AuditLogFilter filter, int offset, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT a.id, a.occurred_at, u.full_name, u.username, a.action, a.entity_type, a.entity_id,"
                + " a.old_values, a.new_values, a.reason, a.ip_address"
                + " FROM audit_logs a LEFT JOIN users u ON u.id = a.actor_user_id" + buildWhere(filter, params)
                + " ORDER BY a.occurred_at DESC, a.id DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        List<AuditLogRecord> records = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long entityId = resultSet.getLong("entity_id");
                    records.add(new AuditLogRecord(resultSet.getLong("id"),
                            resultSet.getObject("occurred_at", LocalDateTime.class),
                            resultSet.getString("full_name"),
                            resultSet.getString("username"),
                            resultSet.getString("action"),
                            resultSet.getString("entity_type"),
                            resultSet.wasNull() ? null : entityId,
                            resultSet.getString("old_values"),
                            resultSet.getString("new_values"),
                            resultSet.getString("reason"),
                            resultSet.getString("ip_address")));
                }
            }
        }
        return records;
    }

    // Ô lọc "Người thực hiện": chỉ những người đã từng có thao tác được ghi lại
    public List<SelectOption> findActors() throws SQLException {
        String sql = "SELECT u.id, u.full_name, u.username FROM users u"
                + " WHERE EXISTS (SELECT 1 FROM audit_logs a WHERE a.actor_user_id = u.id) ORDER BY u.full_name";
        List<SelectOption> actors = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                actors.add(new SelectOption(resultSet.getLong("id"),
                        resultSet.getString("full_name") + " (" + resultSet.getString("username") + ")"));
            }
        }
        return actors;
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

    private static String buildWhere(AuditLogFilter filter, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (filter.getActorUserId() != null) {
            conditions.add("a.actor_user_id = ?");
            params.add(filter.getActorUserId());
        }
        if (filter.getEntityType() != null) {
            conditions.add("a.entity_type = ?");
            params.add(filter.getEntityType().getCode());
        }
        // occurred_at lưu UTC; so theo khoảng [đầu ngày bắt đầu, đầu ngày sau ngày kết thúc) giờ Việt Nam
        if (filter.getFromDate() != null) {
            conditions.add("a.occurred_at >= ?");
            params.add(DateTimeUtil.startOfDayUtc(filter.getFromDate()));
        }
        if (filter.getToDate() != null) {
            conditions.add("a.occurred_at < ?");
            params.add(DateTimeUtil.startOfDayUtc(filter.getToDate().plusDays(1)));
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private static void bind(PreparedStatement statement, List<?> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }
}
