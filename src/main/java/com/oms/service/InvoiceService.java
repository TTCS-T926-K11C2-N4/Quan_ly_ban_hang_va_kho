package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.InvoiceDao;
import com.oms.dao.PaymentDao;
import com.oms.dao.PermissionDao;
import com.oms.model.Invoice;
import com.oms.model.InvoiceCandidate;
import com.oms.model.Permission;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Phát hành hoá đơn bán hàng (S7-01).
//  * Hoá đơn sinh từ số lượng THỰC GIAO của phiếu xuất đã giao (không phải số đặt); giá và chiết khấu theo dòng đơn,
//    chiết khấu tính theo tỉ lệ số thực giao trên số đặt.
//  * Số hoá đơn cấp liên tục theo năm trong cùng transaction nên không trùng, không nhảy cóc.
//  * Gộp nhiều phiếu giao của cùng một đại lý thành một hoá đơn; một dòng phiếu chỉ vào một hoá đơn.
//  * Ghi Nợ vào sổ công nợ cùng transaction. Đã phát hành thì không sửa, không xoá (trigger CSDL).
public class InvoiceService {

    public static final int LIST_LIMIT = 100;
    public static final int NOTE_MAX = 1000;
    private static final String AUDIT_ENTITY = "INVOICE";
    private static final String NOT_FOUND = "NOT_FOUND";

    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();
    private final CreditFlagService creditFlagService = new CreditFlagService();

    // error != null: lỗi nghiệp vụ (hoặc "NOT_FOUND"); ngược lại id hoá đơn và số hoá đơn
    public static final class Outcome {
        private final String error;
        private final long id;
        private final String invoiceNo;

        private Outcome(String error, long id, String invoiceNo) {
            this.error = error;
            this.id = id;
            this.invoiceNo = invoiceNo;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0, null);
        }

        static Outcome success(long id, String invoiceNo) {
            return new Outcome(null, id, invoiceNo);
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

        public String getInvoiceNo() {
            return invoiceNo;
        }
    }

    // Hoá đơn chứa công nợ của nhiều đại lý nên chỉ người xem được mọi hoá đơn (INVOICE_VIEW phạm vi ALL)
    public boolean canView(long userId) throws SQLException {
        return "ALL".equals(permissionDao.findScope(userId, Permission.INVOICE_VIEW));
    }

    public List<InvoiceCandidate> candidates() throws SQLException {
        return invoiceDao.findCandidates();
    }

    public List<Invoice> findInvoices(Long customerId, String status, boolean overdueOnly) throws SQLException {
        return invoiceDao.findInvoices(customerId, status, overdueOnly, DateTimeUtil.today(), LIST_LIMIT);
    }

    public Invoice find(long id) throws SQLException {
        return invoiceDao.findDetail(id);
    }

    // Một dòng hoá đơn đã tính (không đọc CSDL nên test được)
    record Line(InvoiceDao.Source source, BigDecimal qty, BigDecimal discount, BigDecimal lineTotal) {
    }

    // Tính dòng hoá đơn từ dòng phiếu xuất: số lượng theo đơn vị đặt = số thực giao / hệ số; giao đủ số đặt thì lấy đúng
    // thành tiền dòng đơn, giao thiếu thì chiết khấu và thành tiền theo tỉ lệ (làm tròn tới đồng)
    static Line price(InvoiceDao.Source source) {
        BigDecimal qty = source.deliveredBase().divide(source.unitFactor(), 3, RoundingMode.HALF_UP);
        if (source.deliveredBase().compareTo(source.orderQtyBase()) >= 0) {
            return new Line(source, qty, source.orderDiscount(), source.orderLineTotal());
        }
        BigDecimal ratio = source.deliveredBase().divide(source.orderQtyBase(), 10, RoundingMode.HALF_UP);
        BigDecimal discount = source.orderDiscount().multiply(ratio).setScale(0, RoundingMode.HALF_UP).setScale(2);
        BigDecimal gross = source.unitPrice().multiply(qty).setScale(0, RoundingMode.HALF_UP).setScale(2);
        return new Line(source, qty, discount, gross.subtract(discount));
    }

