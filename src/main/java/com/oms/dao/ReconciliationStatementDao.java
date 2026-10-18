package com.oms.dao;

import com.oms.model.ReconciliationStatement;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Biên bản đối chiếu công nợ (S7-06). Số dư lấy từ sổ công nợ ar_ledger_entries: Nợ tăng, Có giảm.
public class ReconciliationStatementDao {

    private static final String DOC_TYPE = "STATEMENT";
    private static final String NO_PREFIX = "DC";
    private static final String HEADER = "SELECT s.id, s.statement_no, s.customer_id, c.code, c.name, s.period_from,"
            + " s.period_to, s.opening_balance, s.total_debit, s.total_credit, s.closing_balance, s.status,"
            + " s.customer_response_note, u.full_name, s.responded_at, s.created_at FROM reconciliation_statements s"
            + " JOIN customers c ON c.id = s.customer_id LEFT JOIN users u ON u.id = s.responded_by";

    // Khách hàng đang hoạt động để chọn lập biên bản
    public List<SelectOption> findCustomers() throws SQLException {
        List<SelectOption> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, code, name FROM customers WHERE status = 'ACTIVE' ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(new SelectOption(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3)));
            }
        }
        return result;
    }

    // Số dư (Nợ - Có) trước ngày from
    public BigDecimal findBalanceBefore(Connection connection, long customerId, LocalDate from) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(debit_amount - credit_amount), 0) FROM ar_ledger_entries"
                        + " WHERE customer_id = ? AND entry_date < ?")) {
            statement.setLong(1, customerId);
            statement.setDate(2, Date.valueOf(from));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    // Tổng phát sinh Nợ, Có trong kỳ [from, to]
    public BigDecimal[] findTurnover(Connection connection, long customerId, LocalDate from, LocalDate to)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(debit_amount), 0), COALESCE(SUM(credit_amount), 0) FROM ar_ledger_entries"
                        + " WHERE customer_id = ? AND entry_date BETWEEN ? AND ?")) {
            statement.setLong(1, customerId);
            statement.setDate(2, Date.valueOf(from));
            statement.setDate(3, Date.valueOf(to));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new BigDecimal[]{resultSet.getBigDecimal(1), resultSet.getBigDecimal(2)};
            }
        }
    }

    public String nextStatementNo(Connection connection, int year) throws SQLException {
        String period = String.valueOf(year);
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO document_sequences (doc_type, period_key, prefix, last_number) VALUES (?, ?, ?, 0)")) {
            statement.setString(1, DOC_TYPE);
            statement.setString(2, period);
            statement.setString(3, NO_PREFIX);
            statement.executeUpdate();
        }
        long next;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT last_number FROM document_sequences WHERE doc_type = ? AND period_key = ? FOR UPDATE")) {
            statement.setString(1, DOC_TYPE);
            statement.setString(2, period);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                next = resultSet.getLong(1) + 1;
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE document_sequences SET last_number = ? WHERE doc_type = ? AND period_key = ?")) {
            statement.setLong(1, next);
            statement.setString(2, DOC_TYPE);
            statement.setString(3, period);
            statement.executeUpdate();
        }
        return NO_PREFIX + period + "-" + String.format("%06d", next);
    }

    public long insert(Connection connection, String statementNo, long customerId, LocalDate from, LocalDate to,
                       BigDecimal opening, BigDecimal debit, BigDecimal credit, BigDecimal closing, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO reconciliation_statements (statement_no, customer_id, period_from, period_to,"
                + " opening_balance, total_debit, total_credit, closing_balance, status, created_at, created_by,"
                + " updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', UTC_TIMESTAMP(), ?,"
                + " UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, statementNo);
            statement.setLong(2, customerId);
            statement.setDate(3, Date.valueOf(from));
            statement.setDate(4, Date.valueOf(to));
            statement.setBigDecimal(5, opening);
            statement.setBigDecimal(6, debit);
            statement.setBigDecimal(7, credit);
            statement.setBigDecimal(8, closing);
            statement.setLong(9, actorUserId);
            statement.setLong(10, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // Mới nhất trước; customerId null: mọi đại lý; onlySent: chỉ biên bản đã gửi trở đi (cổng đại lý)
    public List<ReconciliationStatement> findAll(Long customerId, boolean onlySent, int limit) throws SQLException {
        String sql = HEADER + " WHERE (? IS NULL OR s.customer_id = ?)"
                + (onlySent ? " AND s.status <> 'DRAFT'" : "") + " ORDER BY s.id DESC LIMIT ?";
        List<ReconciliationStatement> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (customerId == null) {
                statement.setNull(1, Types.BIGINT);
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(1, customerId);
                statement.setLong(2, customerId);
            }
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(read(resultSet, List.of()));
                }
            }
        }
        return result;
    }

    // Kèm bút toán trong kỳ; null nếu không có
    public ReconciliationStatement find(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            ReconciliationStatement header;
            try (PreparedStatement statement = connection.prepareStatement(HEADER + " WHERE s.id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    header = read(resultSet, List.of());
                }
            }
            List<ReconciliationStatement.Entry> entries = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT entry_date, entry_type, note, debit_amount, credit_amount FROM ar_ledger_entries"
                            + " WHERE customer_id = ? AND entry_date BETWEEN ? AND ? ORDER BY entry_date, id")) {
                statement.setLong(1, header.getCustomerId());
                statement.setDate(2, Date.valueOf(header.getPeriodFrom()));
                statement.setDate(3, Date.valueOf(header.getPeriodTo()));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        entries.add(new ReconciliationStatement.Entry(resultSet.getObject(1, LocalDate.class),
                                resultSet.getString(2), resultSet.getString(3), resultSet.getBigDecimal(4),
                                resultSet.getBigDecimal(5)));
                    }
                }
            }
            return header.withEntries(entries);
        }
    }

    // Khoá biên bản; null nếu không có. Trả trạng thái và đại lý
    public Lock lock(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, statement_no, customer_id, status FROM reconciliation_statements WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new Lock(resultSet.getLong(1), resultSet.getString(2),
                        resultSet.getLong(3), resultSet.getString(4)) : null;
            }
        }
    }

    public void markSent(Connection connection, long id, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE reconciliation_statements SET status = 'SENT', updated_at = UTC_TIMESTAMP(), updated_by = ?"
                        + " WHERE id = ?")) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void respond(Connection connection, long id, String status, String note, long userId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE reconciliation_statements SET status = ?, customer_response_note = ?, responded_by = ?,"
                        + " responded_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP(), updated_by = ?"
                        + " WHERE id = ?")) {
            statement.setString(1, status);
            statement.setString(2, note);
            statement.setLong(3, userId);
            statement.setLong(4, userId);
            statement.setLong(5, id);
            statement.executeUpdate();
        }
    }

    private static ReconciliationStatement read(ResultSet resultSet, List<ReconciliationStatement.Entry> entries)
            throws SQLException {
        return new ReconciliationStatement(resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3),
                resultSet.getString(4), resultSet.getString(5), resultSet.getObject(6, LocalDate.class),
                resultSet.getObject(7, LocalDate.class), resultSet.getBigDecimal(8), resultSet.getBigDecimal(9),
                resultSet.getBigDecimal(10), resultSet.getBigDecimal(11), resultSet.getString(12),
                resultSet.getString(13), resultSet.getString(14), resultSet.getObject(15, LocalDateTime.class),
                resultSet.getObject(16, LocalDateTime.class), entries);
    }

    public record Lock(long id, String statementNo, long customerId, String status) {
    }
}
