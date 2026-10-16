package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Dữ liệu màn "Xác nhận xuất kho & giao thiếu" (S6-05) của một đơn trong một phiếu soạn đã soạn xong:
// mỗi dòng đơn có số cần xuất (phần còn phải giao lúc lập phiếu soạn) và số đã soạn được.
public class DeliveryIssueDraft {

    private final long pickListId;
    private final String pickNo;
    private final String pickStatus;
    private final long warehouseId;
    private final String warehouseName;
    private final String pickerName;
    private final long orderId;
    private final String orderNo;
    private final String orderStatus;
    private final String customerName;
    private final LocalDate deliveryDate;
    private final Long deliveryNoteId;
    private final List<Line> lines;

    public DeliveryIssueDraft(long pickListId, String pickNo, String pickStatus, long warehouseId, String warehouseName,
                              String pickerName, long orderId, String orderNo, String orderStatus, String customerName,
                              LocalDate deliveryDate, Long deliveryNoteId, List<Line> lines) {
        this.pickListId = pickListId;
        this.pickNo = pickNo;
        this.pickStatus = pickStatus;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.pickerName = pickerName;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.orderStatus = orderStatus;
        this.customerName = customerName;
        this.deliveryDate = deliveryDate;
        this.deliveryNoteId = deliveryNoteId;
        this.lines = lines;
    }

    public long getPickListId() {
        return pickListId;
    }

    public String getPickNo() {
        return pickNo;
    }

    public String getPickStatus() {
        return pickStatus;
    }

    // Chỉ xác nhận xuất kho khi phiếu soạn đã soạn xong
    public boolean isPickDone() {
        return PickListStatus.DONE.equals(pickStatus);
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getPickerName() {
        return pickerName;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getDeliveryDateText() {
        return deliveryDate == null ? "Chưa hẹn" : DateTimeUtil.formatDay(deliveryDate);
    }

    // Đã xác nhận xuất kho (phiếu xuất không sửa được nữa)
    public Long getDeliveryNoteId() {
        return deliveryNoteId;
    }

    public List<Line> getLines() {
        return lines;
    }

    public Line findLine(long orderItemId) {
        return lines.stream().filter(line -> line.getOrderItemId() == orderItemId).findFirst().orElse(null);
    }

    public BigDecimal getTotalRequired() {
        return lines.stream().map(Line::getQtyRequired).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPicked() {
        return lines.stream().map(Line::getQtyPicked).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Một dòng đơn: số cần xuất, số đã soạn, lý do soạn thiếu lúc soạn (gợi ý sẵn cho lý do xuất thiếu)
    public static class Line {

        private final long orderItemId;
        private final long productId;
        private final String sku;
        private final String productName;
        private final String unitName;
        private final BigDecimal qtyRequired;
        private final BigDecimal qtyPicked;
        private final String pickShortReason;

        public Line(long orderItemId, long productId, String sku, String productName, String unitName,
                    BigDecimal qtyRequired, BigDecimal qtyPicked, String pickShortReason) {
            this.orderItemId = orderItemId;
            this.productId = productId;
            this.sku = sku;
            this.productName = productName;
            this.unitName = unitName;
            this.qtyRequired = qtyRequired;
            this.qtyPicked = qtyPicked;
            this.pickShortReason = pickShortReason;
        }

        public long getOrderItemId() {
            return orderItemId;
        }

        public long getProductId() {
            return productId;
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

        public String getQtyRequiredText() {
            return qtyRequired.stripTrailingZeros().toPlainString();
        }

        public BigDecimal getQtyPicked() {
            return qtyPicked;
        }

        public String getQtyPickedText() {
            return qtyPicked.stripTrailingZeros().toPlainString();
        }

        public String getPickShortReason() {
            return pickShortReason;
        }
    }
}
