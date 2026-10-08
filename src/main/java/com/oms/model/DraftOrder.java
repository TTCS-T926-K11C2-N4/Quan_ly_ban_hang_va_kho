package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Đơn nháp trong hộp "Mở lại" của màn tạo đơn (S3-09). updatedAt lưu theo UTC.
public class DraftOrder {

    private final long id;
    private final String orderNo;
    private final String customerName;
    private final boolean customerBlocked;
    private final BigDecimal total;
    private final LocalDateTime updatedAt;

    public DraftOrder(long id, String orderNo, String customerName, boolean customerBlocked, BigDecimal total,
                      LocalDateTime updatedAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.customerBlocked = customerBlocked;
        this.total = total;
        this.updatedAt = updatedAt;
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
}
