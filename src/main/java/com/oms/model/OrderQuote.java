package com.oms.model;

import java.math.BigDecimal;
import java.util.List;

// Tiền của đơn đang gõ (S3-09): từng dòng và tổng tiền hàng, chiết khấu, tổng phải thu.
// Tính ở server (SalesOrderService.quote) cho cả lúc gõ (AJAX) lẫn lúc lưu nên hai nơi luôn khớp.
public class OrderQuote {

    private final List<Line> lines;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public OrderQuote(List<Line> lines) {
        this.lines = List.copyOf(lines);
        BigDecimal subtotalSum = BigDecimal.ZERO;
        BigDecimal discountSum = BigDecimal.ZERO;
        for (Line line : lines) {
            if (line.getError() == null) {
                subtotalSum = subtotalSum.add(line.getAmount());
                discountSum = discountSum.add(line.getDiscount());
            }
        }
        this.subtotal = subtotalSum;
        this.discount = discountSum;
        this.total = subtotalSum.subtract(discountSum);
    }

    public List<Line> getLines() {
        return lines;
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

    public boolean hasErrors() {
        return lines.stream().anyMatch(line -> line.getError() != null);
    }

    // Một dòng đã tính giá. error != null: dòng chưa hợp lệ (thiếu hàng, sai số lượng, chưa có giá...),
    // các giá trị tiền bằng 0. Các trường còn lại là snapshot ghi vào sales_order_items.
    public static class Line {
        private final OrderProduct product;
        private final OrderProduct.Unit unit;
        private final BigDecimal qty;
        private final BigDecimal qtyBase;
        private final BigDecimal unitPrice;
        private final BigDecimal floorPrice;
        private final Long priceListItemId;
        private final Long discountPolicyId;
        private final BigDecimal amount;
        private final BigDecimal discount;
        private final String error;

        public Line(OrderProduct product, OrderProduct.Unit unit, BigDecimal qty, BigDecimal qtyBase,
                    BigDecimal unitPrice, BigDecimal floorPrice, Long priceListItemId, Long discountPolicyId,
                    BigDecimal amount, BigDecimal discount) {
            this.product = product;
            this.unit = unit;
            this.qty = qty;
            this.qtyBase = qtyBase;
            this.unitPrice = unitPrice;
            this.floorPrice = floorPrice;
            this.priceListItemId = priceListItemId;
            this.discountPolicyId = discountPolicyId;
            this.amount = amount;
            this.discount = discount;
            this.error = null;
        }

        public Line(String error) {
            this.product = null;
            this.unit = null;
            this.qty = null;
            this.qtyBase = null;
            this.unitPrice = BigDecimal.ZERO;
            this.floorPrice = BigDecimal.ZERO;
            this.priceListItemId = null;
            this.discountPolicyId = null;
            this.amount = BigDecimal.ZERO;
            this.discount = BigDecimal.ZERO;
            this.error = error;
        }

        public OrderProduct getProduct() {
            return product;
        }

        public OrderProduct.Unit getUnit() {
            return unit;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public BigDecimal getQtyBase() {
            return qtyBase;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getFloorPrice() {
            return floorPrice;
        }

        public Long getPriceListItemId() {
            return priceListItemId;
        }

        public Long getDiscountPolicyId() {
            return discountPolicyId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BigDecimal getDiscount() {
            return discount;
        }

        public BigDecimal getTotal() {
            return amount.subtract(discount);
        }

        public String getError() {
            return error;
        }
    }
}
