package com.oms.model;

// Điều kiện lọc danh sách kho (S5-03): từ khoá theo mã/tên/địa chỉ, loại kho, trạng thái; null là "Tất cả"
public class WarehouseFilter {

    private final String keyword;
    private final String type;
    private final String status;

    public WarehouseFilter(String keyword, String type, String status) {
        this.keyword = keyword == null ? "" : keyword;
        this.type = Warehouse.TYPE_LABELS.containsKey(type) ? type : null;
        this.status = Warehouse.STATUS_LABELS.containsKey(status) ? status : null;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public boolean isFiltered() {
        return !keyword.isEmpty() || type != null || status != null;
    }
}
