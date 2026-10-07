package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.CreditLimitDao;
import com.oms.model.CreditLimitForm;
import com.oms.model.Customer;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

// Hạn mức công nợ của đại lý (S3-05): hạn mức tiền và số ngày nợ tối đa; mỗi lần đổi phải có lý do,
// ghi credit_limit_history và nhật ký thao tác trong cùng transaction.
public class CreditLimitService {

    public static final int MAX_DEBT_DAYS = 365;
    public static final int REASON_MAX_LENGTH = 500;

    // Tiền VNĐ là số nguyên; cho gõ có dấu chấm ngăn nghìn (100.000.000) hoặc liền (100000000)
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{3})*|\\d+");
    // credit_limit decimal(18,2): tối đa 16 chữ số phần nguyên
    private static final int MONEY_MAX_DIGITS = 16;
    private static final Pattern DAYS_PATTERN = Pattern.compile("\\d{1,3}");
    private static final String ENTITY = "CREDIT_LIMIT";

    private final CreditLimitDao creditLimitDao = new CreditLimitDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Lỗi theo tên ô ("form" là lỗi chung); rỗng nghĩa là hợp lệ
    public Map<String, String> validate(CreditLimitForm form, Customer customer) {
        Map<String, String> errors = new LinkedHashMap<>();
        BigDecimal limit = parseMoney(form.getCreditLimit());
        Integer days = parseDays(form.getMaxDebtDays());

        if (form.getCreditLimit() == null) {
            errors.put("creditLimit", "Vui lòng nhập hạn mức tiền tối đa.");
        } else if (limit == null) {
            errors.put("creditLimit", "Hạn mức tiền là số nguyên không âm, ví dụ 100.000.000.");
        } else if (limit.precision() > MONEY_MAX_DIGITS) {
            errors.put("creditLimit", "Hạn mức tiền tối đa " + MONEY_MAX_DIGITS + " chữ số.");
        }

        if (form.getMaxDebtDays() == null) {
            errors.put("maxDebtDays", "Vui lòng nhập số ngày nợ tối đa.");
        } else if (days == null || days > MAX_DEBT_DAYS) {
            errors.put("maxDebtDays", "Số ngày nợ tối đa là số nguyên từ 0 đến " + MAX_DEBT_DAYS + ".");
        }

        if (form.getReason() == null) {
            errors.put("reason", "Vui lòng nhập lý do thay đổi hạn mức.");
        } else if (form.getReason().length() > REASON_MAX_LENGTH) {
            errors.put("reason", "Lý do tối đa " + REASON_MAX_LENGTH + " ký tự.");
        }

        if (errors.isEmpty() && limit.compareTo(customer.getCreditLimit()) == 0
                && days == customer.getMaxDebtDays()) {
            errors.put("form", "Hạn mức tiền và số ngày nợ chưa thay đổi so với hiện tại.");
        }
        return errors;
    }

    // Gọi validate trước. false nếu đại lý vừa được người khác sửa hạn mức (version đã khác) hoặc đã bị xoá.
    public boolean update(Customer customer, CreditLimitForm form, long actorUserId, String ipAddress)
            throws SQLException {
        long version = parseVersion(form.getVersion());
        BigDecimal limit = parseMoney(form.getCreditLimit());
        int days = parseDays(form.getMaxDebtDays());
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (version != customer.getVersion() || !creditLimitDao.updateCreditLimit(connection, customer.getId(),
                        version, limit, days, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                creditLimitDao.insertCreditLimitHistory(connection, customer.getId(), customer.getCreditLimit(),
                        customer.getMaxDebtDays(), limit, days, form.getReason(), actorUserId);
                auditLogDao.insert(connection, actorUserId, "CREDIT_LIMIT_UPDATE", ENTITY, customer.getId(),
                        JsonUtil.object(toValues(customer.getCode(), customer.getCreditLimit(),
                                customer.getMaxDebtDays())),
                        JsonUtil.object(toValues(customer.getCode(), limit, days)), form.getReason(), ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // null nếu sai định dạng
    static BigDecimal parseMoney(String text) {
        if (text == null || !MONEY_PATTERN.matcher(text).matches()) {
            return null;
        }
        return new BigDecimal(text.replace(".", ""));
    }

    private static Integer parseDays(String text) {
        return text == null || !DAYS_PATTERN.matcher(text).matches() ? null : Integer.valueOf(text);
    }

    // Version sai định dạng coi như đã cũ để không ghi đè dữ liệu
    private static long parseVersion(String text) {
        try {
            return text == null ? -1 : Long.parseLong(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Tiền ghi dạng chuỗi để JSON.parse ở trang nhật ký không làm tròn số lớn
    private static Map<String, Object> toValues(String code, BigDecimal limit, int days) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("creditLimit", limit.stripTrailingZeros().toPlainString());
        values.put("maxDebtDays", days);
        return values;
    }
}
