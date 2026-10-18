package com.oms.dao;

import com.oms.model.Invoice;
import com.oms.model.InvoiceCandidate;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// Hoá đơn bán hàng (S7-01): phiếu xuất đã giao chờ lập hoá đơn, cấp số liên tục, ghi hoá đơn và sổ công nợ.
// Các hàm nhận Connection chạy trong transaction của InvoiceService.
public class InvoiceDao {

    private static final String DOC_TYPE = "INVOICE";
    private static final String NO_PREFIX = "HD";
    private static final String NOT_INVOICED = " AND n.id NOT IN (SELECT delivery_note_item_id FROM invoice_items)";
    private static final String DELIVERED_QTY = "COALESCE(n.qty_delivered_base, n.qty_issued_base)";

    private static final String HEADER = "SELECT v.id, v.invoice_no, v.customer_id, c.code, c.name, c.tax_code, c.address,"
            + " v.issue_date, v.due_date, v.payment_term_days, v.status, v.subtotal_amount, v.discount_amount,"
            + " v.total_amount, v.amount_paid, v.amount_credited, v.outstanding_amount, u.full_name, v.issued_at,"
            + " v.note FROM invoices v JOIN customers c ON c.id = v.customer_id LEFT JOIN users u ON u.id = v.issued_by";

