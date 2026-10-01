package com.oms.model;

// Một nhóm hàng trong cây (S2-06), kèm số liệu để hiển thị và kiểm tra trước khi xoá
public class ProductCategory {

    private final long id;
    private final Long parentId;
    private final String code;
    private final String name;
    private final String description;
    private final int level;
    private final String path;
    private final int sortOrder;
    private final boolean active;
    private final int productCount;
    private final int childCount;
    private final int branchProductCount;

    public ProductCategory(long id, Long parentId, String code, String name, String description, int level, String path,
                           int sortOrder, boolean active, int productCount, int childCount, int branchProductCount) {
        this.id = id;
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.level = level;
        this.path = path;
        this.sortOrder = sortOrder;
        this.active = active;
        this.productCount = productCount;
        this.childCount = childCount;
        this.branchProductCount = branchProductCount;
    }

    public long getId() {
        return id;
    }

    // null với nhóm cấp 1
    public Long getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    // Có thể null
    public String getDescription() {
        return description;
    }

    // 1 = nhóm gốc
    public int getLevel() {
        return level;
    }

    // Đường dẫn id từ gốc, vd /1/5/12/ (nhóm 12 nằm trong 5, 5 nằm trong 1)
    public String getPath() {
        return path;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    // Sản phẩm gắn trực tiếp vào nhóm này
    public int getProductCount() {
        return productCount;
    }

    public int getChildCount() {
        return childCount;
    }

    // Sản phẩm của cả nhánh (nhóm này và mọi nhóm con cháu)
    public int getBranchProductCount() {
        return branchProductCount;
    }

    public boolean isDescendantOf(ProductCategory other) {
        return id != other.id && path.startsWith(other.path);
    }
}
