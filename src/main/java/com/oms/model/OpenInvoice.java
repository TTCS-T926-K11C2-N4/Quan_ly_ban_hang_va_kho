package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

// Hoá đơn còn nợ của một đại lý, dùng cho màn ghi nhận thanh toán (S7-02) và đối trừ cũ nhất trước
public class OpenInvoice {

    private final long id;
    private final String invoiceNo;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final String status;
    private final BigDecimal total;
    private final BigDecimal outstanding;

    public OpenInvoice(long id, String invoiceNo, LocalDate issueDate, LocalDate dueDate, String status,
                       BigDecimal total, BigDecimal outstanding) {
        this.id = id;
        this.invoiceNo = invoiceNo;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = status;
        this.total = total;
        this.outstanding = outstanding;
    }

    public long getId() {
        return id;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getIssueDateText() {
        return DateTimeUtil.formatDay(issueDate);
    }

    public String getDueDateText() {
        return DateTimeUtil.formatDay(dueDate);
    }

    public boolean isOverdue() {
        return dueDate.isBefore(DateTimeUtil.today());
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }
}