    // Phiếu xuất DELIVERED còn dòng chưa lập hoá đơn, cũ nhất trước
    public List<InvoiceCandidate> findCandidates() throws SQLException {
        String sql = "SELECT d.id, d.delivery_no, o.id, o.order_no, c.id, c.code, c.name, d.delivered_at,"
                + " ROUND(SUM(" + DELIVERED_QTY + " * oi.line_total / oi.qty_base), 0) FROM delivery_notes d"
                + " JOIN delivery_note_items n ON n.delivery_note_id = d.id"
                + " JOIN sales_order_items oi ON oi.id = n.order_item_id JOIN sales_orders o ON o.id = d.order_id"
                + " JOIN customers c ON c.id = o.customer_id WHERE d.status = 'DELIVERED' AND " + DELIVERED_QTY
                + " > 0" + NOT_INVOICED + " GROUP BY d.id, d.delivery_no, o.id, o.order_no, c.id, c.code, c.name,"
                + " d.delivered_at ORDER BY c.name, d.delivered_at, d.id";
        List<InvoiceCandidate> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(new InvoiceCandidate(resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3),
                        resultSet.getString(4), resultSet.getLong(5), resultSet.getString(6), resultSet.getString(7),
                        resultSet.getObject(8, LocalDateTime.class), resultSet.getBigDecimal(9)));
            }
        }
        return result;
    }

    // Dòng nguồn của các phiếu, khoá phiếu xuất theo thứ tự id; chỉ dòng chưa lập hoá đơn và có số thực giao > 0
    public List<Source> lockSources(Connection connection, Collection<Long> noteIds) throws SQLException {
        if (noteIds.isEmpty()) {
            return List.of();
        }
        String in = String.join(", ", Collections.nCopies(noteIds.size(), "?"));
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM delivery_notes WHERE id IN (" + in + ") ORDER BY id FOR UPDATE")) {
            int index = 1;
            for (Long id : noteIds) {
                statement.setLong(index++, id);
            }
            statement.executeQuery().close();
        }
        String sql = "SELECT d.id, d.delivery_no, d.status, o.id, o.order_no, o.customer_id, n.id, oi.id, oi.product_id,"
                + " oi.unit_id, oi.unit_factor, " + DELIVERED_QTY + ", oi.qty_base, oi.unit_price, oi.discount_amount,"
                + " oi.line_total, p.cost_price, (n.id IN (SELECT delivery_note_item_id FROM invoice_items))"
                + " FROM delivery_notes d JOIN sales_orders o ON o.id = d.order_id"
                + " JOIN delivery_note_items n ON n.delivery_note_id = d.id"
                + " JOIN sales_order_items oi ON oi.id = n.order_item_id JOIN products p ON p.id = oi.product_id"
                + " WHERE d.id IN (" + in + ") ORDER BY d.id, oi.line_no, n.id";
        List<Source> sources = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long id : noteIds) {
                statement.setLong(index++, id);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sources.add(new Source(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
                            resultSet.getLong(4), resultSet.getString(5), resultSet.getLong(6), resultSet.getLong(7),
                            resultSet.getLong(8), resultSet.getLong(9), resultSet.getLong(10),
                            resultSet.getBigDecimal(11), resultSet.getBigDecimal(12), resultSet.getBigDecimal(13),
                            resultSet.getBigDecimal(14), resultSet.getBigDecimal(15), resultSet.getBigDecimal(16),
                            resultSet.getBigDecimal(17), resultSet.getBoolean(18)));
                }
            }
        }
        return sources;
    }

    // Khoá dòng đại lý và đọc số ngày nợ; -1 nếu không còn đại lý
    public int lockCustomerTerm(Connection connection, long customerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT max_debt_days FROM customers WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : -1;
            }
        }
    }

    // Cấp số hoá đơn liên tục theo năm HD2026-000001, trong transaction lập hoá đơn (rollback thì số không bị mất)
    public String nextInvoiceNo(Connection connection, int year) throws SQLException {
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

    public long insertInvoice(Connection connection, String invoiceNo, long customerId, LocalDate issueDate,
                              LocalDate dueDate, int termDays, BigDecimal subtotal, BigDecimal discount,
                              BigDecimal total, String note, long actorUserId) throws SQLException {
        String sql = "INSERT INTO invoices (invoice_no, customer_id, issue_date, due_date, payment_term_days, status,"
                + " subtotal_amount, discount_amount, total_amount, issued_by, issued_at, note, created_at, created_by,"
                + " updated_at, updated_by) VALUES (?, ?, ?, ?, ?, 'ISSUED', ?, ?, ?, ?, UTC_TIMESTAMP(), ?,"
                + " UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, invoiceNo);
            statement.setLong(2, customerId);
            statement.setDate(3, Date.valueOf(issueDate));
            statement.setDate(4, Date.valueOf(dueDate));
            statement.setInt(5, termDays);
            statement.setBigDecimal(6, subtotal);
            statement.setBigDecimal(7, discount);
            statement.setBigDecimal(8, total);
            statement.setLong(9, actorUserId);
            if (note == null) {
                statement.setNull(10, Types.VARCHAR);
            } else {
                statement.setString(10, note);
            }
            statement.setLong(11, actorUserId);
            statement.setLong(12, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertItem(Connection connection, long invoiceId, Source source, BigDecimal qty, BigDecimal discount,
                           BigDecimal lineTotal) throws SQLException {
        String sql = "INSERT INTO invoice_items (invoice_id, delivery_note_item_id, order_item_id, product_id, unit_id,"
                + " unit_factor, qty, qty_base, unit_price, discount_amount, line_total, unit_cost_base_snapshot)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, invoiceId);
            statement.setLong(2, source.noteItemId());
            statement.setLong(3, source.orderItemId());
            statement.setLong(4, source.productId());
            statement.setLong(5, source.unitId());
            statement.setBigDecimal(6, source.unitFactor());
            statement.setBigDecimal(7, qty);
            statement.setBigDecimal(8, source.deliveredBase());
            statement.setBigDecimal(9, source.unitPrice());
            statement.setBigDecimal(10, discount);
            statement.setBigDecimal(11, lineTotal);
            statement.setBigDecimal(12, source.costPrice());
            statement.executeUpdate();
        }
    }

    // Bút toán Nợ trên sổ công nợ (chỉ thêm)
    public void insertLedgerEntry(Connection connection, long customerId, BigDecimal debit, long invoiceId,
                                  LocalDate entryDate, String note, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ar_ledger_entries (customer_id, entry_type, debit_amount, credit_amount, ref_type, ref_id,"
                        + " entry_date, note, created_at, created_by) VALUES (?, 'INVOICE', ?, 0, 'INVOICE', ?, ?, ?,"
                        + " UTC_TIMESTAMP(), ?)")) {
            statement.setLong(1, customerId);
            statement.setBigDecimal(2, debit);
            statement.setLong(3, invoiceId);
            statement.setDate(4, Date.valueOf(entryDate));
            statement.setString(5, note);
            statement.setLong(6, actorUserId);
            statement.executeUpdate();
        }
    }

    // Mới nhất trước; customerId, status null: không lọc; overdueOnly: chỉ hoá đơn quá hạn còn nợ
    public List<Invoice> findInvoices(Long customerId, String status, boolean overdueOnly, LocalDate today, int limit)
            throws SQLException {
        String sql = HEADER + " WHERE (? IS NULL OR v.customer_id = ?) AND (? IS NULL OR v.status = ?)"
                + (overdueOnly ? " AND v.outstanding_amount > 0 AND v.due_date < ? AND v.status IN ('ISSUED',"
                + " 'PARTIALLY_PAID')" : "") + " ORDER BY v.issue_date DESC, v.id DESC LIMIT ?";
        List<Invoice> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (customerId == null) {
                statement.setNull(index++, Types.BIGINT);
                statement.setNull(index++, Types.BIGINT);
            } else {
                statement.setLong(index++, customerId);
                statement.setLong(index++, customerId);
            }
            statement.setString(index++, status);
            statement.setString(index++, status);
            if (overdueOnly) {
                statement.setDate(index++, Date.valueOf(today));
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(readInvoice(resultSet, List.of()));
                }
            }
        }
        return result;
    }

    // null nếu không có hoá đơn
    public Invoice findDetail(long id) throws SQLException {
        String itemSql = "SELECT i.id, p.sku, p.name, u.name, i.qty, i.unit_price, i.discount_amount, i.line_total,"
                + " d.delivery_no, o.order_no FROM invoice_items i JOIN products p ON p.id = i.product_id"
                + " JOIN units u ON u.id = i.unit_id JOIN delivery_note_items n ON n.id = i.delivery_note_item_id"
                + " JOIN delivery_notes d ON d.id = n.delivery_note_id JOIN sales_orders o ON o.id = d.order_id"
                + " WHERE i.invoice_id = ? ORDER BY d.id, i.id";
        try (Connection connection = DbConnection.getConnection()) {
            List<Invoice.Item> items = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    int line = 1;
                    while (resultSet.next()) {
                        items.add(new Invoice.Item(line++, resultSet.getString(2), resultSet.getString(3),
                                resultSet.getString(4), resultSet.getBigDecimal(5), resultSet.getBigDecimal(6),
                                resultSet.getBigDecimal(7), resultSet.getBigDecimal(8), resultSet.getString(9),
                                resultSet.getString(10)));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(HEADER + " WHERE v.id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? readInvoice(resultSet, items) : null;
                }
            }
        }
    }

    private static Invoice readInvoice(ResultSet resultSet, List<Invoice.Item> items) throws SQLException {
        return new Invoice(resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3), resultSet.getString(4),
                resultSet.getString(5), resultSet.getString(6), resultSet.getString(7),
                resultSet.getObject(8, LocalDate.class), resultSet.getObject(9, LocalDate.class), resultSet.getInt(10),
                resultSet.getString(11), resultSet.getBigDecimal(12), resultSet.getBigDecimal(13),
                resultSet.getBigDecimal(14), resultSet.getBigDecimal(15), resultSet.getBigDecimal(16),
                resultSet.getBigDecimal(17), resultSet.getString(18), resultSet.getObject(19, LocalDateTime.class),
                resultSet.getString(20), items);
    }

    // Dòng phiếu xuất cùng thông tin dòng đơn để tính dòng hoá đơn theo số thực giao
    public record Source(long noteId, String deliveryNo, String noteStatus, long orderId, String orderNo,
                         long customerId, long noteItemId, long orderItemId, long productId, long unitId,
                         BigDecimal unitFactor, BigDecimal deliveredBase, BigDecimal orderQtyBase,
                         BigDecimal unitPrice, BigDecimal orderDiscount, BigDecimal orderLineTotal,
                         BigDecimal costPrice, boolean invoiced) {
    }
}
