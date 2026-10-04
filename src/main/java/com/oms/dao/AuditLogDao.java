package com.oms.dao;

import com.oms.model.AuditCatalog;
import com.oms.model.AuditLogEntry;
import com.oms.model.AuditLogFilter;
import com.oms.model.AuditLogRow;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // Mã dễ nhận ra của đối tượng; đối tượng đã xoá thì lấy từ giá trị đã ghi (old_values lúc xoá, new_values lúc tạo)
    private static final String ENTITY_REF_SQL =
            "CASE a.entity_type WHEN 'USER' THEN COALESCE(tu.username, " + jsonField("username") + ")"
            + " WHEN 'PRODUCT' THEN COALESCE(p.sku, " + jsonField("sku") + ")"
            + " WHEN 'PRODUCT_CATEGORY' THEN COALESCE(c.code, " + jsonField("code") + ")"
            + " WHEN 'PRICE' THEN COALESCE(pl.code, " + jsonField("code") + ")"
            + " WHEN 'UNIT' THEN COALESCE(un.code, " + jsonField("code") + ")"
            + " WHEN 'SUPPLIER' THEN COALESCE(sp.code, " + jsonField("code") + ")"
            + " END";

    private static String jsonField(String field) {
        return "JSON_UNQUOTE(JSON_EXTRACT(a.old_values, '$." + field + "')),"
                + " JSON_UNQUOTE(JSON_EXTRACT(a.new_values, '$." + field + "'))";
    }

    public long count(AuditLogFilter filter) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM audit_logs a" + where(filter, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // Mới nhất trước
    public List<AuditLogRow> findPage(AuditLogFilter filter, int offset, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT a.id, a.occurred_at, u.full_name, u.username, a.action, a.entity_type, a.entity_id, "
                + ENTITY_REF_SQL + " AS entity_ref, a.ip_address, a.old_values, a.new_values, a.reason"
                + " FROM audit_logs a LEFT JOIN users u ON u.id = a.actor_user_id"
                + " LEFT JOIN users tu ON a.entity_type = 'USER' AND tu.id = a.entity_id"
                + " LEFT JOIN products p ON a.entity_type = 'PRODUCT' AND p.id = a.entity_id"
                + " LEFT JOIN product_categories c ON a.entity_type = 'PRODUCT_CATEGORY' AND c.id = a.entity_id"
                + " LEFT JOIN price_lists pl ON a.entity_type = 'PRICE' AND pl.id = a.entity_id"
                + " LEFT JOIN units un ON a.entity_type = 'UNIT' AND un.id = a.entity_id"
                + " LEFT JOIN suppliers sp ON a.entity_type = 'SUPPLIER' AND sp.id = a.entity_id"
                + where(filter, params) + " ORDER BY a.occurred_at DESC, a.id DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        List<AuditLogRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new AuditLogRow(resultSet.getLong("id"),
                            resultSet.getObject("occurred_at", LocalDateTime.class), resultSet.getString("full_name"),
                            resultSet.getString("username"), resultSet.getString("action"),
                            resultSet.getString("entity_type"), resultSet.getObject("entity_id", Long.class),
                            resultSet.getString("entity_ref"), resultSet.getString("ip_address"),
                            resultSet.getString("old_values"), resultSet.getString("new_values"),
                            resultSet.getString("reason")));
                }
            }
        }
        return rows;
    }

    // Người đã từng thực hiện thao tác (cho ô lọc "Người dùng")
    public List<SelectOption> findActors() throws SQLException {
        String sql = "SELECT u.id, u.username, u.full_name FROM users u"
                + " WHERE EXISTS (SELECT 1 FROM audit_logs a WHERE a.actor_user_id = u.id) ORDER BY u.full_name, u.id";
        List<SelectOption> actors = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                actors.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("username"),
                        resultSet.getString("full_name")));
            }
        }
        return actors;
    }

    private static String where(AuditLogFilter filter, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (filter.getFromUtc() != null) {
            conditions.add("a.occurred_at >= ?");
            params.add(filter.getFromUtc());
        }
        if (filter.getToUtcExclusive() != null) {
            conditions.add("a.occurred_at < ?");
            params.add(filter.getToUtcExclusive());
        }
        if (filter.getActorUserId() != null) {
            conditions.add("a.actor_user_id = ?");
            params.add(filter.getActorUserId());
        }
        if (filter.getEntityType() != null) {
            conditions.add("a.entity_type = ?");
            params.add(filter.getEntityType());
        }
        if (filter.getActionGroup() != null) {
            boolean other = AuditCatalog.GROUP_OTHER.equals(filter.getActionGroup());
            List<String> actions = other ? AuditCatalog.knownActions()
                    : AuditCatalog.actionsInGroup(filter.getActionGroup());
            if (actions.isEmpty()) {
                conditions.add(other ? "1 = 1" : "1 = 0");
            } else {
                conditions.add("a.action " + (other ? "NOT IN" : "IN") + " ("
                        + String.join(",", Collections.nCopies(actions.size(), "?")) + ")");
                params.addAll(actions);
            }
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private static void bind(PreparedStatement statement, List<?> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
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
