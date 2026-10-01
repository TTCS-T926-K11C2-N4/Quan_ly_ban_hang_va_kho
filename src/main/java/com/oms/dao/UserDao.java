package com.oms.dao;

import com.oms.model.AccountDetail;
import com.oms.model.AccountFilter;
import com.oms.model.AccountForm;
import com.oms.model.AccountListItem;
import com.oms.model.AuditLogEntry;
import com.oms.model.EditableAccount;
import com.oms.model.LoginAccount;
import com.oms.model.SelectOption;
import com.oms.model.AccountStatus;
import com.oms.model.Role;
import com.oms.model.SessionUser;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserDao {

    // locked_until lưu theo UTC (xem ghi chú đầu OMS_schema_mysql.sql) nên so với UTC_TIMESTAMP()
    private static final String DISPLAY_STATUS_SQL =
            "CASE WHEN u.status = 'LOCKED' THEN 'LOCKED'"
            + " WHEN u.status = 'PENDING' THEN 'PENDING'"
            + " WHEN u.locked_until > UTC_TIMESTAMP() THEN 'TEMP_LOCKED'"
            + " ELSE 'ACTIVE' END";

    public long count(AccountFilter filter) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM users u" + buildWhere(filter, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public List<AccountListItem> findPage(AccountFilter filter, int offset, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT u.id, u.full_name, u.username, u.phone, " + DISPLAY_STATUS_SQL + " AS display_status"
                + " FROM users u" + buildWhere(filter, params)
                + " ORDER BY u.created_at DESC, u.id DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);

        Map<Long, AccountListItem> accounts = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        AccountListItem account = new AccountListItem(
                                resultSet.getLong("id"),
                                resultSet.getString("full_name"),
                                resultSet.getString("username"),
                                resultSet.getString("phone"),
                                AccountStatus.fromCode(resultSet.getString("display_status")));
                        accounts.put(account.getId(), account);
                    }
                }
            }
            loadRoles(connection, accounts);
        }
        return new ArrayList<>(accounts.values());
    }

    // Đăng nhập bằng tên đăng nhập hoặc email (tên đăng nhập không chứa @ nên không trùng với email của người khác)
    public LoginAccount findLoginAccount(String identifier) throws SQLException {
        String sql = "SELECT id, password_hash, status, failed_login_count,"
                + " GREATEST(COALESCE(TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(), locked_until), 0), 0) AS lock_seconds"
                + " FROM users WHERE username = ? OR LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? new LoginAccount(resultSet.getLong("id"), resultSet.getString("password_hash"),
                                resultSet.getString("status"), resultSet.getInt("failed_login_count"),
                                resultSet.getLong("lock_seconds"))
                        : null;
            }
        }
    }

    // Tăng số lần sai; đủ maxFailed lần thì khóa tạm lockMinutes phút và đếm lại từ đầu.
    // MySQL gán SET từ trái sang phải nên phải tính locked_until trước khi đổi failed_login_count.
    public void recordFailedLogin(long userId, int maxFailed, int lockMinutes) throws SQLException {
        String sql = "UPDATE users SET"
                + " locked_until = IF(failed_login_count + 1 >= ?, UTC_TIMESTAMP() + INTERVAL ? MINUTE, locked_until),"
                + " failed_login_count = IF(failed_login_count + 1 >= ?, 0, failed_login_count + 1)"
                + " WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, maxFailed);
            statement.setInt(2, lockMinutes);
            statement.setInt(3, maxFailed);
            statement.setLong(4, userId);
            statement.executeUpdate();
        }
    }

    public void recordSuccessfulLogin(long userId) throws SQLException {
        String sql = "UPDATE users SET failed_login_count = 0, locked_until = NULL, last_login_at = UTC_TIMESTAMP()"
                + " WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    public Long findActiveIdByEmail(String email) throws SQLException {
        String sql = "SELECT id FROM users WHERE LOWER(email) = LOWER(?) AND status = 'ACTIVE'";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    public String findPasswordHash(long userId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT password_hash FROM users WHERE id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    // Đổi hoặc đặt lại mật khẩu: bỏ luôn yêu cầu đổi mật khẩu tạm và khóa tạm do nhập sai
    public void updatePassword(Connection connection, long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, password_changed_at = UTC_TIMESTAMP(),"
                + " must_change_password = false, failed_login_count = 0, locked_until = NULL,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.executeUpdate();
        }
    }

    // Thông tin hiển thị ở sidebar: tên, các vai trò, kho và địa bàn đang phụ trách
    public SessionUser findSessionUser(long userId, Set<String> permissions) throws SQLException {
        String sql = "SELECT username, full_name, must_change_password, avatar_file_id FROM users WHERE id = ?";
        try (Connection connection = DbConnection.getConnection()) {
            String username;
            String fullName;
            boolean mustChangePassword;
            Long avatarFileId;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    username = resultSet.getString("username");
                    fullName = resultSet.getString("full_name");
                    mustChangePassword = resultSet.getBoolean("must_change_password");
                    long fileId = resultSet.getLong("avatar_file_id");
                    avatarFileId = resultSet.wasNull() ? null : fileId;
                }
            }
            List<String> roleNames = findNames(connection, "SELECT r.name FROM user_roles ur"
                    + " JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? ORDER BY r.id", userId);
            List<String> roleCodes = findNames(connection, "SELECT r.code FROM user_roles ur"
                    + " JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ?", userId);
            List<String> scopes = new ArrayList<>(findNames(connection, "SELECT w.name FROM user_warehouses uw"
                    + " JOIN warehouses w ON w.id = uw.warehouse_id WHERE uw.user_id = ? ORDER BY w.name", userId));
            scopes.addAll(findNames(connection, "SELECT g.name FROM user_regions ug"
                    + " JOIN regions g ON g.id = ug.region_id WHERE ug.user_id = ? ORDER BY g.name", userId));
            return new SessionUser(userId, username, fullName, String.join(", ", roleNames),
                    String.join(" • ", scopes), mustChangePassword, Set.copyOf(roleCodes), permissions, avatarFileId);
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        return exists("SELECT 1 FROM users WHERE username = ?", username);
    }

    // Khớp với UNIQUE KEY ux_users_email trên LOWER(email); excludeUserId để bỏ qua chính tài khoản đang sửa
    public boolean existsByEmail(String email, Long excludeUserId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) AND id <> ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setLong(2, excludeUserId == null ? 0 : excludeUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Kho/địa bàn: form chỉ chọn một nên lấy dòng đầu tiên nếu dữ liệu cũ có nhiều dòng
    public EditableAccount findForEdit(long userId) throws SQLException {
        String userSql = "SELECT username, email, phone, full_name, status FROM users WHERE id = ?";
        String rolesSql = "SELECT r.code FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ?";
        try (Connection connection = DbConnection.getConnection()) {
            String username;
            String email;
            String phone;
            String fullName;
            String status;
            try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    username = resultSet.getString("username");
                    email = resultSet.getString("email");
                    phone = resultSet.getString("phone");
                    fullName = resultSet.getString("full_name");
                    status = resultSet.getString("status");
                }
            }

            List<String> roleCodes = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(rolesSql)) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        roleCodes.add(resultSet.getString("code"));
                    }
                }
            }

            Long warehouseId = findFirstLink(connection,
                    "SELECT warehouse_id FROM user_warehouses WHERE user_id = ? ORDER BY warehouse_id LIMIT 1", userId);
            Long regionId = findFirstLink(connection,
                    "SELECT region_id FROM user_regions WHERE user_id = ? ORDER BY region_id LIMIT 1", userId);

            AccountForm form = new AccountForm(fullName, username, email, phone, roleCodes, warehouseId, regionId);
            return new EditableAccount(userId, status, roleCodes, form);
        }
    }

    public AccountDetail findDetail(long userId, AuditLogEntry latestActivity) throws SQLException {
        String userSql = "SELECT u.full_name, u.username, u.email, u.phone, u.created_at, u.last_login_at, "
                + DISPLAY_STATUS_SQL + " AS display_status FROM users u WHERE u.id = ?";
        try (Connection connection = DbConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    List<Role> roles = new ArrayList<>();
                    try (PreparedStatement roleStatement = connection.prepareStatement(
                            "SELECT r.code, r.name FROM user_roles ur JOIN roles r ON r.id = ur.role_id"
                                    + " WHERE ur.user_id = ? ORDER BY r.id")) {
                        roleStatement.setLong(1, userId);
                        try (ResultSet roleRows = roleStatement.executeQuery()) {
                            while (roleRows.next()) {
                                roles.add(new Role(roleRows.getString("code"), roleRows.getString("name")));
                            }
                        }
                    }
                    return new AccountDetail(userId,
                            resultSet.getString("full_name"),
                            resultSet.getString("username"),
                            resultSet.getString("email"),
                            resultSet.getString("phone"),
                            AccountStatus.fromCode(resultSet.getString("display_status")),
                            resultSet.getObject("created_at", LocalDateTime.class),
                            resultSet.getObject("last_login_at", LocalDateTime.class),
                            roles,
                            findNames(connection, "SELECT w.name FROM user_warehouses uw"
                                    + " JOIN warehouses w ON w.id = uw.warehouse_id WHERE uw.user_id = ? ORDER BY w.name", userId),
                            findNames(connection, "SELECT g.name FROM user_regions ug"
                                    + " JOIN regions g ON g.id = ug.region_id WHERE ug.user_id = ? ORDER BY g.name", userId),
                            countCustomers(connection, userId),
                            latestActivity);
                }
            }
        }
    }

    // Người nhận bàn giao địa bàn/đại lý: nhân viên kinh doanh đang hoạt động, trừ chính người bị khóa
    public List<SelectOption> findHandoverCandidates(long excludeUserId) throws SQLException {
        String sql = "SELECT u.id, u.full_name, r.name AS role_name FROM users u"
                + " JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id"
                + " WHERE r.code = 'SALES_REP' AND u.status = 'ACTIVE' AND u.id <> ? ORDER BY u.full_name";
        List<SelectOption> candidates = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, excludeUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    candidates.add(new SelectOption(resultSet.getLong("id"),
                            resultSet.getString("full_name") + " — " + resultSet.getString("role_name")));
                }
            }
        }
        return candidates;
    }

    // Trả về false nếu tài khoản đã bị khóa trước đó (vd hai admin cùng khóa một lúc)
    public boolean lock(Connection connection, long userId, String reason) throws SQLException {
        String sql = "UPDATE users SET status = 'LOCKED', lock_reason = ?, updated_at = UTC_TIMESTAMP()"
                + " WHERE id = ? AND status <> 'LOCKED'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reason);
            statement.setLong(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    // Mở cả khóa hẳn (LOCKED) lẫn khóa tạm do nhập sai mật khẩu (locked_until); trả về false nếu không có gì để mở
    public boolean unlock(Connection connection, long userId) throws SQLException {
        String sql = "UPDATE users SET status = CASE WHEN status = 'LOCKED' THEN 'ACTIVE' ELSE status END,"
                + " lock_reason = NULL, locked_until = NULL, failed_login_count = 0, updated_at = UTC_TIMESTAMP()"
                + " WHERE id = ? AND (status = 'LOCKED' OR locked_until > UTC_TIMESTAMP())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            return statement.executeUpdate() > 0;
        }
    }

    // Người bị khóa không đăng nhập được nữa nên thu hồi luôn các refresh token còn hiệu lực
    public void revokeRefreshTokens(Connection connection, long userId) throws SQLException {
        executeForUser(connection, "UPDATE refresh_tokens SET revoked_at = UTC_TIMESTAMP()"
                + " WHERE user_id = ? AND revoked_at IS NULL", userId);
    }

    public void transferRegions(Connection connection, long fromUserId, long toUserId) throws SQLException {
        // INSERT IGNORE: người nhận có thể đã phụ trách sẵn một số địa bàn này
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO user_regions (user_id, region_id)"
                        + " SELECT ?, region_id FROM user_regions WHERE user_id = ?")) {
            statement.setLong(1, toUserId);
            statement.setLong(2, fromUserId);
            statement.executeUpdate();
        }
        deleteRegions(connection, fromUserId);
    }

    // Ghi customer_assignment_history trước rồi mới đổi sales_rep_id để giữ được người phụ trách cũ
    public void transferCustomers(Connection connection, long fromUserId, long toUserId, String reason)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO customer_assignment_history (customer_id, from_sales_rep_id, to_sales_rep_id, reason, created_at)"
                        + " SELECT id, ?, ?, ?, UTC_TIMESTAMP() FROM customers WHERE sales_rep_id = ?")) {
            statement.setLong(1, fromUserId);
            statement.setLong(2, toUserId);
            statement.setString(3, reason);
            statement.setLong(4, fromUserId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE customers SET sales_rep_id = ?, version = version + 1, updated_at = UTC_TIMESTAMP()"
                        + " WHERE sales_rep_id = ?")) {
            statement.setLong(1, toUserId);
            statement.setLong(2, fromUserId);
            statement.executeUpdate();
        }
    }

    private int countCustomers(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM customers WHERE sales_rep_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private List<String> findNames(Connection connection, String sql, long userId) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
            }
        }
        return names;
    }

    // activate: chuyển PENDING -> ACTIVE (chỉ khi tài khoản đang PENDING)
    public void updateProfile(Connection connection, long userId, String fullName, String email, String phone,
                              boolean activate) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?,"
                + " status = CASE WHEN ? AND status = 'PENDING' THEN 'ACTIVE' ELSE status END,"
                + " updated_at = UTC_TIMESTAMP() WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setBoolean(4, activate);
            statement.setLong(5, userId);
            statement.executeUpdate();
        }
    }

    // Người dùng tự sửa hồ sơ (S2-02): chỉ họ tên và số điện thoại
    public void updateOwnProfile(Connection connection, long userId, String fullName, String phone)
            throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone = ?, updated_at = UTC_TIMESTAMP(), updated_by = ?"
                + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            statement.setString(2, phone);
            statement.setLong(3, userId);
            statement.setLong(4, userId);
            statement.executeUpdate();
        }
    }

    // Khoá dòng người dùng tới hết transaction để hai lần đổi ảnh cùng lúc không làm sót tệp ảnh cũ
    public Long findAvatarFileIdForUpdate(Connection connection, long userId) throws SQLException {
        String sql = "SELECT avatar_file_id FROM users WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                long fileId = resultSet.getLong("avatar_file_id");
                return resultSet.wasNull() ? null : fileId;
            }
        }
    }

    public void updateAvatar(Connection connection, long userId, long fileId) throws SQLException {
        String sql = "UPDATE users SET avatar_file_id = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, fileId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.executeUpdate();
        }
    }

    public void deleteRolesExcept(Connection connection, long userId, String keptRoleCode) throws SQLException {
        String sql = "DELETE FROM user_roles WHERE user_id = ?"
                + " AND role_id NOT IN (SELECT id FROM roles WHERE code = ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, keptRoleCode);
            statement.executeUpdate();
        }
    }

    public void deleteWarehouses(Connection connection, long userId) throws SQLException {
        executeForUser(connection, "DELETE FROM user_warehouses WHERE user_id = ?", userId);
    }

    public void deleteRegions(Connection connection, long userId) throws SQLException {
        executeForUser(connection, "DELETE FROM user_regions WHERE user_id = ?", userId);
    }

    private void executeForUser(Connection connection, String sql, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private Long findFirstLink(Connection connection, String sql, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    // Các hàm insert nhận Connection để AccountService gộp chung một transaction
    public long insert(Connection connection, String username, String email, String phone, String fullName,
                       String passwordHash, String status, boolean mustChangePassword) throws SQLException {
        String sql = "INSERT INTO users (username, email, phone, full_name, password_hash, status,"
                + " must_change_password, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setString(4, fullName);
            statement.setString(5, passwordHash);
            statement.setString(6, status);
            statement.setBoolean(7, mustChangePassword);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertRoles(Connection connection, long userId, List<String> roleCodes) throws SQLException {
        String sql = "INSERT INTO user_roles (user_id, role_id, assigned_at)"
                + " SELECT ?, id, UTC_TIMESTAMP() FROM roles WHERE code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String roleCode : roleCodes) {
                statement.setLong(1, userId);
                statement.setString(2, roleCode);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public void insertWarehouse(Connection connection, long userId, long warehouseId) throws SQLException {
        insertLink(connection, "INSERT INTO user_warehouses (user_id, warehouse_id) VALUES (?, ?)", userId, warehouseId);
    }

    public void insertRegion(Connection connection, long userId, long regionId) throws SQLException {
        insertLink(connection, "INSERT INTO user_regions (user_id, region_id) VALUES (?, ?)", userId, regionId);
    }

    private void insertLink(Connection connection, String sql, long userId, long targetId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, targetId);
            statement.executeUpdate();
        }
    }

    private boolean exists(String sql, String value) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Một người có thể có nhiều vai trò (user_roles) nên nạp riêng cho các tài khoản trên trang
    private void loadRoles(Connection connection, Map<Long, AccountListItem> accounts) throws SQLException {
        if (accounts.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(accounts.size(), "?"));
        String sql = "SELECT ur.user_id, r.code, r.name FROM user_roles ur"
                + " JOIN roles r ON r.id = ur.role_id"
                + " WHERE ur.user_id IN (" + placeholders + ") ORDER BY r.id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, new ArrayList<>(accounts.keySet()));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.get(resultSet.getLong("user_id")).getRoles()
                            .add(new Role(resultSet.getString("code"), resultSet.getString("name")));
                }
            }
        }
    }

    private String buildWhere(AccountFilter filter, List<Object> params) {
        List<String> conditions = new ArrayList<>();

        if (filter.getKeyword() != null) {
            String pattern = "%" + escapeLike(filter.getKeyword()) + "%";
            conditions.add("(u.full_name LIKE ? OR u.username LIKE ? OR u.phone LIKE ?)");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (filter.getRoleCode() != null) {
            conditions.add("EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id"
                    + " WHERE ur.user_id = u.id AND r.code = ?)");
            params.add(filter.getRoleCode());
        }
        if (filter.getStatus() != null) {
            conditions.add(DISPLAY_STATUS_SQL + " = ?");
            params.add(filter.getStatus().name());
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    // Người dùng gõ % hoặc _ thì tìm đúng ký tự đó, không để thành ký tự đại diện của LIKE
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void bind(PreparedStatement statement, List<?> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }
}
