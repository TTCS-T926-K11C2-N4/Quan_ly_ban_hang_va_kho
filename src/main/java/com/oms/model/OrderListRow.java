package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Một dòng của danh sách đơn hàng (S4-07)
public class OrderListRow {

    private final long id;
    private final String orderNo;
    private final String customerName;
    private final String customerCode;
    private final String regionName;
    private final String salesRepName;
    private final LocalDate requestedDate;
    private final LocalDateTime createdAt;
    private final BigDecimal total;
    private final String status;
    private final boolean needsApproval;

    public OrderListRow(long id, String orderNo, String customerName, String customerCode, String regionName,
                        String salesRepName, LocalDate requestedDate, LocalDateTime createdAt, BigDecimal total,
                        String status, boolean needsApproval) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.regionName = regionName;
        this.salesRepName = salesRepName;
        this.requestedDate = requestedDate;
        this.createdAt = createdAt;
        this.total = total;
        this.status = status;
        this.needsApproval = needsApproval;
    }

    public String getStatusLabel() {
        return OrderStatus.label(status);
    }

    public String getStatusTone() {
        return OrderStatus.tone(status);
    }

    public String getRequestedDateText() {
        return DateTimeUtil.formatDay(requestedDate);
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDate(createdAt);
    }

    public long getId() {
        return id;
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

    public String getRegionName() {
        return regionName;
    }

    public String getSalesRepName() {
        return salesRepName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }

    // Đơn chờ duyệt vì vượt hạn mức hoặc dưới giá sàn (S4-05)
    public boolean isNeedsApproval() {
        return needsApproval && OrderStatus.PENDING_APPROVAL.equals(status);
    }
}
