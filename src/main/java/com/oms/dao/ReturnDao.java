package com.oms.dao;

import com.oms.model.ReturnLine;
import com.oms.model.SalesReturn;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Phiếu trả hàng (S8-01). Các hàm nhận Connection chạy trong transaction của ReturnService.
public class ReturnDao {

    private static final String DOC_TYPE = "RETURN";
    private static final String NO_PREFIX = "TH";
    private static final String HEADER = "SELECT r.id, r.return_no, r.invoice_id, v.invoice_no, r.customer_id,"
            + " c.name, w.name, r.reason_code, r.reason_note, r.status, cu.full_name, r.created_at, ru.full_name,"
            + " r.confirmed_at FROM sales_returns r JOIN invoices v ON v.id = r.invoice_id"
            + " JOIN customers c ON c.id = r.customer_id JOIN warehouses w ON w.id = r.warehouse_id"
            + " LEFT JOIN users cu ON cu.id = r.created_by LEFT JOIN users ru ON ru.id = r.confirmed_by";

    // Hoá đơn còn dòng trả được, mới nhất trước; salesRepId: chỉ đại lý nhân viên đó phụ trách (null: tất cả)
    public List<InvoiceOption> findInvoiceOptions(Long salesRepId, int limit) throws SQLException {
        String sql = "SELECT v.id, v.invoice_no, c.name, v.issue_date FROM invoices v"
                + " JOIN customers c ON c.id = v.customer_id WHERE v.status <> 'CANCELLED'"
                + " AND (? IS NULL OR c.sales_rep_id = ?) ORDER BY v.id DESC LIMIT ?";
        List<InvoiceOption> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (salesRepId == null) {
                statement.setNull(1, Types.BIGINT);
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(1, salesRepId);
                statement.setLong(2, salesRepId);
            }
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new InvoiceOption(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(3), resultSet.getObject(4, java.time.LocalDate.class).toString()));
                }
            }
        }
        return result;
    }

    // Khoá hoá đơn (xếp hàng các lần trả cùng hoá đơn); null nếu không có hoặc ngoài phạm vi nhân viên kinh doanh
    public InvoiceLock lockInvoice(Connection connection, long invoiceId, Long salesRepId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT v.id, v.invoice_no, v.customer_id, v.status, c.sales_rep_id FROM invoices v"
                        + " JOIN customers c ON c.id = v.customer_id WHERE v.id = ? FOR UPDATE")) {
            statement.setLong(1, invoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                Long rep = resultSet.getObject(5, Long.class);
                if (salesRepId != null && !salesRepId.equals(rep)) {
                    return null;
                }
                return new InvoiceLock(resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3),
                        resultSet.getString(4));
            }
        }
    }

    // Dòng hoá đơn kèm số đã trả (phiếu chưa huỷ) để tính số còn trả được
    public List<ReturnLine> findLines(Connection connection, long invoiceId) throws SQLException {
        String sql = "SELECT i.id, i.product_id, p.sku, p.name, u.name, l.lot_no, n.lot_id, d.warehouse_id,"
                + " i.unit_factor, i.qty, i.qty_base, COALESCE((SELECT SUM(s.qty_base) FROM sales_return_items s"
                + " JOIN sales_returns r ON r.id = s.return_id WHERE s.invoice_item_id = i.id"
                + " AND r.status <> 'CANCELLED'), 0), i.unit_price FROM invoice_items i"
                + " JOIN products p ON p.id = i.product_id JOIN units u ON u.id = i.unit_id"
                + " JOIN delivery_note_items n ON n.id = i.delivery_note_item_id"
                + " JOIN delivery_notes d ON d.id = n.delivery_note_id JOIN lots l ON l.id = n.lot_id"
                + " WHERE i.invoice_id = ? ORDER BY i.id";
        List<ReturnLine> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, invoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lines.add(new ReturnLine(resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3),
                            resultSet.getString(4), resultSet.getString(5), resultSet.getString(6),
                            resultSet.getLong(7), resultSet.getLong(8), resultSet.getBigDecimal(9),
                            resultSet.getBigDecimal(10), resultSet.getBigDecimal(11), resultSet.getBigDecimal(12),
                            resultSet.getBigDecimal(13)));
                }
            }
        }
        return lines;
    }

    // Kho hàng lỗi đầu tiên đang hoạt động; null nếu chưa có
    public Long findDefectWarehouseId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM warehouses WHERE warehouse_type = 'DEFECT' AND status = 'ACTIVE' ORDER BY id LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : null;
        }
    }

    public String nextReturnNo(Connection connection, int year) throws SQLException {
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

    public long insert(Connection connection, String returnNo, long invoiceId, long customerId, long warehouseId,
                       String reasonCode, String reasonNote, long actorUserId) throws SQLException {
        String sql = "INSERT INTO sales_returns (return_no, invoice_id, customer_id, warehouse_id, reason_code,"
                + " reason_note, status, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?,"
                + " 'SUBMITTED', UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, returnNo);
            statement.setLong(2, invoiceId);
            statement.setLong(3, customerId);
            statement.setLong(4, warehouseId);
            statement.setString(5, reasonCode);
            if (reasonNote == null) {
                statement.setNull(6, Types.VARCHAR);
            } else {
                statement.setString(6, reasonNote);
            }
            statement.setLong(7, actorUserId);
            statement.setLong(8, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertItem(Connection connection, long returnId, ReturnLine line, BigDecimal qty, BigDecimal qtyBase,
                           String condition, long toWarehouseId) throws SQLException {
        String sql = "INSERT INTO sales_return_items (return_id, invoice_item_id, product_id, lot_id, unit_id,"
                + " unit_factor, qty, qty_base, `condition`, to_warehouse_id) VALUES (?, ?, ?, ?,"
                + " (SELECT unit_id FROM invoice_items WHERE id = ?), ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, returnId);
            statement.setLong(2, line.getInvoiceItemId());
            statement.setLong(3, line.getProductId());
            statement.setLong(4, line.getLotId());
            statement.setLong(5, line.getInvoiceItemId());
            statement.setBigDecimal(6, line.getUnitFactor());
            statement.setBigDecimal(7, qty);
            statement.setBigDecimal(8, qtyBase);
            statement.setString(9, condition);
            statement.setLong(10, toWarehouseId);
            statement.executeUpdate();
        }
    }

    // Mới nhất trước; salesRepId: chỉ đại lý nhân viên đó phụ trách; status null: tất cả
    public List<SalesReturn> findAll(Long salesRepId, String status, int limit) throws SQLException {
        String sql = HEADER + " WHERE (? IS NULL OR c.sales_rep_id = ?) AND (? IS NULL OR r.status = ?)"
                + " ORDER BY r.id DESC LIMIT ?";
        List<SalesReturn> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (salesRepId == null) {
                statement.setNull(1, Types.BIGINT);
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(1, salesRepId);
                statement.setLong(2, salesRepId);
            }
            statement.setString(3, status);
            statement.setString(4, status);
            statement.setInt(5, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(read(resultSet, List.of()));
                }
            }
        }
        return result;
    }

    // Kèm dòng trả; null nếu không có hoặc ngoài phạm vi nhân viên kinh doanh
    public SalesReturn find(long id, Long salesRepId) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            SalesReturn header;
            try (PreparedStatement statement = connection.prepareStatement(
                    HEADER + " WHERE r.id = ? AND (? IS NULL OR c.sales_rep_id = ?)")) {
                statement.setLong(1, id);
                if (salesRepId == null) {
                    statement.setNull(2, Types.BIGINT);
                    statement.setNull(3, Types.BIGINT);
                } else {
                    statement.setLong(2, salesRepId);
                    statement.setLong(3, salesRepId);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    header = read(resultSet, List.of());
                }
            }
            List<SalesReturn.Item> items = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT p.sku, p.name, u.name, l.lot_no, s.qty, s.`condition`, w.name FROM sales_return_items s"
                            + " JOIN products p ON p.id = s.product_id JOIN units u ON u.id = s.unit_id"
                            + " JOIN lots l ON l.id = s.lot_id JOIN warehouses w ON w.id = s.to_warehouse_id"
                            + " WHERE s.return_id = ? ORDER BY s.id")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        items.add(new SalesReturn.Item(resultSet.getString(1), resultSet.getString(2),
                                resultSet.getString(3), resultSet.getString(4), resultSet.getBigDecimal(5),
                                resultSet.getString(6), resultSet.getString(7)));
                    }
                }
            }
            return header.withItems(items);
        }
    }

    // Khoá phiếu trả; null nếu không có
    public ReturnLock lock(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, return_no, status, customer_id FROM sales_returns WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new ReturnLock(resultSet.getLong(1), resultSet.getString(2),
                        resultSet.getString(3), resultSet.getLong(4)) : null;
            }
        }
    }

    public List<ReceiveItem> findReceiveItems(Connection connection, long returnId) throws SQLException {
        List<ReceiveItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT product_id, lot_id, to_warehouse_id, qty_base FROM sales_return_items WHERE return_id = ?"
                        + " ORDER BY id")) {
            statement.setLong(1, returnId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new ReceiveItem(resultSet.getLong(1), resultSet.getLong(2), resultSet.getLong(3),
                            resultSet.getBigDecimal(4)));
                }
            }
        }
        return items;
    }

    public void markReceived(Connection connection, long id, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE sales_returns SET status = 'RECEIVED', confirmed_by = ?, confirmed_at = UTC_TIMESTAMP(),"
                        + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    public void markCancelled(Connection connection, long id, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE sales_returns SET status = 'CANCELLED', updated_at = UTC_TIMESTAMP(), updated_by = ?"
                        + " WHERE id = ?")) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    // Thẻ kho: RETURN_IN (dương) ở kho nhận
    public void insertMovement(Connection connection, long warehouseId, long productId, long lotId,
                               BigDecimal change, BigDecimal balanceAfter, long returnId, String note,
                               long actorUserId) throws SQLException {
        String sql = "INSERT INTO stock_movements (warehouse_id, product_id, lot_id, movement_type, qty_change_base,"
                + " balance_after_base, ref_type, ref_id, occurred_at, note, created_at, created_by)"
                + " VALUES (?, ?, ?, 'RETURN_IN', ?, ?, 'SALES_RETURN', ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            statement.setLong(3, lotId);
            statement.setBigDecimal(4, change);
            statement.setBigDecimal(5, balanceAfter);
            statement.setLong(6, returnId);
            statement.setString(7, note);
            statement.setLong(8, actorUserId);
            statement.executeUpdate();
        }
    }

    private static SalesReturn read(ResultSet resultSet, List<SalesReturn.Item> items) throws SQLException {
        return new SalesReturn(resultSet.getLong(1), resultSet.getString(2), resultSet.getLong(3),
                resultSet.getString(4), resultSet.getLong(5), resultSet.getString(6), resultSet.getString(7),
                resultSet.getString(8), resultSet.getString(9), resultSet.getString(10), resultSet.getString(11),
                resultSet.getObject(12, LocalDateTime.class), resultSet.getString(13),
                resultSet.getObject(14, LocalDateTime.class), items);
    }

    public record InvoiceOption(long id, String invoiceNo, String customerName, String issueDate) {
        // EL không đọc được record: các getter dưới đây cho JSP
        public long getId() {
            return id;
        }

        public String getInvoiceNo() {
            return invoiceNo;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getIssueDate() {
            return issueDate;
        }
    }

    public record InvoiceLock(long id, String invoiceNo, long customerId, String status) {
    }

    public record ReturnLock(long id, String returnNo, String status, long customerId) {
    }

    public record ReceiveItem(long productId, long lotId, long toWarehouseId, BigDecimal qtyBase) {
    }
}
