package com.oms.model;

// Một dòng trên màn hình cây nhóm hàng: nhóm hàng kèm STT theo cấp (1, 1.1, 1.1.1...)
public class CategoryRow {

    private final ProductCategory category;
    private final String number;
    private final boolean matched;

    public CategoryRow(ProductCategory category, String number, boolean matched) {
        this.category = category;
        this.number = number;
        this.matched = matched;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public String getNumber() {
        return number;
    }

    // false: nhóm cha chỉ hiện để giữ ngữ cảnh khi tìm kiếm/lọc, bản thân không khớp điều kiện
    public boolean isMatched() {
        return matched;
    }
}
