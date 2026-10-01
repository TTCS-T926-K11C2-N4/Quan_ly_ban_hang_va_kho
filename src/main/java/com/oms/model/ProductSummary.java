package com.oms.model;

// Sản phẩm hiển thị trong danh sách của một nhóm hàng (để chọn và chuyển sang nhóm khác)
public class ProductSummary {

    private final long id;
    private final String sku;
    private final String name;
    private final String status;

    public ProductSummary(long id, String sku, String name, String status) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.status = status;
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

    // ACTIVE | DISCONTINUED
    public String getStatus() {
        return status;
    }
}
