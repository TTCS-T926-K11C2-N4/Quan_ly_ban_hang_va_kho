package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.model.SalesTarget;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Chỉ tiêu doanh số theo tháng cho từng nhân viên và khu vực (S8-04).
//  * Doanh số thực tế = tổng dòng hoá đơn chưa huỷ trong tháng, tính tức thời mỗi lần mở trang.
//  * Chỉ tiêu của tháng đã qua bị khoá: sửa phải có lý do; mọi thay đổi chỉ tiêu đều ghi nhật ký.
public class SalesTargetService {

    public static final int REASON_MAX = 500;
    private static final String AUDIT_ENTITY = "SALES_TARGET";
    private static final Pattern MONEY = Pattern.compile("[0-9]{1,13}");

    private final AuditLogDao auditLogDao = new AuditLogDao();

    // error != null: lỗi nghiệp vụ; ngược lại số chỉ tiêu đã đổi
    public static final class Outcome {
        private final String error;
        private final int changed;

        private Outcome(String error, int changed) {
            this.error = error;
            this.changed = changed;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0);
        }

        static Outcome success(int changed) {
            return new Outcome(null, changed);
        }

        public String getError() {
            return error;
        }

        public int getChanged() {
            return changed;
        }
    }

    // Ngày đầu tháng từ chuỗi yyyy-MM; sai hoặc trống thì tháng hiện tại
    public static LocalDate parseMonth(String text, LocalDate today) {
        try {
            return text == null || text.isBlank() ? today.withDayOfMonth(1) : YearMonth.parse(text.trim()).atDay(1);
        } catch (DateTimeParseException e) {
            return today.withDayOfMonth(1);
        }
    }

    // Tháng đã qua kỳ thì chỉ tiêu bị khoá
    public static boolean isLocked(LocalDate month, LocalDate today) {
        return month.isBefore(today.withDayOfMonth(1));
    }

    // Chỉ tiêu và tiến độ của mọi nhân viên kinh doanh đang hoạt động; onlyUserId: chỉ một nhân viên
    public List<SalesTarget> repTargets(LocalDate month, Long onlyUserId) throws SQLException {
        LocalDate today = DateTimeUtil.today();
        boolean locked = isLocked(month, today);
        int expected = SalesTarget.expectedPercent(month, today);
        Map<Long, BigDecimal> targets = loadTargets(month, "user_id");
        Map<Long, BigDecimal> actuals = loadActuals(month, "c.sales_rep_id");
        List<SalesTarget> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT DISTINCT u.id, u.full_name FROM users u JOIN user_roles ur ON ur.user_id = u.id"
                             + " JOIN roles r ON r.id = ur.role_id WHERE r.code = 'SALES_REP' AND u.status = 'ACTIVE'"
                             + " AND (? IS NULL OR u.id = ?) ORDER BY u.full_name")) {
            if (onlyUserId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, onlyUserId);
                statement.setLong(2, onlyUserId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long id = resultSet.getLong(1);
                    result.add(new SalesTarget("u:" + id, resultSet.getString(2),
                            targets.getOrDefault(id, BigDecimal.ZERO), actuals.getOrDefault(id, BigDecimal.ZERO),
                            locked, expected));
                }
            }
        }
        return result;
    }

    public List<SalesTarget> regionTargets(LocalDate month) throws SQLException {
        LocalDate today = DateTimeUtil.today();
        boolean locked = isLocked(month, today);
        int expected = SalesTarget.expectedPercent(month, today);
        Map<Long, BigDecimal> targets = loadTargets(month, "region_id");
        Map<Long, BigDecimal> actuals = loadActuals(month, "c.region_id");
        List<SalesTarget> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name FROM regions WHERE is_active = 1 ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                result.add(new SalesTarget("r:" + id, resultSet.getString(2),
                        targets.getOrDefault(id, BigDecimal.ZERO), actuals.getOrDefault(id, BigDecimal.ZERO), locked,
                        expected));
            }
        }
        return result;
    }

    private static Map<Long, BigDecimal> loadTargets(LocalDate month, String column) throws SQLException {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + column + ", target_amount FROM sales_targets WHERE period_month = ? AND " + column
                             + " IS NOT NULL")) {
            statement.setDate(1, Date.valueOf(month));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.put(resultSet.getLong(1), resultSet.getBigDecimal(2));
                }
            }
        }
        return result;
    }

    // keyExpression là hằng trong mã (c.sales_rep_id hoặc c.region_id), không lấy từ người dùng
    private static Map<Long, BigDecimal> loadActuals(LocalDate month, String keyExpression) throws SQLException {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + keyExpression + ", SUM(i.line_total) FROM invoice_items i"
                             + " JOIN invoices v ON v.id = i.invoice_id AND v.status <> 'CANCELLED'"
                             + " JOIN customers c ON c.id = v.customer_id WHERE v.issue_date BETWEEN ? AND ?"
                             + " AND " + keyExpression + " IS NOT NULL GROUP BY " + keyExpression)) {
            statement.setDate(1, Date.valueOf(month));
            statement.setDate(2, Date.valueOf(month.withDayOfMonth(month.lengthOfMonth())));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.put(resultSet.getLong(1), resultSet.getBigDecimal(2));
                }
            }
        }
        return result;
    }

    // entries: khoá "u:<id>" / "r:<id>" → số tiền nhập (chuỗi rỗng là bỏ qua). Chỉ ghi các chỉ tiêu có thay đổi.
    public Outcome save(long actorUserId, LocalDate month, Map<String, String> entries, String reason,
                        String ipAddress) throws SQLException {
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.length() > REASON_MAX) {
            return Outcome.failure("Lý do tối đa " + REASON_MAX + " ký tự.");
        }
        Map<Long, BigDecimal> userTargets = loadTargets(month, "user_id");
        Map<Long, BigDecimal> regionTargets = loadTargets(month, "region_id");
        Map<String, BigDecimal[]> changes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String text = entry.getValue() == null ? "" : entry.getValue().trim().replace(".", "").replace(",", "");
            if (text.isEmpty()) {
                continue;
            }
            if (!MONEY.matcher(text).matches()) {
                return Outcome.failure("Chỉ tiêu phải là số nguyên đồng không âm.");
            }
            BigDecimal value = new BigDecimal(text);
            boolean forUser = entry.getKey().startsWith("u:");
            long id;
            try {
                id = Long.parseLong(entry.getKey().substring(2));
            } catch (NumberFormatException e) {
                return Outcome.failure("Khoá chỉ tiêu không hợp lệ.");
            }
            BigDecimal old = (forUser ? userTargets : regionTargets).getOrDefault(id, BigDecimal.ZERO);
            if (old.compareTo(value) != 0) {
                changes.put(entry.getKey(), new BigDecimal[]{old, value});
            }
        }
        if (changes.isEmpty()) {
            return Outcome.success(0);
        }
        if (isLocked(month, DateTimeUtil.today()) && cleanReason.isEmpty()) {
            return Outcome.failure("Chỉ tiêu của tháng đã qua đã khoá; nhập lý do để sửa.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Map.Entry<String, BigDecimal[]> change : changes.entrySet()) {
                    boolean forUser = change.getKey().startsWith("u:");
                    long id = Long.parseLong(change.getKey().substring(2));
                    upsert(connection, month, forUser, id, change.getValue()[1], actorUserId);
                    Map<String, Object> before = new LinkedHashMap<>();
                    before.put("target", change.getValue()[0].toPlainString());
                    Map<String, Object> after = new LinkedHashMap<>();
                    after.put("month", month.toString());
                    after.put(forUser ? "userId" : "regionId", id);
                    after.put("target", change.getValue()[1].toPlainString());
                    auditLogDao.insert(connection, actorUserId, "TARGET_SET", AUDIT_ENTITY, id,
                            JsonUtil.object(before), JsonUtil.object(after), cleanReason.isEmpty() ? null : cleanReason,
                            ipAddress);
                }
                connection.commit();
                return Outcome.success(changes.size());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void upsert(Connection connection, LocalDate month, boolean forUser, long id, BigDecimal value,
                               long actorUserId) throws SQLException {
        String column = forUser ? "user_id" : "region_id";
        boolean locked = isLocked(month, DateTimeUtil.today());
        int updated;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE sales_targets SET target_amount = ?, is_locked = ?, updated_at = UTC_TIMESTAMP(),"
                        + " updated_by = ? WHERE period_month = ? AND " + column + " = ?")) {
            statement.setBigDecimal(1, value);
            statement.setBoolean(2, locked);
            statement.setLong(3, actorUserId);
            statement.setDate(4, Date.valueOf(month));
            statement.setLong(5, id);
            updated = statement.executeUpdate();
        }
        if (updated == 0) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO sales_targets (period_month, " + column + ", target_amount, is_locked, created_at,"
                            + " created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, UTC_TIMESTAMP(), ?,"
                            + " UTC_TIMESTAMP(), ?)")) {
                statement.setDate(1, Date.valueOf(month));
                statement.setLong(2, id);
                statement.setBigDecimal(3, value);
                statement.setBoolean(4, locked);
                statement.setLong(5, actorUserId);
                statement.setLong(6, actorUserId);
                statement.executeUpdate();
            }
        }
    }
}
