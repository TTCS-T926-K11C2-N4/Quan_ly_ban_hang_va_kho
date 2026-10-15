package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Đơn hàng nhìn từ cổng đại lý (S5-01): chỉ thông tin đại lý được xem, không có kho, nhân viên, giá sàn, giá vốn
public class PortalOrder {

    private final long id;
    private final String orderNo;
    private final String status;
    private final LocalDate requestedDate;
    private final LocalDateTime createdAt;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final String note;
    private final String deliveryAddress;
    private final List<Item> items;

    public PortalOrder(long id, String orderNo, String status, LocalDate requestedDate, LocalDateTime createdAt,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal total, String note,
                       String deliveryAddress, List<Item> items) {
        this.id = id;
        this.orderNo = orderNo;
        this.status = status;
        this.requestedDate = requestedDate;
        this.createdAt = createdAt;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.note = note;
        this.deliveryAddress = deliveryAddress;
        this.items = List.copyOf(items);
    }

    public long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return OrderStatus.label(status);
    }

    public String getStatusTone() {
        return OrderStatus.tone(status);
    }

    public String getRequestedDateText() {
        return DateTimeUtil.formatDay(requestedDate);
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDate(createdAt);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getNote() {
        return note;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public List<Item> getItems() {
        return items;
    }

    // Có dòng nào giao thiếu so với số đặt (chỉ xét khi đơn đã xuất kho)
    public boolean isShortDelivery() {
        return items.stream().anyMatch(Item::isShortDelivered);
    }

    // Dòng hàng; qty tính theo đơn vị đặt, qtyShipped quy ngược từ đơn vị cơ sở theo hệ số đã chụp lúc đặt
    public static class Item {
        private final int lineNo;
        private final String sku;
        private final String productName;
        private final String unitName;
        private final BigDecimal qty;
        private final BigDecimal qtyShipped;
        private final boolean shippedKnown;
        private final BigDecimal unitPrice;
        private final BigDecimal discount;
        private final BigDecimal lineTotal;

        public Item(int lineNo, String sku, String productName, String unitName, BigDecimal qty,
                    BigDecimal qtyShipped, boolean shippedKnown, BigDecimal unitPrice, BigDecimal discount,
                    BigDecimal lineTotal) {
            this.lineNo = lineNo;
            this.sku = sku;
            this.productName = productName;
            this.unitName = unitName;
            this.qty = qty;
            this.qtyShipped = qtyShipped;
            this.shippedKnown = shippedKnown;
            this.unitPrice = unitPrice;
            this.discount = discount;
            this.lineTotal = lineTotal;
        }

        public int getLineNo() {
            return lineNo;
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

        public BigDecimal getQty() {
            return qty;
        }

        public BigDecimal getQtyShipped() {
            return qtyShipped;
        }

        // Đơn đã xuất kho nên số thực giao có ý nghĩa
        public boolean isShippedKnown() {
            return shippedKnown;
        }

        public boolean isShortDelivered() {
            return shippedKnown && qtyShipped.compareTo(qty) < 0;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getDiscount() {
            return discount;
        }

        public BigDecimal getLineTotal() {
            return lineTotal;
        }
    }
}
