package com.oms.dao;

import com.oms.model.CreditNote;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Chứng từ điều chỉnh giảm trừ công nợ (S8-02). Các hàm nhận Connection chạy trong transaction của CreditNoteService.
public class CreditNoteDao {

    private static final String DOC_TYPE = "CREDIT_NOTE";
    private static final String NO_PREFIX = "CN";
    private static final String HEADER = "SELECT n.id, n.credit_note_no, n.invoice_id, v.invoice_no, r.return_no,"
            + " c.name, n.total_amount, n.reason, u.full_name, n.issued_at FROM credit_notes n"
            + " JOIN invoices v ON v.id = n.invoice_id JOIN customers c ON c.id = n.customer_id"
            + " LEFT JOIN sales_returns r ON r.id = n.return_id LEFT JOIN users u ON u.id = n.issued_by";

    // Khoá phiếu trả để lập chứng từ; null nếu không có
    public ReturnForCredit lockReturn(Connection connection, long returnId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT r.id, r.return_no, r.status, r.invoice_id, r.customer_id, v.invoice_no, v.status,"
                        + " v.outstanding_amount, (SELECT COUNT(*) FROM credit_notes WHERE return_id = r.id)"
                        + " FROM sales_returns r JOIN invoices v ON v.id = r.invoice_id WHERE r.id = ?"
                        + " FOR UPDATE")) {
            statement.setLong(1, returnId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new ReturnForCredit(resultSet.getLong(1), resultSet.getString(2),
                        resultSet.getString(3), resultSet.getLong(4), resultSet.getLong(5), resultSet.getString(6),
                        resultSet.getString(7), resultSet.getBigDecimal(8), resultSet.getInt(9) > 0) : null;
            }
        }
    }

    // Khoá hoá đơn gốc để cập nhật số đã giảm trừ
    public void lockInvoice(Connection connection, long invoiceId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM invoices WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, invoiceId);
            statement.executeQuery().close();
        }
    }

    // Dòng trả kèm giá đã bán trên hoá đơn gốc (thành tiền dòng và số lượng cơ sở của dòng hoá đơn)
    public List<Line> findLines(Connection connection, long returnId) throws SQLException {
        String sql = "SELECT s.id, s.invoice_item_id, s.qty_base, i.qty_base, i.line_total, p.sku, p.name"
                + " FROM sales_return_items s JOIN invoice_items i ON i.id = s.invoice_item_id"
                + " JOIN products p ON p.id = s.product_id WHERE s.return_id = ? ORDER BY s.id";
        List<Line> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, returnId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lines.add(new Line(resultSet.getLong(1), resultSet.getLong(2), resultSet.getBigDecimal(3),
                            resultSet.getBigDecimal(4), resultSet.getBigDecimal(5), resultSet.getString(6),
                            resultSet.getString(7)));
                }
            }
        }
        return lines;
    }

    public String nextCreditNoteNo(Connection connection, int year) throws SQLException {
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

    public long insert(Connection connection, String no, long invoiceId, long returnId, long customerId,
                       BigDecimal total, String reason, long actorUserId) throws SQLException {
        String sql = "INSERT INTO credit_notes (credit_note_no, invoice_id, return_id, customer_id, total_amount,"
                + " reason, status, issued_by, issued_at, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, 'ISSUED', ?, UTC_TIMESTAMP(), UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, no);
            statement.setLong(2, invoiceId);
            statement.setLong(3, returnId);
            statement.setLong(4, customerId);
            statement.setBigDecimal(5, total);
            statement.setString(6, reason);
            statement.setLong(7, actorUserId);
            statement.setLong(8, actorUserId);
            statement.setLong(9, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertItem(Connection connection, long noteId, Line line, BigDecimal unitPrice, BigDecimal amount)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO credit_note_items (credit_note_id, invoice_item_id, return_item_id, qty_base,"
                        + " unit_price, amount) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, noteId);
            statement.setLong(2, line.invoiceItemId());
            statement.setLong(3, line.returnItemId());
            statement.setBigDecimal(4, line.qtyBase());
            statement.setBigDecimal(5, unitPrice);
            statement.setBigDecimal(6, amount);
            statement.executeUpdate();
        }
    }

    // Cộng số đã giảm trừ vào hoá đơn gốc (nội dung hoá đơn giữ nguyên) và chuyển trạng thái nếu hết nợ
    public void applyCredit(Connection connection, long invoiceId, BigDecimal applied, long actorUserId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE invoices SET amount_credited = amount_credited + ?, status = IF(amount_paid + amount_credited"
                        + " >= total_amount, 'PAID', IF(amount_paid + amount_credited > 0, 'PARTIALLY_PAID',"
                        + " status)), version = version + 1, updated_at = UTC_TIMESTAMP(), updated_by = ?"
                        + " WHERE id = ?")) {
            statement.setBigDecimal(1, applied);
            statement.setLong(2, actorUserId);
            statement.setLong(3, invoiceId);
            statement.executeUpdate();
        }
    }

    // Bút toán Có loại CREDIT_NOTE trên sổ công nợ (chỉ thêm)
    public void insertLedgerEntry(Connection connection, long customerId, BigDecimal credit, long noteId,
                                  LocalDate entryDate, String note, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ar_ledger_entries (customer_id, entry_type, debit_amount, credit_amount, ref_type, ref_id,"
                        + " entry_date, note, created_at, created_by) VALUES (?, 'CREDIT_NOTE', 0, ?, 'CREDIT_NOTE',"
                        + " ?, ?, ?, UTC_TIMESTAMP(), ?)")) {
            statement.setLong(1, customerId);
            statement.setBigDecimal(2, credit);
            statement.setLong(3, noteId);
            statement.setDate(4, Date.valueOf(entryDate));
            statement.setString(5, note);
            statement.setLong(6, actorUserId);
            statement.executeUpdate();
        }
    }

    public List<CreditNote> findAll(int limit) throws SQLException {
        List<CreditNote> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(HEADER + " ORDER BY n.id DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(read(resultSet));
                }
            }
        }
        return result;
    }

    // Kèm dòng giảm trừ; null nếu không có
    public CreditNote find(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            CreditNote header;
            try (PreparedStatement statement = connection.prepareStatement(HEADER + " WHERE n.id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    header = read(resultSet);
                }
            }
            List<CreditNote.Item> items = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT p.sku, p.name, ci.qty_base, ci.unit_price, ci.amount FROM credit_note_items ci"
                            + " JOIN invoice_items i ON i.id = ci.invoice_item_id JOIN products p ON p.id = i.product_id"
                            + " WHERE ci.credit_note_id = ? ORDER BY ci.id")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        items.add(new CreditNote.Item(resultSet.getString(1), resultSet.getString(2),
                                resultSet.getBigDecimal(3), resultSet.getBigDecimal(4), resultSet.getBigDecimal(5)));
                    }
                }
            }
            return header.withItems(items);
        }
    }

    // Mã chứng từ của phiếu trả (đã lập hay chưa); null nếu chưa
    public Long findIdByReturn(long returnId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM credit_notes WHERE return_id = ?")) {
            statement.setLong(1, returnId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    private static CreditNote read(ResultSet resultSet) throws SQLException {
        return new CreditNote(resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3),
                resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getBigDecimal(7),
                resultSet.getString(8), resultSet.getString(9), resultSet.getObject(10, LocalDateTime.class),
                List.of());
    }

    public record ReturnForCredit(long id, String returnNo, String status, long invoiceId, long customerId,
                                  String invoiceNo, String invoiceStatus, BigDecimal invoiceOutstanding,
                                  boolean hasCreditNote) {
    }

    public record Line(long returnItemId, long invoiceItemId, BigDecimal qtyBase, BigDecimal invoiceQtyBase,
                       BigDecimal invoiceLineTotal, String sku, String productName) {
    }
}
