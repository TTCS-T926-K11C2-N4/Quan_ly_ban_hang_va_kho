package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Một phiếu xuất (một đơn) cần giao tại một điểm giao (S6-08): hàng trong phiếu, giá trị thu hộ, kết quả giao
public class DeliveryStop {

    private final long noteId;
    private final String deliveryNo;
    private final String status;
    private final long warehouseId;
    private final long orderId;
    private final String orderNo;
    private final LocalDate requestedDate;
    private final LocalDateTime issuedAt;
    private final BigDecimal value;
    private final String recipientName;
    private final LocalDateTime deliveredAt;
    private final String failureReason;
    private final String deliveryRemark;
    private final List<Item> items = new ArrayList<>();
    private final List<Long> proofIds = new ArrayList<>();

    public DeliveryStop(long noteId, String deliveryNo, String status, long warehouseId, long orderId, String orderNo,
                        LocalDate requestedDate, LocalDateTime issuedAt, BigDecimal value, String recipientName,
                        LocalDateTime deliveredAt, String failureReason, String deliveryRemark) {
        this.noteId = noteId;
        this.deliveryNo = deliveryNo;
        this.status = status;
        this.warehouseId = warehouseId;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.requestedDate = requestedDate;
        this.issuedAt = issuedAt;
        this.value = value;
        this.recipientName = recipientName;
        this.deliveredAt = deliveredAt;
        this.failureReason = failureReason;
        this.deliveryRemark = deliveryRemark;
    }

    public long getNoteId() {
        return noteId;
    }

    public String getDeliveryNo() {
        return deliveryNo;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return DeliveryNoteStatus.label(status);
    }

    public String getTone() {
        return DeliveryNoteStatus.tone(status);
    }

    public boolean isPending() {
        return DeliveryNoteStatus.isPending(status);
    }

    public boolean isDelivered() {
        return DeliveryNoteStatus.DELIVERED.equals(status);
    }

    public boolean isFailed() {
        return DeliveryNoteStatus.FAILED.equals(status);
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public String getRequestedDateText() {
        return requestedDate == null ? "Chưa hẹn" : DateTimeUtil.formatDay(requestedDate);
    }

    public String getIssuedAtText() {
        return DateTimeUtil.formatDateTime(issuedAt);
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getDeliveredAtText() {
        return DateTimeUtil.formatDateTime(deliveredAt);
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getDeliveryRemark() {
        return deliveryRemark;
    }

    public List<Item> getItems() {
        return items;
    }

    public List<Long> getProofIds() {
        return proofIds;
    }

    // Một mặt hàng trong phiếu xuất: số đã xuất theo đơn vị cơ sở
    public static class Item {

        private final String sku;
        private final String productName;
        private final BigDecimal qty;
        private final String unitName;

        public Item(String sku, String productName, BigDecimal qty, String unitName) {
            this.sku = sku;
            this.productName = productName;
            this.qty = qty;
            this.unitName = unitName;
        }

        public String getSku() {
            return sku;
        }

        public String getProductName() {
            return productName;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public String getUnitName() {
            return unitName;
        }
    }
}
