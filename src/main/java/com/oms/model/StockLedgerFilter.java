package com.oms.model;

// Bộ lọc sổ tồn kho (S5-05): tìm theo mã hoặc tên sản phẩm, lọc theo kho, nhóm hàng (gồm cả nhóm con) và trạng thái tồn.
// null là "Tất cả".
public class StockLedgerFilter {

    private final String keyword;
    private final Long warehouseId;
    private final Long categoryId;
    private final StockLevel level;

    public StockLedgerFilter(String keyword, Long warehouseId, Long categoryId, StockLevel level) {
        this.keyword = keyword == null ? "" : keyword;
        this.warehouseId = warehouseId;
        this.categoryId = categoryId;
        this.level = level;
    }

    // Cùng điều kiện nhưng bỏ lọc trạng thái (để đếm số liệu ở các thẻ đầu trang)
    public StockLedgerFilter withoutLevel() {
        return new StockLedgerFilter(keyword, warehouseId, categoryId, null);
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public StockLevel getLevel() {
        return level;
    }

    public boolean isFiltered() {
        return !keyword.isEmpty() || warehouseId != null || categoryId != null || level != null;
    }
}
