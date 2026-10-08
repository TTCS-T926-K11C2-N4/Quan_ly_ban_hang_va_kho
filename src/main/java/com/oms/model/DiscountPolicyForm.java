package com.oms.model;

import java.util.ArrayList;
import java.util.List;

// Dữ liệu form thêm/sửa chính sách chiết khấu (S3-01), giữ nguyên chữ người dùng gõ để hiện lại khi có lỗi
public class DiscountPolicyForm {

    private final Long id;
    private final String name;
    private final String customerGroupId;
    private final String scopeType;
    private final String productSku;
    private final String categoryId;
    private final String discountType;
    private final String validFrom;
    private final String validTo;
    private final boolean active;
    private final List<TierLine> tiers;

    public DiscountPolicyForm(Long id, String name, String customerGroupId, String scopeType, String productSku,
                              String categoryId, String discountType, String validFrom, String validTo,
                              boolean active, List<TierLine> tiers) {
        this.id = id;
        this.name = name;
        this.customerGroupId = customerGroupId;
        this.scopeType = scopeType;
        this.productSku = productSku;
        this.categoryId = categoryId;
        this.discountType = discountType;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = active;
        this.tiers = List.copyOf(tiers);
    }

    // Một bậc như người dùng gõ: số lượng tối thiểu và mức chiết khấu
    public record TierLine(String minQty, String value) {
    }

    public static DiscountPolicyForm empty() {
        return new DiscountPolicyForm(null, null, null, DiscountPolicyRow.SCOPE_PRODUCT, null, null,
                DiscountPolicyRow.PERCENT, null, null, true, List.of(new TierLine(null, null)));
    }

    public static DiscountPolicyForm of(DiscountPolicyRow policy) {
        List<TierLine> tiers = new ArrayList<>();
        for (DiscountPolicyRow.Tier tier : policy.getTiers()) {
            tiers.add(new TierLine(DiscountPolicyRow.number(tier.minQtyBase()).replace(".", ""),
                    tier.value().stripTrailingZeros().toPlainString().replace('.', ',')));
        }
        return new DiscountPolicyForm(policy.getId(), policy.getName(),
                policy.getCustomerGroupId() == null ? null : String.valueOf(policy.getCustomerGroupId()),
                policy.getScopeType(), policy.isProductScope() ? policy.getTargetCode() : null,
                policy.getCategoryId() == null ? null : String.valueOf(policy.getCategoryId()),
                policy.getDiscountType(), policy.getValidFrom().toString(),
                policy.getValidTo() == null ? null : policy.getValidTo().toString(), policy.isActive(), tiers);
    }

    // id = null: thêm chính sách mới
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCustomerGroupId() {
        return customerGroupId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getDiscountType() {
        return discountType;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public boolean isActive() {
        return active;
    }

    public List<TierLine> getTiers() {
        return tiers;
    }
}
