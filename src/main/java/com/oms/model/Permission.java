package com.oms.model;

// Mã quyền trong bảng permissions (nạp bằng database/seed_permissions.sql).
// Chỉ khai báo các quyền đang được code Java kiểm tra; thêm dần khi làm từng module.
public final class Permission {

    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String INVENTORY_VIEW = "INVENTORY_VIEW";
    public static final String INVENTORY_MANAGE = "INVENTORY_MANAGE";

    private Permission() {
    }
}
