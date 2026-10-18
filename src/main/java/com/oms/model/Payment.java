package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Phiếu thu tiền của đại lý và các hoá đơn được đối trừ (S7-02)
public class Payment {

    public static final String CASH = "CASH";
    public static final String BANK_TRANSFER = "BANK_TRANSFER";
    private static final Map<String, String> METHODS = Map.of(CASH, "Tiền mặt", BANK_TRANSFER, "Chuyển khoản");
    private static final Map<String, String> STATUSES = Map.of("PENDING_HANDOVER", "Chờ nộp tiền",
            "CONFIRMED", "Đã ghi nhận", "VOIDED", "Đã huỷ");

    private final long id;
    private final String receiptNo;
    private final long customerId;
    private final String customerCode;
    private final String customerName;
    private final BigDecimal amount;
    private final String method;
    private final LocalDate paymentDate;
    private final String collectedByName;
    private final String status;
    private final String note;
    private final LocalDateTime createdAt;
    private final List<Allocation> allocations;
    private final boolean receiptAttached;

    public Payment(long id, String receiptNo, long customerId, String customerCode, String customerName,
                   BigDecimal amount, String method, LocalDate paymentDate, String collectedByName, String status,
                   String note, LocalDateTime createdAt, List<Allocation> allocations, boolean receiptAttached) {
        this.receiptAttached = receiptAttached;
        this.id = id;
        this.receiptNo = receiptNo;
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.amount = amount;
        this.method = method;
        this.paymentDate = paymentDate;
        this.collectedByName = collectedByName;
        this.status = status;
        this.note = note;
        this.createdAt = createdAt;
        this.allocations = allocations;
    }

    public static final String PENDING_HANDOVER = "PENDING_HANDOVER";

    public static boolean isMethod(String value) {
        return value != null && METHODS.containsKey(value);
    }

    public long getId() {
        return id;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public long getCustomerId() {
        return customerId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMethodLabel() {
        return METHODS.getOrDefault(method, method);
    }

    public String getPaymentDateText() {
        return DateTimeUtil.formatDay(paymentDate);
    }

    public String getCollectedByName() {
        return collectedByName;
    }

    // Khoản thu theo tuyến chờ kế toán xác nhận đã nhận tiền (S7-05)
    public boolean isPending() {
        return PENDING_HANDOVER.equals(status);
    }

    public boolean isReceiptAttached() {
        return receiptAttached;
    }

    public String getStatusLabel() {
        return STATUSES.getOrDefault(status, status);
    }

    public String getNote() {
        return note;
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public List<Allocation> getAllocations() {
        return allocations;
    }

    public static class Allocation {
        private final long invoiceId;
        private final String invoiceNo;
        private final BigDecimal amount;

        public Allocation(long invoiceId, String invoiceNo, BigDecimal amount) {
            this.invoiceId = invoiceId;
            this.invoiceNo = invoiceNo;
            this.amount = amount;
        }

        public long getInvoiceId() {
            return invoiceId;
        }

        public String getInvoiceNo() {
            return invoiceNo;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
