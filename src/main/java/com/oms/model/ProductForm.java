package com.oms.model;

import java.util.List;

// Dữ liệu form Thêm/Sửa sản phẩm, giữ nguyên chuỗi người dùng nhập để hiện lại khi có lỗi.
// costPrice = null khi người dùng không có quyền sửa giá vốn (ô không hiện trên form).
public class ProductForm {

    // Một dòng quy đổi trong form (S2-07); unitId = null nếu chưa chọn đơn vị
    public static class Conversion {
        private final Long unitId;
        private final String factor;

        public Conversion(Long unitId, String factor) {
            this.unitId = unitId;
            this.factor = factor;
        }

        public Long getUnitId() {
            return unitId;
        }

        public String getFactor() {
            return factor;
        }
    }

    private final String sku;
    private final String name;
    private final Long categoryId;
    private final Long baseUnitId;
    private final String packagingSpec;
    private final String costPrice;
    private final String status;
    private final String description;
    private final Long version;
    private final List<Conversion> conversions;

    public ProductForm(String sku, String name, Long categoryId, Long baseUnitId, String packagingSpec,
                       String costPrice, String status, String description, Long version,
                       List<Conversion> conversions) {
        this.sku = sku;
        this.name = name;
        this.categoryId = categoryId;
        this.baseUnitId = baseUnitId;
        this.packagingSpec = packagingSpec;
        this.costPrice = costPrice;
        this.status = status;
        this.description = description;
        this.version = version;
        this.conversions = conversions;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getBaseUnitId() {
        return baseUnitId;
    }

    public String getPackagingSpec() {
        return packagingSpec;
    }

    public String getCostPrice() {
        return costPrice;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public Long getVersion() {
        return version;
    }

    public List<Conversion> getConversions() {
        return conversions;
    }
}
