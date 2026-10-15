package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Một dòng bảng "Danh sách phiếu soạn" (S6-03)
public class PickListRow {

    private final long id;
    private final String pickNo;
    private final String orderNos;
    private final String warehouseName;
    private final LocalDate deliveryDate;
    private final String status;
    private final int orderCount;
    private final int issuedCount;
    private final LocalDateTime createdAt;

    public PickListRow(long id, String pickNo, String orderNos, String warehouseName, LocalDate deliveryDate,
                       String status, int orderCount, int issuedCount, LocalDateTime createdAt) {
        this.id = id;
        this.pickNo = pickNo;
        this.orderNos = orderNos;
        this.warehouseName = warehouseName;
        this.deliveryDate = deliveryDate;
        this.status = status;
        this.orderCount = orderCount;
        this.issuedCount = issuedCount;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getPickNo() {
        return pickNo;
    }

    public String getOrderNos() {
        return orderNos;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getDeliveryDateText() {
        return deliveryDate == null ? "Chưa hẹn" : DateTimeUtil.formatDay(deliveryDate);
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public String getStatus() {
        return status;
    }

    public int getOrderCount() {
        return orderCount;
    }

    // Đã soạn xong và mọi đơn trong phiếu đã xác nhận xuất kho (S6-05)
    public boolean isFullyIssued() {
        return PickListStatus.DONE.equals(status) && orderCount > 0 && issuedCount >= orderCount;
    }

    // Đã soạn xong, còn đơn chờ xác nhận xuất kho
    public boolean isAwaitingIssue() {
        return PickListStatus.DONE.equals(status) && issuedCount < orderCount;
    }

    public String getStatusLabel() {
        return isFullyIssued() ? "Đã xuất kho" : PickListStatus.label(status);
    }

    public String getTone() {
        return isFullyIssued() ? "issued" : PickListStatus.tone(status);
    }
}
