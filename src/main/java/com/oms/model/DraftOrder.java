package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Đơn trong hộp "Mở lại" của màn tạo đơn: đơn nháp (S3-09) và đơn đã gửi gần đây (S4-06). updatedAt lưu theo UTC.
public class DraftOrder {

    private final long id;
    private final String orderNo;
    private final String customerName;
    private final boolean customerBlocked;
    private final BigDecimal total;
    private final LocalDateTime updatedAt;
    private final String status;

    public DraftOrder(long id, String orderNo, String customerName, boolean customerBlocked, BigDecimal total,
                      LocalDateTime updatedAt, String status) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.customerBlocked = customerBlocked;
        this.total = total;
        this.updatedAt = updatedAt;
        this.status = status;
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

    public boolean isCustomerBlocked() {
        return customerBlocked;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getUpdatedAtText() {
        return DateTimeUtil.formatDateTime(updatedAt);
    }

    public String getStatusLabel() {
        return OrderStatus.label(status);
    }

    public String getStatusTone() {
        return OrderStatus.tone(status);
    }
}
