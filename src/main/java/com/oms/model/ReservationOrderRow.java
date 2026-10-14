package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Một đơn ở danh sách đơn đang giữ chỗ tồn (S5-06): tổng số lượng đang giữ (đơn vị cơ sở) và tình trạng giữ chỗ
public class ReservationOrderRow {

    private final long orderId;
    private final String orderNo;
    private final String customerName;
    private final String customerCode;
    private final BigDecimal reservedQty;
    private final LocalDateTime approvedAt;
    private final String orderStatus;
    private final ReservationState state;

    public ReservationOrderRow(long orderId, String orderNo, String customerName, String customerCode,
                               BigDecimal reservedQty, LocalDateTime approvedAt, String orderStatus,
                               ReservationState state) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.reservedQty = reservedQty;
        this.approvedAt = approvedAt;
        this.orderStatus = orderStatus;
        this.state = state;
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

    public BigDecimal getReservedQty() {
        return reservedQty;
    }

    public String getApprovedAtText() {
        return approvedAt == null ? "—" : DateTimeUtil.formatDate(approvedAt);
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getOrderStatusLabel() {
        return OrderStatus.label(orderStatus);
    }

    // Đơn còn đi tiếp (đã duyệt, đang soạn...) thì nhãn xanh; đơn huỷ, bị từ chối thì nhãn xám
    public boolean isOrderLive() {
        return !OrderStatus.isTerminalBranch(orderStatus);
    }

    public ReservationState getState() {
        return state;
    }
}
