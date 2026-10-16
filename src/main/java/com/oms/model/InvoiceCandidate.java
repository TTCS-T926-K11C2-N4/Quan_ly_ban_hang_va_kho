package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Phiếu xuất đã giao thành công nhưng chưa lập hoá đơn (S7-01)
public class InvoiceCandidate {

    private final long noteId;
    private final String deliveryNo;
    private final long orderId;
    private final String orderNo;
    private final long customerId;
    private final String customerCode;
    private final String customerName;
    private final LocalDateTime deliveredAt;
    private final BigDecimal value;

    public InvoiceCandidate(long noteId, String deliveryNo, long orderId, String orderNo, long customerId,
                            String customerCode, String customerName, LocalDateTime deliveredAt, BigDecimal value) {
        this.noteId = noteId;
        this.deliveryNo = deliveryNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.deliveredAt = deliveredAt;
        this.value = value;
    }

    public long getNoteId() {
        return noteId;
    }

    public String getDeliveryNo() {
        return deliveryNo;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
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

    public String getDeliveredAtText() {
        return DateTimeUtil.formatDateTime(deliveredAt);
    }

    // Giá trị theo số thực giao (chưa trừ chiết khấu đã gộp trong giá dòng)
    public BigDecimal getValue() {
        return value;
    }
}
