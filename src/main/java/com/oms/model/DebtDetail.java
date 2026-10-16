package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Sổ chi tiết công nợ của một đại lý trong một khoảng thời gian (S8-10): số dư đầu kỳ, từng bút toán tăng giảm kèm số dư
// luỹ kế, tổng phát sinh và số dư cuối kỳ
public class DebtDetail {

    private final long customerId;
    private final String customerCode;
    private final String customerName;
    private final LocalDate from;
    private final LocalDate to;
    private final BigDecimal opening;
    private final List<Row> rows;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private final BigDecimal closing;

    public DebtDetail(long customerId, String customerCode, String customerName, LocalDate from, LocalDate to,
                      BigDecimal opening, List<Row> rows, BigDecimal totalDebit, BigDecimal totalCredit,
                      BigDecimal closing) {
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.from = from;
        this.to = to;
        this.opening = opening;
        this.rows = rows;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.closing = closing;
    }

    // Dựng sổ từ số dư đầu kỳ và các bút toán theo thứ tự thời gian: số dư luỹ kế = số dư trước + Nợ - Có
    public static DebtDetail build(long customerId, String code, String name, LocalDate from, LocalDate to,
                                   BigDecimal opening, List<Entry> entries) {
        List<Row> rows = new ArrayList<>();
        BigDecimal balance = opening;
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (Entry entry : entries) {
            balance = balance.add(entry.debit()).subtract(entry.credit());
            debit = debit.add(entry.debit());
            credit = credit.add(entry.credit());
            rows.add(new Row(entry, balance));
        }
        return new DebtDetail(customerId, code, name, from, to, opening, rows, debit, credit, balance);
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

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public String getPeriodText() {
        return DateTimeUtil.formatDay(from) + " – " + DateTimeUtil.formatDay(to);
    }

    public BigDecimal getOpening() {
        return opening;
    }

    public List<Row> getRows() {
        return rows;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public BigDecimal getClosing() {
        return closing;
    }

    // Bút toán thô từ sổ công nợ
    public record Entry(LocalDate date, String type, String note, BigDecimal debit, BigDecimal credit) {
    }

    // Một dòng sổ: bút toán và số dư luỹ kế sau bút toán
    public static class Row {
        private final Entry entry;
        private final BigDecimal balance;

        public Row(Entry entry, BigDecimal balance) {
            this.entry = entry;
            this.balance = balance;
        }

        public String getDateText() {
            return DateTimeUtil.formatDay(entry.date());
        }

        public String getTypeLabel() {
            return switch (entry.type()) {
                case "INVOICE" -> "Hoá đơn";
                case "PAYMENT" -> "Thanh toán";
                case "CREDIT_NOTE" -> "Giảm trừ";
                case "OPENING_BALANCE" -> "Số dư đầu kỳ";
                default -> "Điều chỉnh";
            };
        }

        public String getNote() {
            return entry.note();
        }

        public BigDecimal getDebit() {
            return entry.debit();
        }

        public BigDecimal getCredit() {
            return entry.credit();
        }

        public BigDecimal getBalance() {
            return balance;
        }
    }
}
