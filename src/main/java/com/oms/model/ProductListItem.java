package com.oms.model;

import java.math.BigDecimal;
import java.util.List;

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
    private final List<String> conversions;

    public ProductListItem(long id, String sku, String name, String categoryName, String baseUnitName,
                           BigDecimal costPrice, Long imageFileId, BigDecimal stock, StockStatus stockStatus,
                           List<String> conversions) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.categoryName = categoryName;
        this.baseUnitName = baseUnitName;
        this.costPrice = costPrice;
        this.imageFileId = imageFileId;
        this.stock = stock;
        this.stockStatus = stockStatus;
        this.conversions = conversions;
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

    // Đơn vị quy đổi để kho biết nhập xuất theo thùng/lốc (S2-07), vd "1 Thùng = 24 Lon"
    public List<String> getConversions() {
        return conversions;
    }

    public boolean isDiscontinued() {
        return stockStatus == StockStatus.DISCONTINUED;
    }
}
