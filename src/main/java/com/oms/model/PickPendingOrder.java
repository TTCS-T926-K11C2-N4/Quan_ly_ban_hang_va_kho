package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Một dòng ở màn phiếu soạn hàng (S6-03): đơn đã duyệt chờ soạn, hoặc đơn còn nợ hàng sau khi xuất thiếu
// (S6-05, chọn "Giao bổ sung sau") chờ soạn bổ sung.
public class PickPendingOrder {

    private final long orderId;
    private final String orderNo;
    private final String customerName;
    private final long warehouseId;
    private final String warehouseName;
    private final LocalDate deliveryDate;
    private final boolean backorder;
    private final BigDecimal remainingQty;
    private final LocalDateTime backorderDueAt;
    private final String deliveryNo;
    private final String openPickNo;

    public PickPendingOrder(long orderId, String orderNo, String customerName, long warehouseId, String warehouseName,
                            LocalDate deliveryDate, boolean backorder, BigDecimal remainingQty,
                            LocalDateTime backorderDueAt, String deliveryNo, String openPickNo) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.deliveryDate = deliveryDate;
        this.backorder = backorder;
        this.remainingQty = remainingQty;
        this.backorderDueAt = backorderDueAt;
        this.deliveryNo = deliveryNo;
        this.openPickNo = openPickNo;
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

    public long getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public String getDeliveryDateText() {
        return deliveryDate == null ? "Chưa hẹn" : DateTimeUtil.formatDay(deliveryDate);
    }

    // Quá ngày giao mà chưa soạn
    public boolean isOverdue() {
        return deliveryDate != null && deliveryDate.isBefore(DateTimeUtil.today());
    }

    public boolean isBackorder() {
        return backorder;
    }

    public BigDecimal getRemainingQty() {
        return remainingQty;
    }

    public String getBackorderDueText() {
        return backorderDueAt == null ? "Chưa hẹn" : DateTimeUtil.formatDateTime(backorderDueAt);
    }

    public String getDeliveryNo() {
        return deliveryNo;
    }

    // Phiếu soạn bổ sung đang mở cho đơn (null nếu chưa soạn bổ sung)
    public String getOpenPickNo() {
        return openPickNo;
    }

    public boolean isSelectable() {
        return openPickNo == null;
    }
}