    public Outcome issue(long actorUserId, List<Long> noteIds, String note, String ipAddress) throws SQLException {
        Set<Long> ids = new LinkedHashSet<>(noteIds);
        if (ids.isEmpty()) {
            return Outcome.failure("Chọn ít nhất một phiếu xuất đã giao để lập hoá đơn.");
        }
        String cleanNote = note == null || note.trim().isEmpty() ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > NOTE_MAX) {
            return Outcome.failure("Ghi chú tối đa " + NOTE_MAX + " ký tự.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Outcome outcome = issueInTransaction(connection, actorUserId, ids, cleanNote, ipAddress);
                if (outcome.getError() != null) {
                    connection.rollback();
                } else {
                    connection.commit();
                }
                return outcome;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private Outcome issueInTransaction(Connection connection, long actorUserId, Set<Long> ids, String note,
                                       String ipAddress) throws SQLException {
        List<InvoiceDao.Source> sources = invoiceDao.lockSources(connection, ids);
        Set<Long> found = new LinkedHashSet<>();
        Set<Long> customers = new LinkedHashSet<>();
        for (InvoiceDao.Source source : sources) {
            found.add(source.noteId());
            customers.add(source.customerId());
            if (!"DELIVERED".equals(source.noteStatus())) {
                return Outcome.failure("Phiếu " + source.deliveryNo() + " chưa giao thành công nên chưa lập hoá đơn"
                        + " được.");
            }
        }
        if (!found.containsAll(ids)) {
            return Outcome.failure(NOT_FOUND);
        }
        if (customers.size() != 1) {
            return Outcome.failure("Một hoá đơn chỉ gộp các phiếu giao của cùng một đại lý.");
        }
        long customerId = customers.iterator().next();
        List<Line> lines = new ArrayList<>();
        Set<Long> invoicedNotes = new LinkedHashSet<>();
        for (InvoiceDao.Source source : sources) {
            if (source.invoiced()) {
                invoicedNotes.add(source.noteId());
            } else if (source.deliveredBase().signum() > 0) {
                lines.add(price(source));
            }
        }
        if (!invoicedNotes.isEmpty() && lines.isEmpty()) {
            return Outcome.failure("Các phiếu đã chọn đã được lập hoá đơn.");
        }
        if (lines.isEmpty()) {
            return Outcome.failure("Các phiếu đã chọn không có số lượng thực giao để lập hoá đơn.");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Line line : lines) {
            BigDecimal gross = line.lineTotal().add(line.discount());
            subtotal = subtotal.add(gross);
            discount = discount.add(line.discount());
            total = total.add(line.lineTotal());
        }
        if (total.signum() <= 0) {
            return Outcome.failure("Hoá đơn có tổng tiền bằng 0 nên không lập được.");
        }
        int termDays = invoiceDao.lockCustomerTerm(connection, customerId);
        if (termDays < 0) {
            return Outcome.failure(NOT_FOUND);
        }
        LocalDate today = DateTimeUtil.today();
        LocalDate due = today.plusDays(termDays);
        String invoiceNo = invoiceDao.nextInvoiceNo(connection, today.getYear());
        long invoiceId = invoiceDao.insertInvoice(connection, invoiceNo, customerId, today, due, termDays, subtotal,
                discount, total, note, actorUserId);
        for (Line line : lines) {
            invoiceDao.insertItem(connection, invoiceId, line.source(), line.qty(), line.discount(), line.lineTotal());
        }
        invoiceDao.insertLedgerEntry(connection, customerId, total, invoiceId, today, "Hoá đơn " + invoiceNo,
                actorUserId);
        PaymentDao.adjustBalance(connection, customerId, total);
        creditFlagService.evaluate(connection, customerId, actorUserId, ipAddress);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("invoiceNo", invoiceNo);
        values.put("customerId", customerId);
        values.put("deliveryNotes", found.size());
        values.put("lines", lines.size());
        values.put("total", total.stripTrailingZeros().toPlainString());
        auditLogDao.insert(connection, actorUserId, "INVOICE_ISSUE", AUDIT_ENTITY, invoiceId, null,
                JsonUtil.object(values), null, ipAddress);
        return Outcome.success(invoiceId, invoiceNo);
    }
}
