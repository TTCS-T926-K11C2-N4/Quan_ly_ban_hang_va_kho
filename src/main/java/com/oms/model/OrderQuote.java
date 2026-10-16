package com.oms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // S4-01: có dòng bán dưới giá sàn thì đơn phải qua quản lý duyệt (S4-05)
    public boolean isNeedsApproval() {
        return lines.stream().anyMatch(Line::isBelowFloor);
    }

    // Có dòng cần nêu trong khung Thông báo & cảnh báo: sửa giá tay hoặc dưới giá sàn
    public boolean isPriceFlagged() {
        return lines.stream().anyMatch(line -> line.isManualPrice() || line.isBelowFloor());
    }

    public List<Line> getBelowFloorLines() {
        return lines.stream().filter(Line::isBelowFloor).toList();
    }

    // Một dòng đã tính giá. error != null: dòng chưa hợp lệ (thiếu hàng, sai số lượng, chưa có giá...),
    // các giá trị tiền bằng 0. Các trường còn lại là snapshot ghi vào sales_order_items.
    // listPrice: giá bảng giá theo đơn vị đã chọn; unitPrice: giá áp cho dòng (bằng listPrice hoặc giá sửa tay).
    public static class Line {
        private final OrderProduct product;
        private final OrderProduct.Unit unit;
        private final BigDecimal qty;
        private final BigDecimal qtyBase;
        private final BigDecimal listPrice;
        private final BigDecimal unitPrice;
        private final BigDecimal floorPrice;
        private final boolean manualPrice;
        private final Long priceListItemId;
        private final Long discountPolicyId;
        private final BigDecimal amount;
        private final BigDecimal discount;
        private final String error;

        public Line(OrderProduct product, OrderProduct.Unit unit, BigDecimal qty, BigDecimal qtyBase,
                    BigDecimal listPrice, BigDecimal unitPrice, BigDecimal floorPrice, boolean manualPrice,
                    Long priceListItemId, Long discountPolicyId, BigDecimal amount, BigDecimal discount) {
            this.product = product;
            this.unit = unit;
            this.qty = qty;
            this.qtyBase = qtyBase;
            this.listPrice = listPrice;
            this.unitPrice = unitPrice;
            this.floorPrice = floorPrice;
            this.manualPrice = manualPrice;
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
            this.listPrice = BigDecimal.ZERO;
            this.unitPrice = BigDecimal.ZERO;
            this.floorPrice = BigDecimal.ZERO;
            this.manualPrice = false;
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

        public BigDecimal getListPrice() {
            return listPrice;
        }

        public boolean isManualPrice() {
            return manualPrice;
        }

        // Giá áp cho dòng thấp hơn giá sàn (chỉ xảy ra khi sửa tay, vì giá sàn không cao hơn giá bảng giá)
        // So giá sàn bằng giá thực bán sau chiết khấu, để giá sửa tay sát sàn cộng chiết khấu không lọt duyệt
        public boolean isBelowFloor() {
            return error == null && getTotal().compareTo(floorPrice.multiply(qty)) < 0;
        }

        public BigDecimal getNetUnitPrice() {
            return getTotal().divide(qty, 0, RoundingMode.HALF_UP);
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
