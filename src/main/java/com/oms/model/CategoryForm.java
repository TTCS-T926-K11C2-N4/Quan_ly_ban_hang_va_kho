package com.oms.model;

// Dữ liệu form thêm/sửa nhóm hàng (đã trim). sortOrder giữ nguyên chữ người dùng gõ để hiển thị lại khi lỗi.
public class CategoryForm {

    private final String code;
    private final String name;
    private final String description;
    private final Long parentId;
    private final String sortOrder;

    public CategoryForm(String code, String name, String description, Long parentId, String sortOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    // null = nhóm cấp 1
    public Long getParentId() {
        return parentId;
    }

    public String getSortOrder() {
        return sortOrder;
    }
}
