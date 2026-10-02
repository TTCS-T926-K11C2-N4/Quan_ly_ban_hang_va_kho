package com.oms.model;

import java.math.BigDecimal;

// Một sản phẩm (SKU) để sửa. costPrice = null khi người xem không có quyền xem giá vốn.
public class Product {

    public static final String ACTIVE = "ACTIVE";
    public static final String DISCONTINUED = "DISCONTINUED";

    private final long id;
    private final String sku;
    private final String name;
    private final long categoryId;
    private final long baseUnitId;
    private final String packagingSpec;
    private final BigDecimal costPrice;
    private final Long imageFileId;
    private final String status;
    private final String description;
    private final long version;

    public Product(long id, String sku, String name, long categoryId, long baseUnitId, String packagingSpec,
                   BigDecimal costPrice, Long imageFileId, String status, String description, long version) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.categoryId = categoryId;
        this.baseUnitId = baseUnitId;
        this.packagingSpec = packagingSpec;
        this.costPrice = costPrice;
        this.imageFileId = imageFileId;
        this.status = status;
        this.description = description;
        this.version = version;
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

    public long getCategoryId() {
        return categoryId;
    }

    public long getBaseUnitId() {
        return baseUnitId;
    }

    public String getPackagingSpec() {
        return packagingSpec;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public Long getImageFileId() {
        return imageFileId;
    }

    // ACTIVE | DISCONTINUED
    public String getStatus() {
        return status;
    }

    public boolean isDiscontinued() {
        return DISCONTINUED.equals(status);
    }

    public String getDescription() {
        return description;
    }

    // Khoá lạc quan: form sửa gửi lại version lúc mở, lưu chỉ khi chưa ai sửa trong lúc đó
    public long getVersion() {
        return version;
    }

    public Product withoutCostPrice() {
        return new Product(id, sku, name, categoryId, baseUnitId, packagingSpec, null, imageFileId, status,
                description, version);
    }
}
