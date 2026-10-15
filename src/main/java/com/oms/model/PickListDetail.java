package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Phiếu soạn hàng kèm các đơn được gộp và dòng soạn đã sắp theo đường đi trong kho (S6-03)
public class PickListDetail {

    private final long id;
    private final String pickNo;
    private final long warehouseId;
    private final String warehouseCode;
    private final String warehouseName;
    private final String status;
    private final String assignedName;
    private final String createdByName;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;
    private final List<Order> orders;
    private final List<Line> lines;

    public PickListDetail(long id, String pickNo, long warehouseId, String warehouseCode, String warehouseName,
                          String status, String assignedName, String createdByName, LocalDateTime createdAt,
                          LocalDateTime completedAt, List<Order> orders, List<Line> lines) {
        this.id = id;
        this.pickNo = pickNo;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.status = status;
        this.assignedName = assignedName;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.orders = orders;
        this.lines = lines;
    }

    public long getId() {
        return id;
    }

    public String getPickNo() {
        return pickNo;
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return PickListStatus.label(status);
    }

    public String getTone() {
        return PickListStatus.tone(status);
    }

    public boolean isEditable() {
        return PickListStatus.isEditable(status);
    }

    public boolean isDone() {
        return PickListStatus.DONE.equals(status);
    }

    // Người soạn: người nhận phiếu (ghi nhận soạn đầu tiên); chưa ai nhận thì để trống
    public String getAssignedName() {
        return assignedName;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public String getCreatedDateText() {
        return DateTimeUtil.formatDate(createdAt);
    }

    public String getCompletedAtText() {
        return DateTimeUtil.formatDateTime(completedAt);
    }

    public List<Order> getOrders() {
        return orders;
    }

    public List<Line> getLines() {
        return lines;
    }

    public Line findLine(long lineId) {
        return lines.stream().filter(line -> line.getId() == lineId).findFirst().orElse(null);
    }

    public int getPickedLines() {
        return (int) lines.stream().filter(Line::isPicked).count();
    }

    public int getShortLines() {
        return (int) lines.stream().filter(Line::isShortage).count();
    }

    // Phần trăm dòng đã soạn cho thanh tiến độ
    public int getProgressPercent() {
        return lines.isEmpty() ? 0 : getPickedLines() * 100 / lines.size();
    }

    // Đã xác nhận xuất kho ít nhất một đơn của phiếu (S6-05)
    public boolean isAnyIssued() {
        return orders.stream().anyMatch(Order::isIssued);
    }

    public Order findOrder(long orderId) {
        return orders.stream().filter(order -> order.getOrderId() == orderId).findFirst().orElse(null);
    }

    // Ngày giao sớm nhất trong các đơn của phiếu
    public String getDeliveryDateText() {
        return orders.stream().map(Order::getDeliveryDate).filter(date -> date != null).min(LocalDate::compareTo)
                .map(DateTimeUtil::formatDay).orElse("Chưa hẹn");
    }

    // Một đơn được gộp trong phiếu; deliveryNoteId khác null khi đã xác nhận xuất kho (S6-05)
    public static class Order {

        private final long orderId;
        private final String orderNo;
        private final String customerName;
        private final LocalDate deliveryDate;
        private final Long deliveryNoteId;
        private final String deliveryNo;

        public Order(long orderId, String orderNo, String customerName, LocalDate deliveryDate, Long deliveryNoteId,
                     String deliveryNo) {
            this.orderId = orderId;
            this.orderNo = orderNo;
            this.customerName = customerName;
            this.deliveryDate = deliveryDate;
            this.deliveryNoteId = deliveryNoteId;
            this.deliveryNo = deliveryNo;
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

        public LocalDate getDeliveryDate() {
            return deliveryDate;
        }

        public String getDeliveryDateText() {
            return deliveryDate == null ? "Chưa hẹn" : DateTimeUtil.formatDay(deliveryDate);
        }

        public Long getDeliveryNoteId() {
            return deliveryNoteId;
        }

        public String getDeliveryNo() {
            return deliveryNo;
        }

        public boolean isIssued() {
            return deliveryNoteId != null;
        }
    }

    // Một dòng soạn: một dòng đơn tại một lô (FEFO), vị trí lấy hàng, số cần lấy và số thực lấy (đơn vị cơ sở)
    public static class Line {

        private final long id;
        private final long orderId;
        private final String orderNo;
        private final String sku;
        private final String productName;
        private final String unitName;
        private final String lotNo;
        private final boolean defaultLot;
        private final LocalDate expiryDate;
        private final String locationCode;
        private final String locationName;
        private final BigDecimal qtyRequired;
        private final BigDecimal qtyPicked;
        private final String shortReason;

        public Line(long id, long orderId, String orderNo, String sku, String productName, String unitName,
                    String lotNo, boolean defaultLot, LocalDate expiryDate, String locationCode, String locationName,
                    BigDecimal qtyRequired, BigDecimal qtyPicked, String shortReason) {
            this.id = id;
            this.orderId = orderId;
            this.orderNo = orderNo;
            this.sku = sku;
            this.productName = productName;
            this.unitName = unitName;
            this.lotNo = lotNo;
            this.defaultLot = defaultLot;
            this.expiryDate = expiryDate;
            this.locationCode = locationCode;
            this.locationName = locationName;
            this.qtyRequired = qtyRequired;
            this.qtyPicked = qtyPicked;
            this.shortReason = shortReason;
        }

        public long getId() {
            return id;
        }

        public long getOrderId() {
            return orderId;
        }

        public String getOrderNo() {
            return orderNo;
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

        // Lô mặc định của SKU không quản lý lô thì không hiện số lô
        public String getLotText() {
            if (defaultLot) {
                return null;
            }
            return expiryDate == null ? "Lô " + lotNo : "Lô " + lotNo + " · HSD " + DateTimeUtil.formatDay(expiryDate);
        }

        public String getLocationCode() {
            return locationCode;
        }

        public String getLocationName() {
            return locationName;
        }

        public BigDecimal getQtyRequired() {
            return qtyRequired;
        }

        public BigDecimal getQtyPicked() {
            return qtyPicked;
        }

        public String getQtyPickedText() {
            return qtyPicked == null ? "" : qtyPicked.stripTrailingZeros().toPlainString();
        }

        public String getShortReason() {
            return shortReason;
        }

        public boolean isPicked() {
            return qtyPicked != null;
        }

        public boolean isShortage() {
            return qtyPicked != null && qtyPicked.compareTo(qtyRequired) < 0;
        }
    }
}
