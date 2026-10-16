package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerCreditDao;
import com.oms.model.CustomerCredit;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Cờ công nợ tự động của đại lý (S7-04): OVERDUE khi còn hoá đơn quá số ngày nợ cho phép, OVER_LIMIT khi công nợ
// vượt hạn mức, OK khi không vi phạm. Chạy mỗi ngày một lần (CreditFlagJob) và ngay sau mỗi lần ghi nhận thu tiền.
// Cờ được ghi vào customers.credit_status kèm nhật ký khi đổi; việc chặn hay chuyển duyệt đơn mới theo
// credit.overdue_mode do CustomerCredit quyết định từ số liệu hiện tại.
public class CreditFlagService {

    private static final String AUDIT_ENTITY = "CUSTOMER";

    private final CustomerCreditDao creditDao = new CustomerCreditDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Đánh giá lại một đại lý trong transaction của người gọi; trả về cờ mới (null nếu không còn đại lý)
    public String evaluate(Connection connection, long customerId, Long actorUserId, String ipAddress)
            throws SQLException {
        CustomerCredit credit = creditDao.find(connection, customerId, DateTimeUtil.today());
        if (credit == null) {
            return null;
        }
        String flag = credit.getFlag();
        String old;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT credit_status FROM customers WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                old = resultSet.getString(1);
            }
        }
        if (flag.equals(old)) {
            return flag;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE customers SET credit_status = ?, updated_at = UTC_TIMESTAMP() WHERE id = ?")) {
            statement.setString(1, flag);
            statement.setLong(2, customerId);
            statement.executeUpdate();
        }
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("creditStatus", old);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("creditStatus", flag);
        after.put("overdueAmount", credit.getOverdueAmount().stripTrailingZeros().toPlainString());
        after.put("used", credit.getUsed().stripTrailingZeros().toPlainString());
        after.put("creditLimit", credit.getCreditLimit().stripTrailingZeros().toPlainString());
        auditLogDao.insert(connection, actorUserId, "CREDIT_FLAG_CHANGE", AUDIT_ENTITY, customerId,
                JsonUtil.object(before), JsonUtil.object(after), null, ipAddress);
        return flag;
    }

    // Đánh giá lại mọi đại lý đang hoạt động, mỗi đại lý một transaction; trả về số đại lý đổi cờ
    public int evaluateAll() throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM customers WHERE status = 'ACTIVE' ORDER BY id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getLong(1));
            }
        }
        int changed = 0;
        for (long id : ids) {
            try (Connection connection = DbConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    String before = flagOf(connection, id);
                    String after = evaluate(connection, id, null, null);
                    connection.commit();
                    if (after != null && !after.equals(before)) {
                        changed++;
                    }
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            }
        }
        return changed;
    }

    private static String flagOf(Connection connection, long customerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT credit_status FROM customers WHERE id = ?")) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }
}
