package com.oms.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// Giá và chiết khấu áp cho một nhóm khách hàng vào một ngày (S3-09 tính tiền khi gõ đơn):
// dòng giá của bảng giá đang hiệu lực (S2-10) theo "productId:unitId" và các chính sách chiết khấu (S3-01).
public class PricingRules {

    private final Map<String, PriceItem> priceItems;
    private final List<DiscountPolicy> discountPolicies;

    public PricingRules(Map<String, PriceItem> priceItems, List<DiscountPolicy> discountPolicies) {
        this.priceItems = Map.copyOf(priceItems);
        this.discountPolicies = List.copyOf(discountPolicies);
    }

    public static String key(long productId, long unitId) {
        return productId + ":" + unitId;
    }

    public PriceItem findPrice(long productId, long unitId) {
        return priceItems.get(key(productId, unitId));
    }

    public List<DiscountPolicy> getDiscountPolicies() {
        return discountPolicies;
    }

    public static class PriceItem {
        private final long id;
        private final BigDecimal price;
        private final BigDecimal floorPrice;

        public PriceItem(long id, BigDecimal price, BigDecimal floorPrice) {
            this.id = id;
            this.price = price;
            this.floorPrice = floorPrice;
        }

        public long getId() {
            return id;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public BigDecimal getFloorPrice() {
            return floorPrice;
        }
    }

    // Một chính sách cho một SKU (productId) hoặc một nhóm hàng (categoryId); tiers sắp min_qty_base tăng dần
    public static class DiscountPolicy {
        public static final String PERCENT = "PERCENT";

        private final long id;
        private final Long productId;
        private final Long categoryId;
        private final String discountType;
        private final List<Tier> tiers;

        public DiscountPolicy(long id, Long productId, Long categoryId, String discountType, List<Tier> tiers) {
            this.id = id;
            this.productId = productId;
            this.categoryId = categoryId;
            this.discountType = discountType;
            this.tiers = List.copyOf(tiers);
        }

        public long getId() {
            return id;
        }

        public Long getProductId() {
            return productId;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public boolean isPercent() {
            return PERCENT.equals(discountType);
        }

        public List<Tier> getTiers() {
            return tiers;
        }
    }

    public static class Tier {
        private final BigDecimal minQtyBase;
        private final BigDecimal value;

        public Tier(BigDecimal minQtyBase, BigDecimal value) {
            this.minQtyBase = minQtyBase;
            this.value = value;
        }

        public BigDecimal getMinQtyBase() {
            return minQtyBase;
        }

        public BigDecimal getValue() {
            return value;
        }
    }
}
