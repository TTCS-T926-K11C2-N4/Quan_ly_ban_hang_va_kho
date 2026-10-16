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
    private final List<Milestone> timeline;

    public PortalOrder(long id, String orderNo, String status, LocalDate requestedDate, LocalDateTime createdAt,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal total, String note,
                       String deliveryAddress, List<Item> items) {
        this(id, orderNo, status, requestedDate, createdAt, subtotal, discount, total, note, deliveryAddress, items,
                List.of());
    }

    public PortalOrder(long id, String orderNo, String status, LocalDate requestedDate, LocalDateTime createdAt,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal total, String note,
                       String deliveryAddress, List<Item> items, List<Milestone> timeline) {
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
        this.timeline = List.copyOf(timeline);
    }

    // Các mốc giao hàng của đơn (S6-09): đã duyệt, đang soạn, đã xuất kho, đang giao, đã giao
    public List<Milestone> getTimeline() {
        return timeline;
    }

    // Chỉ số mốc cuối cùng đã đạt (-1 nếu chưa đạt mốc nào), để làm nổi bật bước hiện tại
    public int getCurrentStep() {
        int current = -1;
        for (int i = 0; i < timeline.size(); i++) {
            if (timeline.get(i).isReached()) {
                current = i;
            }
        }
        return current;
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

    // Một mốc trên đường đi của đơn; at null nghĩa là chưa tới mốc này
    public static class Milestone {
        private final String label;
        private final LocalDateTime at;

        public Milestone(String label, LocalDateTime at) {
            this.label = label;
            this.at = at;
        }

        public String getLabel() {
            return label;
        }

        public boolean isReached() {
            return at != null;
        }

        public String getAtText() {
            return DateTimeUtil.formatDateTime(at);
        }
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
        private final BigDecimal qtyDelivered;

        public Item(int lineNo, String sku, String productName, String unitName, BigDecimal qty,
                    BigDecimal qtyShipped, boolean shippedKnown, BigDecimal unitPrice, BigDecimal discount,
                    BigDecimal lineTotal) {
            this(lineNo, sku, productName, unitName, qty, qtyShipped, shippedKnown, unitPrice, discount, lineTotal,
                    null);
        }

        // qtyDelivered: số đã giao thành công theo đơn vị đặt (từ phiếu xuất đã giao); null nếu chưa có phiếu giao xong
        public Item(int lineNo, String sku, String productName, String unitName, BigDecimal qty,
                    BigDecimal qtyShipped, boolean shippedKnown, BigDecimal unitPrice, BigDecimal discount,
                    BigDecimal lineTotal, BigDecimal qtyDelivered) {
            this.qtyDelivered = qtyDelivered;
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

        // Số thực giao thành công; null nếu chưa có phiếu xuất nào giao xong
        public BigDecimal getQtyDelivered() {
            return qtyDelivered;
        }

        public boolean isDeliveredKnown() {
            return qtyDelivered != null;
        }

        // Số lượng thực giao nếu đã giao xong, ngược lại số đã xuất kho
        public BigDecimal getQtyActual() {
            return qtyDelivered != null ? qtyDelivered : qtyShipped;
        }

        public boolean isShortDelivered() {
            return shippedKnown && getQtyActual().compareTo(qty) < 0;
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
