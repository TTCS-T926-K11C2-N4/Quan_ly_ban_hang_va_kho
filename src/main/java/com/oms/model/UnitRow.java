package com.oms.model;

// Một đơn vị tính ở trang Đơn vị tính (S2-07), kèm số SKU đang dùng
public class UnitRow {

    private final long id;
    private final String code;
    private final String name;
    private final int productCount;

    public UnitRow(long id, String code, String name, int productCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.productCount = productCount;
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

    public int getProductCount() {
        return productCount;
    }
}
