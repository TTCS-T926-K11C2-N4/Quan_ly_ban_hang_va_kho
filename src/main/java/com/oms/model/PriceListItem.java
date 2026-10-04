package com.oms.model;

import java.math.BigDecimal;

// Một dòng giá (price_list_items) kèm thông tin sản phẩm. costPrice = null khi người xem không có quyền giá vốn.
public class PriceListItem {

    private final long productId;
    private final String sku;
    private final String productName;
    private final long unitId;
    private final String unitName;
    private final BigDecimal costPrice;
    private final BigDecimal price;
    private final BigDecimal floorPrice;

    public PriceListItem(long productId, String sku, String productName, long unitId, String unitName,
                         BigDecimal costPrice, BigDecimal price, BigDecimal floorPrice) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitId = unitId;
        this.unitName = unitName;
        this.costPrice = costPrice;
        this.price = price;
        this.floorPrice = floorPrice;
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

    public long getUnitId() {
        return unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public BigDecimal getPrice() {
        return price;
    }

    // Bán dưới giá này phải qua duyệt (S4-05)
    public BigDecimal getFloorPrice() {
        return floorPrice;
    }
}
