package com.oms.model;

// Điều kiện lọc danh sách nhà cung cấp; null nghĩa là không lọc theo tiêu chí đó
public class SupplierFilter {

    private final String keyword;
    private final String status;

    public SupplierFilter(String keyword, String status) {
        this.keyword = keyword;
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getStatus() {
        return status;
    }
}
