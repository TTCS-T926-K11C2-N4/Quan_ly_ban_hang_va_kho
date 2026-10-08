package com.oms.model;

import java.util.List;

// Dữ liệu form "Tạo đơn hàng" (S3-09), giữ nguyên chuỗi người dùng gõ để hiện lại khi có lỗi.
// draftId/version: đang mở lại đơn nháp nào (null khi tạo mới) và version lúc mở, để phát hiện sửa chồng.
public class OrderForm {

    private final String customerId;
    private final String deliveryAddressId;
    private final String requestedDate;
    private final String note;
    private final Long draftId;
    private final String version;
    private final List<Line> lines;

    public OrderForm(String customerId, String deliveryAddressId, String requestedDate, String note, Long draftId,
                     String version, List<Line> lines) {
        this.customerId = customerId;
        this.deliveryAddressId = deliveryAddressId;
        this.requestedDate = requestedDate;
        this.note = note;
        this.draftId = draftId;
        this.version = version;
        this.lines = List.copyOf(lines);
    }

    public static OrderForm empty() {
        return new OrderForm(null, null, null, null, null, null, List.of());
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getDeliveryAddressId() {
        return deliveryAddressId;
    }

    public String getRequestedDate() {
        return requestedDate;
    }

    public String getNote() {
        return note;
    }

    public Long getDraftId() {
        return draftId;
    }

    public String getVersion() {
        return version;
    }

    public List<Line> getLines() {
        return lines;
    }

    // Một dòng hàng; productText là chữ trong ô tìm hàng (để hiện lại khi chưa chọn được sản phẩm)
    public static class Line {
        private final String productId;
        private final String productText;
        private final String unitId;
        private final String qty;

        public Line(String productId, String productText, String unitId, String qty) {
            this.productId = productId;
            this.productText = productText;
            this.unitId = unitId;
            this.qty = qty;
        }

        public String getProductId() {
            return productId;
        }

        public String getProductText() {
            return productText;
        }

        public String getUnitId() {
            return unitId;
        }

        public String getQty() {
            return qty;
        }

        public boolean isBlank() {
            return productId == null && productText == null && qty == null;
        }
    }
}
