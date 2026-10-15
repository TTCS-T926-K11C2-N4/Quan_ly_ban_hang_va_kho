package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.PaymentDao;
import com.oms.model.Debtor;
import com.oms.model.OpenInvoice;
import com.oms.model.Payment;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

// Ghi nhận thanh toán và đối trừ công nợ (S7-02).
//  * Chỉ định hoá đơn thì đối trừ vào các hoá đơn đó, không chỉ định thì theo nguyên tắc hoá đơn cũ nhất trước
//    (hạn thanh toán, ngày lập, id); thu một phần thì hoá đơn sang "Thanh toán một phần".
//  * Không thu vượt tổng còn nợ của các hoá đơn được đối trừ.
//  * Ghi phiếu thu, dòng đối trừ, cập nhật hoá đơn, sổ công nợ, số dư và nhật ký trong MỘT transaction. Đã ghi nhận
//    thì không xoá, không sửa (trigger CSDL).
public class PaymentService {

    public static final int LIST_LIMIT = 100;
    public static final int NOTE_MAX = 1000;
    private static final Pattern MONEY = Pattern.compile("[0-9]{1,13}");
    private static final String AUDIT_ENTITY = "PAYMENT";
    private static final String NOT_FOUND = "NOT_FOUND";

    private final PaymentDao paymentDao = new PaymentDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // error != null: lỗi nghiệp vụ (hoặc "NOT_FOUND"); ngược lại id và số phiếu thu
    public static final class Outcome {
        private final String error;
        private final long id;
        private final String receiptNo;

        private Outcome(String error, long id, String receiptNo) {
            this.error = error;
            this.id = id;
            this.receiptNo = receiptNo;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0, null);
        }

        static Outcome success(long id, String receiptNo) {
            return new Outcome(null, id, receiptNo);
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

        public String getReceiptNo() {
            return receiptNo;
        }
    }

    // Phần tiền đối trừ vào một hoá đơn
    record Share(OpenInvoice invoice, BigDecimal amount) {
    }

    public List<Debtor> debtors() throws SQLException {
        return paymentDao.findDebtors();
    }

    public String customerName(long customerId) throws SQLException {
        return paymentDao.findCustomerName(customerId);
    }

    public List<OpenInvoice> openInvoices(long customerId) throws SQLException {
        return paymentDao.findOpenInvoices(customerId);
    }

    public List<Payment> findPayments(Long customerId) throws SQLException {
        return paymentDao.findPayments(customerId, LIST_LIMIT);
    }

    // Chia amount lần lượt vào các hoá đơn theo thứ tự đã cho, mỗi hoá đơn tối đa số còn nợ; null nếu amount lớn hơn
    // tổng còn nợ (thu vượt)
    static List<Share> allocate(BigDecimal amount, List<OpenInvoice> ordered) {
        List<Share> shares = new ArrayList<>();
        BigDecimal left = amount;
        for (OpenInvoice invoice : ordered) {
            if (left.signum() == 0) {
                break;
            }
            BigDecimal part = left.min(invoice.getOutstanding());
            if (part.signum() > 0) {
                shares.add(new Share(invoice, part));
                left = left.subtract(part);
            }
        }
        return left.signum() == 0 ? shares : null;
    }

    // null nếu sai định dạng hoặc bằng 0; chấp nhận dấu chấm ngăn cách hàng nghìn
    static BigDecimal parseAmount(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.trim().replace(".", "").replace(",", "");
        if (!MONEY.matcher(digits).matches()) {
            return null;
        }
        BigDecimal value = new BigDecimal(digits);
        return value.signum() > 0 ? value : null;
    }

