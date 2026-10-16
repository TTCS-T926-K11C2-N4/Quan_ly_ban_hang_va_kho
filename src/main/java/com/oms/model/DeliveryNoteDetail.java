package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Phiếu xuất kho đã xác nhận (S6-05, chỉ xem): dòng xuất theo dòng đơn (cần xuất, thực xuất, thiếu, lý do),
// cách xử lý phần thiếu, kết quả giao hàng (S6-08).
public class DeliveryNoteDetail {

    private final long id;
    private final String deliveryNo;
    private final String status;
    private final LocalDateTime issuedAt;
    private final String issuedByName;
    private final Long pickListId;
    private final String pickNo;
    private final long orderId;
    private final String orderNo;
    private final String customerName;
    private final String warehouseName;
    private final LocalDate deliveryDate;
    private final String shortageAction;
    private final LocalDateTime backorderDueAt;
    private final String shortageNote;
    private final String recipientName;
    private final LocalDateTime deliveredAt;
    private final String failureReason;
    private final String deliveryRemark;
    private final List<Line> lines;

    public DeliveryNoteDetail(long id, String deliveryNo, String status, LocalDateTime issuedAt, String issuedByName,
                              Long pickListId, String pickNo, long orderId, String orderNo, String customerName,
                              String warehouseName, LocalDate deliveryDate, String shortageAction,
                              LocalDateTime backorderDueAt, String shortageNote, String recipientName,
                              LocalDateTime deliveredAt, String failureReason, String deliveryRemark, List<Line> lines) {
        this.id = id;
        this.deliveryNo = deliveryNo;
        this.status = status;
        this.issuedAt = issuedAt;
        this.issuedByName = issuedByName;
        this.pickListId = pickListId;
        this.pickNo = pickNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.warehouseName = warehouseName;
        this.deliveryDate = deliveryDate;
        this.shortageAction = shortageAction;
        this.backorderDueAt = backorderDueAt;
        this.shortageNote = shortageNote;
        this.recipientName = recipientName;
        this.deliveredAt = deliveredAt;
        this.failureReason = failureReason;
        this.deliveryRemark = deliveryRemark;
        this.lines = lines;
    }

    public long getId() {
        return id;
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

    public String getIssuedAtText() {
        return DateTimeUtil.formatDateTime(issuedAt);
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public Long getPickListId() {
        return pickListId;
    }

    public String getPickNo() {
        return pickNo;
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

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getDeliveryDateText() {
        return deliveryDate == null ? "Chưa hẹn" : DateTimeUtil.formatDay(deliveryDate);
    }

    public String getShortageAction() {
        return shortageAction;
    }

    public String getShortageLabel() {
        return DeliveryNoteStatus.shortageLabel(shortageAction);
    }

    public boolean isBackorder() {
        return DeliveryNoteStatus.BACKORDER.equals(shortageAction);
    }

    public String getBackorderDueText() {
        return DateTimeUtil.formatDateTime(backorderDueAt);
    }

    public String getShortageNote() {
        return shortageNote;
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

    public List<Line> getLines() {
        return lines;
    }

    public BigDecimal getTotalRequired() {
        return lines.stream().map(Line::getQtyRequired).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalIssued() {
        return lines.stream().map(Line::getQtyIssued).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalShort() {
        return getTotalRequired().subtract(getTotalIssued());
    }

    public int getShortLines() {
        return (int) lines.stream().filter(Line::isShortage).count();
    }

    // Một dòng đơn trên phiếu xuất; lots là các lô đã xuất, vd "L2610A: 40, MAC-DINH: 8"
    public static class Line {

        private final long orderItemId;
        private final String sku;
        private final String productName;
        private final String unitName;
        private final BigDecimal qtyRequired;
        private final BigDecimal qtyIssued;
        private final BigDecimal qtyDelivered;
        private final String shortReason;
        private final String lots;

        public Line(long orderItemId, String sku, String productName, String unitName, BigDecimal qtyRequired,
                    BigDecimal qtyIssued, BigDecimal qtyDelivered, String shortReason, String lots) {
            this.orderItemId = orderItemId;
            this.sku = sku;
            this.productName = productName;
            this.unitName = unitName;
            this.qtyRequired = qtyRequired;
            this.qtyIssued = qtyIssued;
            this.qtyDelivered = qtyDelivered;
            this.shortReason = shortReason;
            this.lots = lots;
        }

        public long getOrderItemId() {
            return orderItemId;
        }

        public String getSku() {
            return sku;
        }

        public String getProductName() {
            return productName;
        }

        public String getUnitName() {
            return unitName;
        }

        public BigDecimal getQtyRequired() {
            return qtyRequired;
        }

        public BigDecimal getQtyIssued() {
            return qtyIssued;
        }

        public BigDecimal getQtyShort() {
            return qtyRequired.subtract(qtyIssued);
        }

        public BigDecimal getQtyDelivered() {
            return qtyDelivered;
        }

        public String getShortReason() {
            return shortReason;
        }

        public String getLots() {
            return lots;
        }

        public boolean isShortage() {
            return qtyIssued.compareTo(qtyRequired) < 0;
        }
    }
}
