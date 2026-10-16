package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.FileObjectDao;
import com.oms.dao.PaymentDao;
import com.oms.model.Debtor;
import com.oms.model.OpenInvoice;
import com.oms.model.Payment;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.FileStorage;
import com.oms.util.JsonUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
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
import java.util.UUID;
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
    public static final long RECEIPT_MAX_BYTES = 5L * 1024 * 1024;
    private static final Pattern MONEY = Pattern.compile("[0-9]{1,13}");
    private static final String AUDIT_ENTITY = "PAYMENT";
    private static final String NOT_FOUND = "NOT_FOUND";

    private final PaymentDao paymentDao = new PaymentDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();
    private final FileObjectDao fileObjectDao = new FileObjectDao();
    private final CreditFlagService creditFlagService = new CreditFlagService();

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

    public Payment find(long id) throws SQLException {
        return paymentDao.findPayment(id);
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
        creditFlagService.evaluate(connection, customerId, actorUserId, ipAddress);
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

    // Ảnh biên lai tải lên (S7-05)
    public record ReceiptUpload(byte[] content, String originalName) {
    }

    // S7-05: nhân viên kinh doanh ghi nhận khoản thu tại cửa hàng của đại lý mình phụ trách, kèm ảnh biên lai.
    // Khoản thu ở trạng thái Chờ nộp, chưa trừ công nợ cho tới khi kế toán xác nhận đã nhận tiền.
    public Outcome collect(long actorUserId, long customerId, String amountText, String method, String dateText,
                           String note, ReceiptUpload receipt, String ipAddress) throws SQLException, IOException {
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
        if (receipt == null || receipt.content().length == 0) {
            return Outcome.failure("Vui lòng chụp hoặc chọn ảnh biên lai.");
        }
        if (receipt.content().length > RECEIPT_MAX_BYTES) {
            return Outcome.failure("Ảnh biên lai tối đa 5MB.");
        }
        String type = DeliveryConfirmService.detectImageType(receipt.content());
        if (type == null) {
            return Outcome.failure("Ảnh biên lai phải là JPG hoặc PNG.");
        }
        if (!paymentDao.isAssignedTo(customerId, actorUserId)) {
            return Outcome.failure(NOT_FOUND);
        }
        String key = "receipts/" + DateTimeUtil.today().toString().substring(0, 7) + "/" + UUID.randomUUID()
                + ("image/png".equals(type) ? ".png" : ".jpg");
        FileStorage.save(key, receipt.content());
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!paymentDao.lockCustomer(connection, customerId)) {
                    connection.rollback();
                    Files.deleteIfExists(FileStorage.resolve(key));
                    return Outcome.failure(NOT_FOUND);
                }
                BigDecimal outstanding = paymentDao.findOutstanding(connection, customerId);
                BigDecimal pending = paymentDao.findPendingTotal(connection, customerId);
                if (amount.compareTo(outstanding.subtract(pending)) > 0) {
                    connection.rollback();
                    Files.deleteIfExists(FileStorage.resolve(key));
                    return Outcome.failure("Số tiền thu vượt công nợ còn lại của đại lý ("
                            + outstanding.subtract(pending).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
                            + " ₫, đã trừ các khoản đang chờ nộp).");
                }
                long fileId = fileObjectDao.insert(connection, key, fileName(receipt.originalName()), type,
                        receipt.content().length, "RECEIPT", actorUserId);
                String receiptNo = paymentDao.nextReceiptNo(connection, paymentDate.getYear());
                long paymentId = paymentDao.insertPending(connection, receiptNo, customerId, amount, method,
                        paymentDate, cleanNote, fileId, actorUserId);
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("receiptNo", receiptNo);
                values.put("customerId", customerId);
                values.put("amount", amount.toPlainString());
                values.put("method", method);
                values.put("paymentDate", paymentDate.toString());
                values.put("status", Payment.PENDING_HANDOVER);
                auditLogDao.insert(connection, actorUserId, "PAYMENT_COLLECT", AUDIT_ENTITY, paymentId, null,
                        JsonUtil.object(values), null, ipAddress);
                connection.commit();
                return Outcome.success(paymentId, receiptNo);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                Files.deleteIfExists(FileStorage.resolve(key));
                throw e;
            }
        }
    }

    // S7-05: kế toán xác nhận đã nhận tiền; khoản thu được đối trừ vào hoá đơn cũ nhất trước như thu trực tiếp
    public Outcome confirmHandover(long actorUserId, long paymentId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PaymentDao.PaymentLock payment = paymentDao.lockPayment(connection, paymentId);
                if (payment == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!Payment.PENDING_HANDOVER.equals(payment.status())) {
                    connection.rollback();
                    return Outcome.failure("Khoản thu " + payment.receiptNo() + " không còn ở trạng thái Chờ nộp.");
                }
                paymentDao.lockCustomer(connection, payment.customerId());
                List<Share> shares = allocate(payment.amount(),
                        paymentDao.lockOpenInvoices(connection, payment.customerId()));
                if (shares == null) {
                    connection.rollback();
                    return Outcome.failure("Công nợ còn lại của đại lý không đủ để đối trừ khoản thu "
                            + payment.receiptNo() + "; có thể hoá đơn đã được thu trực tiếp. Hãy huỷ khoản thu này.");
                }
                List<String> invoiceNos = new ArrayList<>();
                for (Share share : shares) {
                    paymentDao.allocate(connection, payment.id(), share.invoice().getId(), share.amount(),
                            actorUserId);
                    invoiceNos.add(share.invoice().getInvoiceNo());
                }
                paymentDao.insertLedgerEntry(connection, payment.customerId(), payment.amount(), payment.id(),
                        payment.paymentDate(), "Phiếu thu " + payment.receiptNo(), actorUserId);
                PaymentDao.adjustBalance(connection, payment.customerId(), payment.amount().negate());
                paymentDao.confirm(connection, payment.id(), actorUserId);
                creditFlagService.evaluate(connection, payment.customerId(), actorUserId, ipAddress);
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("status", Payment.PENDING_HANDOVER);
                Map<String, Object> after = new LinkedHashMap<>();
                after.put("receiptNo", payment.receiptNo());
                after.put("status", "CONFIRMED");
                after.put("amount", payment.amount().toPlainString());
                after.put("invoices", String.join(", ", invoiceNos));
                auditLogDao.insert(connection, actorUserId, "PAYMENT_CONFIRM", AUDIT_ENTITY, payment.id(),
                        JsonUtil.object(before), JsonUtil.object(after), null, ipAddress);
                connection.commit();
                return Outcome.success(payment.id(), payment.receiptNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Huỷ khoản thu chờ nộp kèm lý do (không nhận được tiền hoặc sai số liệu); không đụng công nợ
    public Outcome voidPending(long actorUserId, long paymentId, String reason, String ipAddress)
            throws SQLException {
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isEmpty()) {
            return Outcome.failure("Nhập lý do huỷ khoản thu.");
        }
        if (cleanReason.length() > NOTE_MAX) {
            return Outcome.failure("Lý do tối đa " + NOTE_MAX + " ký tự.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PaymentDao.PaymentLock payment = paymentDao.lockPayment(connection, paymentId);
                if (payment == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!Payment.PENDING_HANDOVER.equals(payment.status())) {
                    connection.rollback();
                    return Outcome.failure("Chỉ huỷ được khoản thu đang Chờ nộp.");
                }
                paymentDao.voidPending(connection, payment.id(), cleanReason, actorUserId);
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("status", Payment.PENDING_HANDOVER);
                Map<String, Object> after = new LinkedHashMap<>();
                after.put("receiptNo", payment.receiptNo());
                after.put("status", "VOIDED");
                after.put("amount", payment.amount().toPlainString());
                auditLogDao.insert(connection, actorUserId, "PAYMENT_VOID", AUDIT_ENTITY, payment.id(),
                        JsonUtil.object(before), JsonUtil.object(after), cleanReason, ipAddress);
                connection.commit();
                return Outcome.success(payment.id(), payment.receiptNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public List<Debtor> debtorsOf(long salesRepId) throws SQLException {
        return paymentDao.findDebtors(salesRepId);
    }

    public List<Payment> findByCollector(long collectorId) throws SQLException {
        return paymentDao.findPayments(collectorId, null, LIST_LIMIT);
    }

    public List<Payment> findPending() throws SQLException {
        return paymentDao.findPayments(null, Payment.PENDING_HANDOVER, LIST_LIMIT);
    }

    public List<PaymentDao.ReconciliationRow> reconciliation(LocalDate from, LocalDate to) throws SQLException {
        return paymentDao.findReconciliation(from, to);
    }

    public PaymentDao.Receipt findReceipt(long paymentId) throws SQLException {
        return paymentDao.findReceipt(paymentId);
    }

    // Tên tệp gốc do người dùng gửi lên chỉ để hiển thị: bỏ đường dẫn, giới hạn độ dài
    private static String fileName(String submitted) {
        String name = submitted == null ? "receipt" : submitted;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        name = slash >= 0 ? name.substring(slash + 1) : name;
        return name.isBlank() ? "receipt" : name.substring(0, Math.min(name.length(), 200));
    }
}
