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
    // Tạo đơn hàng (S3-09): Nhân viên kinh doanh chỉ cho đại lý mình phụ trách (data_scope ASSIGNED)
    public static final String ORDER_MANAGE = "ORDER_MANAGE";
    // Chỉ Quản lý kinh doanh có, kể cả Admin cũng không (seed_permissions.sql)
    public static final String COST_PRICE_VIEW = "COST_PRICE_VIEW";
    // Nhà cung cấp (S2-09) dùng chung quyền kho: nhân viên kho, quản lý kho, Admin quản lý
    public static final String INVENTORY_VIEW = "INVENTORY_VIEW";
    public static final String INVENTORY_MANAGE = "INVENTORY_MANAGE";
    // Đại lý: Nhân viên kinh doanh chỉ thấy đại lý mình phụ trách (data_scope ASSIGNED)
    public static final String CUSTOMER_VIEW = "CUSTOMER_VIEW";
    // Sửa hạn mức công nợ (S3-05), khoá/mở giao dịch đại lý (S3-07): chỉ Kế toán công nợ và Quản lý kinh doanh,
    // Admin cũng không có
    public static final String CREDIT_MANAGE = "CREDIT_MANAGE";

    private Permission() {
    }
}
