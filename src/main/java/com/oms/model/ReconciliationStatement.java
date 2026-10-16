package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Biên bản đối chiếu công nợ định kỳ với đại lý (S7-06): số dư đầu kỳ, phát sinh, số dư cuối kỳ chụp lúc lập
public class ReconciliationStatement {

    public static final String DRAFT = "DRAFT";
    public static final String SENT = "SENT";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String DISPUTED = "DISPUTED";

    private static final Map<String, String> LABELS = Map.of(DRAFT, "Nháp", SENT, "Đã gửi đại lý, chờ xác nhận",
            CONFIRMED, "Đại lý đã xác nhận", DISPUTED, "Đại lý báo lệch");
    private static final Map<String, String> TONES = Map.of(DRAFT, "info", SENT, "warning", CONFIRMED, "success",
            DISPUTED, "danger");

    private final long id;
    private final String statementNo;
    private final long customerId;
    private final String customerCode;
    private final String customerName;
    private final LocalDate periodFrom;
    private final LocalDate periodTo;
    private final BigDecimal openingBalance;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private final BigDecimal closingBalance;
    private final String status;
    private final String responseNote;
    private final String respondedByName;
    private final LocalDateTime respondedAt;
    private final LocalDateTime createdAt;
    private final List<Entry> entries;

    public ReconciliationStatement(long id, String statementNo, long customerId, String customerCode,
                                   String customerName, LocalDate periodFrom, LocalDate periodTo,
                                   BigDecimal openingBalance, BigDecimal totalDebit, BigDecimal totalCredit,
                                   BigDecimal closingBalance, String status, String responseNote,
                                   String respondedByName, LocalDateTime respondedAt, LocalDateTime createdAt,
                                   List<Entry> entries) {
        this.id = id;
        this.statementNo = statementNo;
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.openingBalance = openingBalance;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.closingBalance = closingBalance;
        this.status = status;
        this.responseNote = responseNote;
        this.respondedByName = respondedByName;
        this.respondedAt = respondedAt;
        this.createdAt = createdAt;
        this.entries = entries;
    }

    public long getId() {
        return id;
    }

    public String getStatementNo() {
        return statementNo;
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

    public LocalDate getPeriodFrom() {
        return periodFrom;
    }

    public LocalDate getPeriodTo() {
        return periodTo;
    }

    // Cùng biên bản kèm danh sách bút toán trong kỳ
    public ReconciliationStatement withEntries(List<Entry> newEntries) {
        return new ReconciliationStatement(id, statementNo, customerId, customerCode, customerName, periodFrom,
                periodTo, openingBalance, totalDebit, totalCredit, closingBalance, status, responseNote,
                respondedByName, respondedAt, createdAt, newEntries);
    }

    public String getPeriodText() {
        return DateTimeUtil.formatDay(periodFrom) + " – " + DateTimeUtil.formatDay(periodTo);
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return LABELS.getOrDefault(status, status);
    }

    public String getStatusTone() {
        return TONES.getOrDefault(status, "info");
    }

    // Đại lý chỉ xác nhận hoặc báo lệch khi biên bản đã gửi
    public boolean isAwaitingResponse() {
        return SENT.equals(status);
    }

    public boolean isDraft() {
        return DRAFT.equals(status);
    }

    public String getResponseNote() {
        return responseNote;
    }

    public String getRespondedByName() {
        return respondedByName;
    }

    public String getRespondedAtText() {
        return DateTimeUtil.formatDateTime(respondedAt);
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    // Một bút toán sổ công nợ trong kỳ
    public static class Entry {
        private final LocalDate date;
        private final String type;
        private final String note;
        private final BigDecimal debit;
        private final BigDecimal credit;

        public Entry(LocalDate date, String type, String note, BigDecimal debit, BigDecimal credit) {
            this.date = date;
            this.type = type;
            this.note = note;
            this.debit = debit;
            this.credit = credit;
        }

        public String getDateText() {
            return DateTimeUtil.formatDay(date);
        }

        public String getTypeLabel() {
            return switch (type) {
                case "INVOICE" -> "Hoá đơn";
                case "PAYMENT" -> "Thanh toán";
                case "CREDIT_NOTE" -> "Giảm trừ";
                case "OPENING_BALANCE" -> "Số dư đầu kỳ";
                default -> "Điều chỉnh";
            };
        }

        public String getNote() {
            return note;
        }

        public BigDecimal getDebit() {
            return debit;
        }

        public BigDecimal getCredit() {
            return credit;
        }
    }
}
