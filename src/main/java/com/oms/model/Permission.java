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
    // Xem chi tiết đơn (S4-06): Nhân viên kinh doanh chỉ đơn của đại lý mình phụ trách; kho, kế toán xem mọi đơn
    public static final String ORDER_VIEW = "ORDER_VIEW";
    // Duyệt đơn vượt hạn mức / dưới giá sàn (S4-05): Quản lý kinh doanh, Admin duyệt; Kế toán chỉ xem hàng chờ
    public static final String ORDER_APPROVAL_VIEW = "ORDER_APPROVAL_VIEW";
    public static final String ORDER_APPROVE = "ORDER_APPROVE";
    // Chỉ Quản lý kinh doanh có, kể cả Admin cũng không (seed_permissions.sql)
    public static final String COST_PRICE_VIEW = "COST_PRICE_VIEW";
    // Hoá đơn và công nợ (S7): Kế toán công nợ lập và thu tiền, Quản lý kinh doanh xem; Đại lý chỉ xem của mình (OWN)
    public static final String INVOICE_VIEW = "INVOICE_VIEW";
    public static final String INVOICE_MANAGE = "INVOICE_MANAGE";
    public static final String PAYMENT_COLLECT = "PAYMENT_COLLECT";
    // Nhà cung cấp (S2-09) dùng chung quyền kho: nhân viên kho, quản lý kho, Admin quản lý
    public static final String INVENTORY_VIEW = "INVENTORY_VIEW";
    public static final String INVENTORY_MANAGE = "INVENTORY_MANAGE";
    // Thêm, sửa kho và vị trí lưu trong kho (S5-03): Quản lý kho và Admin; Nhân viên kho chỉ xem
    public static final String WAREHOUSE_MANAGE = "WAREHOUSE_MANAGE";
    // Phiếu soạn, xuất kho, giao hàng (S6-03, S6-05, S6-08): nhân viên kho, quản lý kho, Admin soạn - xuất - giao;
    // kế toán, quản lý kinh doanh xem; nhân viên kinh doanh chỉ xem đơn của đại lý mình phụ trách (data_scope ASSIGNED)
    public static final String DELIVERY_VIEW = "DELIVERY_VIEW";
    public static final String DELIVERY_MANAGE = "DELIVERY_MANAGE";
    // Đại lý: Nhân viên kinh doanh chỉ thấy đại lý mình phụ trách (data_scope ASSIGNED)
    public static final String CUSTOMER_VIEW = "CUSTOMER_VIEW";
    // Sửa thông tin đại lý, điểm giao hàng (S3-04): Nhân viên kinh doanh chỉ với đại lý mình phụ trách
    public static final String CUSTOMER_MANAGE = "CUSTOMER_MANAGE";
    // Phân công, chuyển giao nhân viên kinh doanh phụ trách đại lý (S3-06): Quản lý kinh doanh và Admin
    public static final String CUSTOMER_ASSIGN = "CUSTOMER_ASSIGN";
    // Sửa hạn mức công nợ (S3-05), khoá/mở giao dịch đại lý (S3-07): chỉ Kế toán công nợ và Quản lý kinh doanh,
    // Admin cũng không có
    public static final String CREDIT_MANAGE = "CREDIT_MANAGE";
    // Chốt phiếu kiểm kê, điều chỉnh tồn về số đếm (S5-08): chỉ Quản lý kho, kể cả Admin cũng không (seed_permissions.sql)
    public static final String STOCKTAKE_FINALIZE = "STOCKTAKE_FINALIZE";

    private Permission() {
    }
}
