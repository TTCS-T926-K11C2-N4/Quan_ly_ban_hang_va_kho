package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Một lần đổi giá của một sản phẩm trong một bảng giá (price_history, S3-02). oldPrice null = giá thêm mới.
// Trạng thái lấy theo hiệu lực của bảng giá chứa lần đổi giá đó.
public class PriceHistoryRow {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final long id;
    private final LocalDate effectiveFrom;
    private final LocalDateTime changedAt;
    private final String sku;
    private final String productName;
    private final String categoryName;
    private final String unitName;
    private final String priceListCode;
    private final int versionNo;
    private final String customerGroupName;
    private final BigDecimal oldPrice;
    private final BigDecimal newPrice;
    private final BigDecimal oldFloorPrice;
    private final BigDecimal newFloorPrice;
    private final String actorName;
    private final LocalDate validFrom;
    private final LocalDate validTo;

    public PriceHistoryRow(long id, LocalDate effectiveFrom, LocalDateTime changedAt, String sku, String productName,
                           String categoryName, String unitName, String priceListCode, int versionNo,
                           String customerGroupName, BigDecimal oldPrice, BigDecimal newPrice,
                           BigDecimal oldFloorPrice, BigDecimal newFloorPrice, String actorName, LocalDate validFrom,
                           LocalDate validTo) {
        this.id = id;
        this.effectiveFrom = effectiveFrom;
        this.changedAt = changedAt;
        this.sku = sku;
        this.productName = productName;
        this.categoryName = categoryName;
        this.unitName = unitName;
        this.priceListCode = priceListCode;
        this.versionNo = versionNo;
        this.customerGroupName = customerGroupName;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.oldFloorPrice = oldFloorPrice;
        this.newFloorPrice = newFloorPrice;
        this.actorName = actorName;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public long getId() {
        return id;
    }

    public String getEffectiveFromText() {
        return DateTimeUtil.formatDay(effectiveFrom);
    }

    public String getChangedAtText() {
        return DateTimeUtil.formatDateTime(changedAt);
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getPriceListCode() {
        return priceListCode;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public String getCustomerGroupName() {
        return customerGroupName;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public BigDecimal getOldFloorPrice() {
        return oldFloorPrice;
    }

    public BigDecimal getNewFloorPrice() {
        return newFloorPrice;
    }

    public String getActorName() {
        return actorName;
    }

    public PriceListStatus getStatus() {
        return PriceListStatus.of(validFrom, validTo, DateTimeUtil.today());
    }

    // % thay đổi giá bán so với giá cũ, làm tròn 1 chữ số; null khi là giá thêm mới hoặc giá cũ bằng 0
    public BigDecimal getChangePercent() {
        if (oldPrice == null || oldPrice.signum() == 0) {
            return null;
        }
        return newPrice.subtract(oldPrice).multiply(HUNDRED).divide(oldPrice, 1, RoundingMode.HALF_UP);
    }
}