    public Outcome record(long actorUserId, long customerId, String amountText, String method, String dateText,
                          String note, List<Long> invoiceIds, String ipAddress) throws SQLException {
        BigDecimal amount = parseAmount(amountText);
        if (amount == null) {
            return Outcome.failure("Số tiền thu phải là số nguyên đồng lớn hơn 0.");
        }
        if (!Payment.isMethod(method)) {
            return Outcome.failure("Chọn hình thức thu (tiền mặt hoặc chuyển khoản).");
        }
        LocalDate paymentDate;
        try {
            paymentDate = dateText == null || dateText.isBlank() ? DateTimeUtil.today() : LocalDate.parse(dateText.trim());
        } catch (DateTimeParseException e) {
            return Outcome.failure("Ngày thu không hợp lệ.");
        }
        if (paymentDate.isAfter(DateTimeUtil.today())) {
            return Outcome.failure("Ngày thu không được sau hôm nay.");
        }
        String cleanNote = note == null || note.trim().isEmpty() ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > NOTE_MAX) {
            return Outcome.failure("Ghi chú tối đa " + NOTE_MAX + " ký tự.");
        }
        Set<Long> chosen = new LinkedHashSet<>(invoiceIds);
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Outcome outcome = recordInTransaction(connection, actorUserId, customerId, amount, method,
                        paymentDate, cleanNote, chosen, ipAddress);
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

    private Outcome recordInTransaction(Connection connection, long actorUserId, long customerId, BigDecimal amount,
                                        String method, LocalDate paymentDate, String note, Set<Long> chosen,
                                        String ipAddress) throws SQLException {
        if (!paymentDao.lockCustomer(connection, customerId)) {
            return Outcome.failure(NOT_FOUND);
        }
        List<OpenInvoice> open = paymentDao.lockOpenInvoices(connection, customerId);
        List<OpenInvoice> targets = new ArrayList<>();
        if (chosen.isEmpty()) {
            targets.addAll(open);
        } else {
            for (OpenInvoice invoice : open) {
                if (chosen.contains(invoice.getId())) {
                    targets.add(invoice);
                }
            }
            if (targets.size() != chosen.size()) {
                return Outcome.failure("Có hoá đơn đã chọn không thuộc đại lý này hoặc không còn nợ.");
            }
        }
        if (targets.isEmpty()) {
            return Outcome.failure("Đại lý này không còn hoá đơn nào còn nợ.");
        }
        List<Share> shares = allocate(amount, targets);
        if (shares == null) {
            BigDecimal remaining = BigDecimal.ZERO;
            for (OpenInvoice invoice : targets) {
                remaining = remaining.add(invoice.getOutstanding());
            }
            return Outcome.failure("Số tiền thu vượt tổng công nợ còn lại ("
                    + remaining.stripTrailingZeros().toPlainString() + " ₫).");
        }
        String receiptNo = paymentDao.nextReceiptNo(connection, paymentDate.getYear());
        long paymentId = paymentDao.insertPayment(connection, receiptNo, customerId, amount, method, paymentDate,
                note, actorUserId);
        List<String> invoiceNos = new ArrayList<>();
        for (Share share : shares) {
            paymentDao.allocate(connection, paymentId, share.invoice().getId(), share.amount(), actorUserId);
            invoiceNos.add(share.invoice().getInvoiceNo());
        }
        paymentDao.insertLedgerEntry(connection, customerId, amount, paymentId, paymentDate,
                "Phiếu thu " + receiptNo, actorUserId);
        PaymentDao.adjustBalance(connection, customerId, amount.negate());
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("receiptNo", receiptNo);
        values.put("customerId", customerId);
        values.put("amount", amount.toPlainString());
        values.put("method", method);
        values.put("paymentDate", paymentDate.toString());
        values.put("invoices", String.join(", ", invoiceNos));
        values.put("mode", chosen.isEmpty() ? "FIFO" : "SELECTED");
        auditLogDao.insert(connection, actorUserId, "PAYMENT_RECORD", AUDIT_ENTITY, paymentId, null,
                JsonUtil.object(values), null, ipAddress);
        return Outcome.success(paymentId, receiptNo);
    }
}
