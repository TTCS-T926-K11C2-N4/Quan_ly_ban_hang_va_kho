package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Một đơn trong danh sách chờ duyệt (S4-05 AC1): lý do cần duyệt và mức vi phạm để quản lý xem nhanh
public class ApprovalQueueRow {

    private final long orderId;
    private final String orderNo;
    private final String customerName;
    private final String customerCode;
    private final String salesRepName;
    private final BigDecimal total;
    private final LocalDateTime requestedAt;
    private final BigDecimal exceededBy;
    private final int belowFloorLines;
    private final boolean overdueDebt;

    public ApprovalQueueRow(long orderId, String orderNo, String customerName, String customerCode,
                            String salesRepName, BigDecimal total, LocalDateTime requestedAt, BigDecimal exceededBy,
                            int belowFloorLines, boolean overdueDebt) {
        this.overdueDebt = overdueDebt;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.salesRepName = salesRepName;
        this.total = total;
        this.requestedAt = requestedAt;
        this.exceededBy = exceededBy;
        this.belowFloorLines = belowFloorLines;
    }

    // S7-04: đơn chờ duyệt vì đại lý đang có nợ quá hạn
    public boolean isOverdueDebt() {
        return overdueDebt;
    }

    public boolean isOverCreditLimit() {
        return exceededBy != null && exceededBy.signum() > 0;
    }

    public String getRequestedAtText() {
        return DateTimeUtil.formatDateTime(requestedAt);
    }

    public long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getSalesRepName() {
        return salesRepName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getExceededBy() {
        return exceededBy;
    }

    public int getBelowFloorLines() {
        return belowFloorLines;
    }
}
