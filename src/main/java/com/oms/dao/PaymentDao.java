package com.oms.dao;

import com.oms.model.Debtor;
import com.oms.model.OpenInvoice;
import com.oms.model.Payment;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Phiếu thu và đối trừ công nợ (S7-02). Các hàm nhận Connection chạy trong transaction của PaymentService.
public class PaymentDao {

    private static final String DOC_TYPE = "PAYMENT";
    private static final String NO_PREFIX = "PT";
    private static final String OPEN = "('ISSUED', 'PARTIALLY_PAID')";

    // Đại lý còn nợ hoá đơn, nợ nhiều nhất trước
    public List<Debtor> findDebtors() throws SQLException {
        String sql = "SELECT c.id, c.code, c.name, COUNT(*), SUM(v.outstanding_amount) FROM invoices v"
                + " JOIN customers c ON c.id = v.customer_id WHERE v.status IN " + OPEN
                + " AND v.outstanding_amount > 0 GROUP BY c.id, c.code, c.name ORDER BY SUM(v.outstanding_amount) DESC,"
                + " c.name";
        List<Debtor> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(new Debtor(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
                        resultSet.getInt(4), resultSet.getBigDecimal(5)));
            }
        }
        return result;
    }

    // Tên đại lý; null nếu không có
    public String findCustomerName(long customerId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT name FROM customers WHERE id = ?")) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    // Hoá đơn còn nợ của đại lý, cũ nhất trước (hạn thanh toán, ngày lập, id)
    public List<OpenInvoice> findOpenInvoices(long customerId) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            return readOpen(connection, customerId, "");
        }
    }

    // Như trên nhưng khoá từng hoá đơn trong transaction ghi nhận
    public List<OpenInvoice> lockOpenInvoices(Connection connection, long customerId) throws SQLException {
        return readOpen(connection, customerId, " FOR UPDATE");
    }

    private static List<OpenInvoice> readOpen(Connection connection, long customerId, String suffix)
            throws SQLException {
        String sql = "SELECT id, invoice_no, issue_date, due_date, status, total_amount, outstanding_amount"
                + " FROM invoices WHERE customer_id = ? AND status IN " + OPEN + " AND outstanding_amount > 0"
                + " ORDER BY due_date, issue_date, id" + suffix;
        List<OpenInvoice> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new OpenInvoice(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getObject(3, LocalDate.class), resultSet.getObject(4, LocalDate.class),
                            resultSet.getString(5), resultSet.getBigDecimal(6), resultSet.getBigDecimal(7)));
                }
            }
        }
        return result;
    }

    // Khoá dòng đại lý để các lần thu đồng thời của cùng đại lý xếp hàng; false nếu không còn đại lý
    public boolean lockCustomer(Connection connection, long customerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM customers WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Số phiếu thu liên tục theo năm PT2026-000001, cấp trong transaction ghi nhận
    public String nextReceiptNo(Connection connection, int year) throws SQLException {
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

    public long insertPayment(Connection connection, String receiptNo, long customerId, BigDecimal amount,
                              String method, LocalDate paymentDate, String note, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO payments (receipt_no, customer_id, amount, method, payment_date, collected_by, status,"
                + " confirmed_by, confirmed_at, note, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, 'CONFIRMED', ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, receiptNo);
            statement.setLong(2, customerId);
            statement.setBigDecimal(3, amount);
            statement.setString(4, method);
            statement.setDate(5, Date.valueOf(paymentDate));
            statement.setLong(6, actorUserId);
            statement.setLong(7, actorUserId);
            if (note == null) {
                statement.setNull(8, Types.VARCHAR);
            } else {
                statement.setString(8, note);
            }
            statement.setLong(9, actorUserId);
            statement.setLong(10, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // Đối trừ vào một hoá đơn: ghi dòng đối trừ, cộng amount_paid, chuyển trạng thái theo số còn lại
    public void allocate(Connection connection, long paymentId, long invoiceId, BigDecimal amount, long actorUserId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO payment_allocations (payment_id, invoice_id, amount, created_at, created_by)"
                        + " VALUES (?, ?, ?, UTC_TIMESTAMP(), ?)")) {
            statement.setLong(1, paymentId);
            statement.setLong(2, invoiceId);
            statement.setBigDecimal(3, amount);
            statement.setLong(4, actorUserId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE invoices SET amount_paid = amount_paid + ?, status = IF(amount_paid + amount_credited"
                        + " >= total_amount, 'PAID', 'PARTIALLY_PAID'), version = version + 1,"
                        + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setBigDecimal(1, amount);
            statement.setLong(2, actorUserId);
            statement.setLong(3, invoiceId);
            statement.executeUpdate();
        }
    }

    // Bút toán Có trên sổ công nợ (chỉ thêm)
    public void insertLedgerEntry(Connection connection, long customerId, BigDecimal credit, long paymentId,
                                  LocalDate entryDate, String note, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ar_ledger_entries (customer_id, entry_type, debit_amount, credit_amount, ref_type, ref_id,"
                        + " entry_date, note, created_at, created_by) VALUES (?, 'PAYMENT', 0, ?, 'PAYMENT', ?, ?, ?,"
                        + " UTC_TIMESTAMP(), ?)")) {
            statement.setLong(1, customerId);
            statement.setBigDecimal(2, credit);
            statement.setLong(3, paymentId);
            statement.setDate(4, Date.valueOf(entryDate));
            statement.setString(5, note);
            statement.setLong(6, actorUserId);
            statement.executeUpdate();
        }
    }

    // Cộng (delta > 0) hoặc trừ số nợ hiện tại của đại lý trong customer_balances, cùng transaction với sổ
    public static void adjustBalance(Connection connection, long customerId, BigDecimal delta) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO customer_balances (customer_id, outstanding_amount, overdue_amount, updated_at, version)"
                        + " VALUES (?, ?, 0, UTC_TIMESTAMP(), 0) ON DUPLICATE KEY UPDATE"
                        + " outstanding_amount = outstanding_amount + VALUES(outstanding_amount),"
                        + " updated_at = UTC_TIMESTAMP(), version = version + 1")) {
            statement.setLong(1, customerId);
            statement.setBigDecimal(2, delta);
            statement.executeUpdate();
        }
    }

    // Mới nhất trước; customerId null: mọi đại lý
    public List<Payment> findPayments(Long customerId, int limit) throws SQLException {
        String sql = "SELECT p.id, p.receipt_no, p.customer_id, c.code, c.name, p.amount, p.method, p.payment_date,"
                + " u.full_name, p.status, p.note, p.created_at FROM payments p JOIN customers c ON c.id = p.customer_id"
                + " LEFT JOIN users u ON u.id = p.collected_by WHERE (? IS NULL OR p.customer_id = ?)"
                + " ORDER BY p.payment_date DESC, p.id DESC LIMIT ?";
        List<Payment> result = new ArrayList<>();
        Map<Long, List<Payment.Allocation>> byPayment = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
                        List<Payment.Allocation> allocations = new ArrayList<>();
                        byPayment.put(resultSet.getLong(1), allocations);
                        result.add(new Payment(resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3),
                                resultSet.getString(4), resultSet.getString(5), resultSet.getBigDecimal(6),
                                resultSet.getString(7), resultSet.getObject(8, LocalDate.class),
                                resultSet.getString(9), resultSet.getString(10), resultSet.getString(11),
                                resultSet.getObject(12, LocalDateTime.class), allocations));
                    }
                }
            }
            if (byPayment.isEmpty()) {
                return result;
            }
            String in = String.join(", ", Collections.nCopies(byPayment.size(), "?"));
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT a.payment_id, a.invoice_id, v.invoice_no, a.amount FROM payment_allocations a"
                            + " JOIN invoices v ON v.id = a.invoice_id WHERE a.payment_id IN (" + in + ")"
                            + " ORDER BY a.id")) {
                int index = 1;
                for (Long id : byPayment.keySet()) {
                    statement.setLong(index++, id);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        byPayment.get(resultSet.getLong(1)).add(new Payment.Allocation(resultSet.getLong(2),
                                resultSet.getString(3), resultSet.getBigDecimal(4)));
                    }
                }
            }
        }
        return result;
    }
}
