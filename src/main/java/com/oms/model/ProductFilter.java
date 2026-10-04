package com.oms.model;

// Bộ lọc danh sách sản phẩm; null = không lọc theo tiêu chí đó
public class ProductFilter {

    private final String keyword;
    private final String categoryPath;
    private final Long warehouseId;
    private final StockStatus status;
    private final String sort;

    // categoryPath: path của nhóm đang lọc (lấy cả nhóm con cháu); sort: một khoá trong ProductService.SORTS
    public ProductFilter(String keyword, String categoryPath, Long warehouseId, StockStatus status, String sort) {
        this.keyword = keyword;
        this.categoryPath = categoryPath;
        this.warehouseId = warehouseId;
        this.status = status;
        this.sort = sort;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public StockStatus getStatus() {
        return status;
    }

    public String getSort() {
        return sort;
    }
}
