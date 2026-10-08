package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// Một chính sách chiết khấu theo sản lượng (S3-01) kèm các bậc; dùng cho danh sách và để mở form sửa.
// targetCode / targetName: SKU và tên sản phẩm (PRODUCT) hoặc mã và tên nhóm hàng (CATEGORY).
public class DiscountPolicyRow {

    public static final String SCOPE_PRODUCT = "PRODUCT";
    public static final String SCOPE_CATEGORY = "CATEGORY";
    public static final String PERCENT = "PERCENT";
    public static final String AMOUNT_PER_UNIT = "AMOUNT_PER_UNIT";

    private final long id;
    private final String code;
    private final String name;
    private final Long customerGroupId;
    private final String customerGroupName;
    private final String scopeType;
    private final Long categoryId;
    private final String targetCode;
    private final String targetName;
    private final String discountType;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final boolean active;
    private final boolean used;
    private final List<Tier> tiers;

    public DiscountPolicyRow(long id, String code, String name, Long customerGroupId, String customerGroupName,
                             String scopeType, Long categoryId, String targetCode, String targetName,
                             String discountType, LocalDate validFrom, LocalDate validTo, boolean active, boolean used,
                             List<Tier> tiers) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.customerGroupId = customerGroupId;
        this.customerGroupName = customerGroupName;
        this.scopeType = scopeType;
        this.categoryId = categoryId;
        this.targetCode = targetCode;
        this.targetName = targetName;
        this.discountType = discountType;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = active;
        this.used = used;
        this.tiers = List.copyOf(tiers);
    }

    // Bậc: mua từ minQtyBase (đơn vị cơ sở) trở lên thì được value (% hoặc đồng / đơn vị cơ sở)
    public record Tier(BigDecimal minQtyBase, BigDecimal value) {
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Long getCustomerGroupId() {
        return customerGroupId;
    }

    // null = áp dụng cho mọi nhóm khách hàng
    public String getCustomerGroupName() {
        return customerGroupName;
    }

    public String getScopeType() {
        return scopeType;
    }

    public boolean isProductScope() {
        return SCOPE_PRODUCT.equals(scopeType);
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getDiscountType() {
        return discountType;
    }

    public boolean isPercent() {
        return PERCENT.equals(discountType);
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public String getValidFromText() {
        return DateTimeUtil.formatDay(validFrom);
    }

    public String getValidToText() {
        return validTo == null ? "Không thời hạn" : DateTimeUtil.formatDay(validTo);
    }

    public boolean isActive() {
        return active;
    }

    // Đã có dòng đơn hàng dùng chính sách này: không xoá được, chỉ ngừng áp dụng
    public boolean isUsed() {
        return used;
    }

    public List<Tier> getTiers() {
        return tiers;
    }

    public DiscountPolicyStatus getStatus() {
        return DiscountPolicyStatus.of(active, validFrom, validTo, DateTimeUtil.today());
    }

    // Tóm tắt các bậc cho danh sách, vd "Từ 24: 2% · Từ 48: 3%" hoặc "Từ 24: 500 đ/đv"
    public String getTierSummary() {
        return tiers.stream().map(tier -> "Từ " + number(tier.minQtyBase()) + ": " + valueText(tier.value()))
                .collect(Collectors.joining(" · "));
    }

    public String valueText(BigDecimal value) {
        return isPercent() ? number(value) + "%" : number(value) + " đ/đv";
    }

    static String number(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));
        return format.format(value);
    }
}
