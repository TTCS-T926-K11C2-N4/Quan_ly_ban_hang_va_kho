package com.oms.service;

import com.oms.dao.NotificationDao;
import com.oms.util.DbConnection;
import com.oms.util.MailSender;
import com.oms.util.MoneyUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// Nhắc hạn thanh toán tự động (S7-08): gửi email (nếu đại lý có email) và thông báo trong ứng dụng cho các tài khoản
// của đại lý, trước hạn 3 ngày (BEFORE_DUE_3D) và khi quá hạn (OVERDUE: ngày đầu tiên quá hạn rồi mỗi 7 ngày).
// payment_reminder_logs có khoá duy nhất (hoá đơn, loại nhắc, ngày) nên chạy lại nhiều lần trong ngày không gửi trùng.
public class PaymentReminderService {

    public static final String BEFORE_DUE = "BEFORE_DUE_3D";
    public static final String OVERDUE = "OVERDUE";
    private static final String NOTIFICATION_TYPE = "PAYMENT_REMINDER";
    private static final int BEFORE_DAYS = 3;
    private static final int OVERDUE_REPEAT_DAYS = 7;

    private final NotificationDao notificationDao = new NotificationDao();

    // Loại nhắc của hoá đơn có hạn dueDate vào ngày today; null nếu hôm nay không phải ngày nhắc
    static String reminderType(LocalDate dueDate, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, dueDate);
        if (days == BEFORE_DAYS) {
            return BEFORE_DUE;
        }
        long overdueDays = -days;
        if (overdueDays >= 1 && (overdueDays - 1) % OVERDUE_REPEAT_DAYS == 0) {
            return OVERDUE;
        }
        return null;
    }

    // Chạy cho ngày today; trả về số nhắc hạn mới được gửi
    public int run(LocalDate today) throws SQLException {
        List<Due> dues = findDue(today);
        int sent = 0;
        for (Due due : dues) {
            String type = reminderType(due.dueDate(), today);
            if (type == null) {
                continue;
            }
            try (Connection connection = DbConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    if (!logOnce(connection, due.invoiceId(), type, today)) {
                        connection.rollback();
                        continue;
                    }
                    String title = BEFORE_DUE.equals(type)
                            ? "Hoá đơn " + due.invoiceNo() + " sắp đến hạn thanh toán"
                            : "Hoá đơn " + due.invoiceNo() + " đã quá hạn thanh toán";
                    String body = (BEFORE_DUE.equals(type)
                            ? "Hoá đơn " + due.invoiceNo() + " đến hạn thanh toán ngày " + due.dueDate() + "."
                            : "Hoá đơn " + due.invoiceNo() + " đã quá hạn từ ngày " + due.dueDate() + ".")
                            + " Số tiền còn nợ: " + MoneyUtil.format(due.outstanding()) + " ₫."
                            + " Vui lòng thanh toán để tránh bị khoá đặt đơn.";
                    boolean emailed = due.email() != null && !due.email().isBlank();
                    for (long userId : findAccounts(connection, due.customerId())) {
                        notificationDao.insert(connection, userId, NOTIFICATION_TYPE, title, body, "INVOICE",
                                due.invoiceId(), emailed);
                    }
                    connection.commit();
                    if (emailed) {
                        MailSender.sendAsync(due.email(), title, body);
                    }
                    sent++;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            }
        }
        return sent;
    }

    // Hoá đơn còn nợ có hạn rơi vào ngày nhắc (cách hôm nay đúng 3 ngày hoặc đã quá hạn)
    private static List<Due> findDue(LocalDate today) throws SQLException {
        String sql = "SELECT v.id, v.invoice_no, v.customer_id, c.email, v.due_date, v.outstanding_amount"
                + " FROM invoices v JOIN customers c ON c.id = v.customer_id"
                + " WHERE v.status IN ('ISSUED', 'PARTIALLY_PAID') AND v.outstanding_amount > 0"
                + " AND v.due_date <= ? ORDER BY v.due_date, v.id";
        List<Due> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today.plusDays(BEFORE_DAYS)));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new Due(resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3),
                            resultSet.getString(4), resultSet.getObject(5, LocalDate.class),
                            resultSet.getBigDecimal(6)));
                }
            }
        }
        return result;
    }

    // false nếu hôm nay đã nhắc loại này cho hoá đơn này
    private static boolean logOnce(Connection connection, long invoiceId, String type, LocalDate today)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO payment_reminder_logs (invoice_id, reminder_type, sent_on, created_at)"
                        + " VALUES (?, ?, ?, UTC_TIMESTAMP())")) {
            statement.setLong(1, invoiceId);
            statement.setString(2, type);
            statement.setDate(3, Date.valueOf(today));
            return statement.executeUpdate() == 1;
        }
    }

    // Tài khoản đang hoạt động của đại lý
    private static List<Long> findAccounts(Connection connection, long customerId) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE customer_id = ? AND status = 'ACTIVE'")) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
            }
        }
        return ids;
    }

    private record Due(long invoiceId, String invoiceNo, long customerId, String email, LocalDate dueDate,
                       BigDecimal outstanding) {
    }
}
