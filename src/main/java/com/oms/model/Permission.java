package com.oms.model;

// Mã quyền trong bảng permissions (nạp bằng database/seed_permissions.sql).
// Chỉ khai báo các quyền đang được code Java kiểm tra; thêm dần khi làm từng module.
public final class Permission {

    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_MANAGE = "USER_MANAGE";
    // Chỉ Admin (seed_permissions.sql: Admin có mọi quyền trừ COST_PRICE_VIEW)
    public static final String AUDIT_LOG_VIEW = "AUDIT_LOG_VIEW";
    public static final String PRODUCT_VIEW = "PRODUCT_VIEW";
    public static final String PRODUCT_MANAGE = "PRODUCT_MANAGE";
    // Chỉ Quản lý kinh doanh có, kể cả Admin cũng không (seed_permissions.sql)
    public static final String COST_PRICE_VIEW = "COST_PRICE_VIEW";

    private Permission() {
    }
}
