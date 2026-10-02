package com.oms.model;

import java.math.BigDecimal;

// Một dòng ở danh sách sản phẩm. costPrice = null khi người xem không có quyền xem giá vốn.
public class ProductListItem {

    private final long id;
    private final String sku;
    private final String name;
    private final String categoryName;
    private final String baseUnitName;
    private final BigDecimal costPrice;
    private final Long imageFileId;
    private final BigDecimal stock;
    private final StockStatus stockStatus;

    public ProductListItem(long id, String sku, String name, String categoryName, String baseUnitName,
                           BigDecimal costPrice, Long imageFileId, BigDecimal stock, StockStatus stockStatus) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.categoryName = categoryName;
        this.baseUnitName = baseUnitName;
        this.costPrice = costPrice;
        this.imageFileId = imageFileId;
        this.stock = stock;
        this.stockStatus = stockStatus;
    }

    public long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getBaseUnitName() {
        return baseUnitName;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public Long getImageFileId() {
        return imageFileId;
    }

    // Tồn khả dụng theo đơn vị cơ sở (tổng các kho, hoặc kho đang lọc)
    public BigDecimal getStock() {
        return stock;
    }

    public StockStatus getStockStatus() {
        return stockStatus;
    }

    public boolean isDiscontinued() {
        return stockStatus == StockStatus.DISCONTINUED;
    }
}
