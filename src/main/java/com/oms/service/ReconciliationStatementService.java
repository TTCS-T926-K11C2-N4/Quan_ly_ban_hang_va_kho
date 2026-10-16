package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.NotificationDao;
import com.oms.dao.PortalOrderDao;
import com.oms.dao.ReconciliationStatementDao;
import com.oms.model.ReconciliationStatement;
import com.oms.model.SelectOption;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Biên bản đối chiếu công nợ định kỳ (S7-06): kế toán lập theo kỳ (số dư đầu kỳ, phát sinh, số dư cuối kỳ chụp từ sổ
// công nợ), gửi đại lý; đại lý xác nhận hoặc báo lệch kèm ghi chú ngay trên cổng. Đã phản hồi thì không đổi.
public class ReconciliationStatementService {

    public static final int LIST_LIMIT = 100;
    public static final int NOTE_MAX = 1000;
    private static final String AUDIT_ENTITY = "RECONCILIATION";
    private static final String NOT_FOUND = "NOT_FOUND";

    private final ReconciliationStatementDao dao = new ReconciliationStatementDao();
    private final PortalOrderDao portalOrderDao = new PortalOrderDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();
    private final NotificationDao notificationDao = new NotificationDao();

    // error != null: lỗi nghiệp vụ (hoặc "NOT_FOUND"); ngược lại id và số biên bản
    public static final class Outcome {
        private final String error;
        private final long id;
        private final String statementNo;

        private Outcome(String error, long id, String statementNo) {
            this.error = error;
            this.id = id;
            this.statementNo = statementNo;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0, null);
        }

        static Outcome success(long id, String statementNo) {
            return new Outcome(null, id, statementNo);
        }

        public boolean isNotFound() {
            return NOT_FOUND.equals(error);
        }

        public String getError() {
            return error;
        }

        public long getId() {
            return id;
        }

        public String getStatementNo() {
            return statementNo;
        }
    }

    public List<SelectOption> customers() throws SQLException {
        return dao.findCustomers();
    }

    public List<ReconciliationStatement> findAll() throws SQLException {
        return dao.findAll(null, false, LIST_LIMIT);
    }

    public ReconciliationStatement find(long id) throws SQLException {
        return dao.find(id);
    }

    // Biên bản của chính đại lý (đã gửi trở đi); tài khoản chưa gắn đại lý thì không có
    public List<ReconciliationStatement> findOwn(long userId) throws SQLException {
        Long customerId = portalOrderDao.findCustomerId(userId);
        return customerId == null ? List.of() : dao.findAll(customerId, true, LIST_LIMIT);
    }

    // null nếu không có biên bản, biên bản nháp hoặc của đại lý khác
    public ReconciliationStatement findOwn(long userId, long id) throws SQLException {
        Long customerId = portalOrderDao.findCustomerId(userId);
        if (customerId == null) {
            return null;
        }
        ReconciliationStatement statement = dao.find(id);
        return statement != null && statement.getCustomerId() == customerId && !statement.isDraft() ? statement : null;
    }

    // Số dư cuối kỳ = số dư đầu kỳ + phát sinh Nợ - phát sinh Có
    static BigDecimal closing(BigDecimal opening, BigDecimal debit, BigDecimal credit) {
        return opening.add(debit).subtract(credit);
    }

    public Outcome create(long actorUserId, long customerId, String fromText, String toText, String ipAddress)
            throws SQLException {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromText == null ? "" : fromText.trim());
            to = LocalDate.parse(toText == null ? "" : toText.trim());
        } catch (DateTimeParseException e) {
            return Outcome.failure("Chọn kỳ đối chiếu từ ngày đến ngày.");
        }
        if (to.isBefore(from)) {
            return Outcome.failure("Ngày kết thúc kỳ không được trước ngày bắt đầu.");
        }
        if (to.isAfter(DateTimeUtil.today())) {
            return Outcome.failure("Kỳ đối chiếu không được kéo dài quá hôm nay.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT id FROM customers WHERE id = ? FOR UPDATE")) {
                    lock.setLong(1, customerId);
                    try (ResultSet resultSet = lock.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return Outcome.failure(NOT_FOUND);
                        }
                    }
                }
                BigDecimal opening = dao.findBalanceBefore(connection, customerId, from);
                BigDecimal[] turnover = dao.findTurnover(connection, customerId, from, to);
                BigDecimal closing = closing(opening, turnover[0], turnover[1]);
                String no = dao.nextStatementNo(connection, DateTimeUtil.today().getYear());
                long id = dao.insert(connection, no, customerId, from, to, opening, turnover[0], turnover[1],
                        closing, actorUserId);
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("statementNo", no);
                values.put("customerId", customerId);
                values.put("periodFrom", from.toString());
                values.put("periodTo", to.toString());
                values.put("opening", opening.toPlainString());
                values.put("debit", turnover[0].toPlainString());
                values.put("credit", turnover[1].toPlainString());
                values.put("closing", closing.toPlainString());
                auditLogDao.insert(connection, actorUserId, "STATEMENT_CREATE", AUDIT_ENTITY, id, null,
                        JsonUtil.object(values), null, ipAddress);
                connection.commit();
                return Outcome.success(id, no);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Gửi biên bản nháp cho đại lý: thông báo tới các tài khoản đang hoạt động của đại lý
    public Outcome send(long actorUserId, long id, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ReconciliationStatementDao.Lock statement = dao.lock(connection, id);
                if (statement == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!ReconciliationStatement.DRAFT.equals(statement.status())) {
                    connection.rollback();
                    return Outcome.failure("Biên bản " + statement.statementNo() + " đã được gửi.");
                }
                dao.markSent(connection, id, actorUserId);
                for (long userId : findAccounts(connection, statement.customerId())) {
                    notificationDao.insert(connection, userId, "STATEMENT",
                            "Biên bản đối chiếu công nợ " + statement.statementNo(),
                            "Công ty gửi biên bản đối chiếu công nợ " + statement.statementNo()
                                    + ". Vui lòng kiểm tra và xác nhận hoặc báo lệch trên trang Đối chiếu công nợ.",
                            "STATEMENT", id, false);
                }
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("status", ReconciliationStatement.DRAFT);
                Map<String, Object> after = new LinkedHashMap<>();
                after.put("statementNo", statement.statementNo());
                after.put("status", ReconciliationStatement.SENT);
                auditLogDao.insert(connection, actorUserId, "STATEMENT_SEND", AUDIT_ENTITY, id,
                        JsonUtil.object(before), JsonUtil.object(after), null, ipAddress);
                connection.commit();
                return Outcome.success(id, statement.statementNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Đại lý xác nhận (confirm = true) hoặc báo lệch (cần ghi chú); chỉ biên bản của chính đại lý và đang chờ phản hồi
    public Outcome respond(long userId, long id, boolean confirm, String note, String ipAddress)
            throws SQLException {
        String cleanNote = note == null || note.trim().isEmpty() ? null : note.trim();
        if (!confirm && cleanNote == null) {
            return Outcome.failure("Nhập nội dung chênh lệch để công ty kiểm tra lại.");
        }
        if (cleanNote != null && cleanNote.length() > NOTE_MAX) {
            return Outcome.failure("Ghi chú tối đa " + NOTE_MAX + " ký tự.");
        }
        Long customerId = portalOrderDao.findCustomerId(userId);
        if (customerId == null) {
            return Outcome.failure(NOT_FOUND);
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ReconciliationStatementDao.Lock statement = dao.lock(connection, id);
                if (statement == null || statement.customerId() != customerId
                        || ReconciliationStatement.DRAFT.equals(statement.status())) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!ReconciliationStatement.SENT.equals(statement.status())) {
                    connection.rollback();
                    return Outcome.failure("Biên bản " + statement.statementNo() + " đã được phản hồi.");
                }
                String status = confirm ? ReconciliationStatement.CONFIRMED : ReconciliationStatement.DISPUTED;
                dao.respond(connection, id, status, cleanNote, userId);
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("status", ReconciliationStatement.SENT);
                Map<String, Object> after = new LinkedHashMap<>();
                after.put("statementNo", statement.statementNo());
                after.put("status", status);
                auditLogDao.insert(connection, userId, "STATEMENT_RESPOND", AUDIT_ENTITY, id,
                        JsonUtil.object(before), JsonUtil.object(after), cleanNote, ipAddress);
                connection.commit();
                return Outcome.success(id, statement.statementNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

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
}
