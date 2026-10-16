package com.oms.security;

import com.oms.model.Permission;

import java.util.Map;
import java.util.Set;

// Quyền cần có cho từng đường dẫn Servlet (S1-05). Mặc định từ chối: Servlet nào chưa khai báo ở đây
// thì không ai vào được, kể cả Admin, để không quên kiểm quyền khi thêm chức năng mới.
public final class AccessRules {

    // Chỉ cần đăng nhập, không cần quyền cụ thể
    private static final String AUTHENTICATED = "";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/login", "/logout", "/forgot-password", "/forgot-password/sent", "/reset-password",
            "/session-expired", "/error");

    private static final Map<String, String> REQUIRED_PERMISSIONS = Map.ofEntries(
            Map.entry("/dashboard", AUTHENTICATED),
            Map.entry("/portal", AUTHENTICATED),
            Map.entry("/change-password", AUTHENTICATED),
            Map.entry("/profile", AUTHENTICATED),
            Map.entry("/profile/avatar", AUTHENTICATED),
            Map.entry("/avatar", AUTHENTICATED),
            Map.entry("/accounts", Permission.USER_VIEW),
            Map.entry("/accounts/view", Permission.USER_VIEW),
            Map.entry("/accounts/new", Permission.USER_MANAGE),
            Map.entry("/accounts/edit", Permission.USER_MANAGE),
            Map.entry("/accounts/lock", Permission.USER_MANAGE),
            Map.entry("/accounts/unlock", Permission.USER_MANAGE),
            Map.entry("/accounts/import", Permission.USER_MANAGE),
            Map.entry("/accounts/import/template", Permission.USER_MANAGE),
            Map.entry("/accounts/import/preview", Permission.USER_MANAGE),
            Map.entry("/accounts/import/result", Permission.USER_MANAGE),
            Map.entry("/accounts/import/errors", Permission.USER_MANAGE),
            Map.entry("/audit-logs", Permission.AUDIT_LOG_VIEW),
            Map.entry("/audit-logs/export", Permission.AUDIT_LOG_VIEW),
            Map.entry("/customers", Permission.CUSTOMER_VIEW),
            Map.entry("/customers/profile", Permission.CUSTOMER_VIEW),
            Map.entry("/customers/profile/action", Permission.CUSTOMER_MANAGE),
            Map.entry("/customers/new", Permission.CUSTOMER_MANAGE),
            Map.entry("/customers/edit", Permission.CUSTOMER_MANAGE),
            Map.entry("/customers/assignments", Permission.CUSTOMER_VIEW),
            Map.entry("/customers/assignments/assign", Permission.CUSTOMER_ASSIGN),
            Map.entry("/customers/assignments/transfer", Permission.CUSTOMER_ASSIGN),
            Map.entry("/customers/credit-limit", Permission.CUSTOMER_VIEW),
            Map.entry("/customers/credit-limit/edit", Permission.CREDIT_MANAGE),
            Map.entry("/categories", Permission.PRODUCT_VIEW),
            Map.entry("/categories/products", Permission.PRODUCT_VIEW),
            Map.entry("/categories/new", Permission.PRODUCT_MANAGE),
            Map.entry("/categories/edit", Permission.PRODUCT_MANAGE),
            Map.entry("/categories/delete", Permission.PRODUCT_MANAGE),
            Map.entry("/categories/status", Permission.PRODUCT_MANAGE),
            Map.entry("/categories/products/move", Permission.PRODUCT_MANAGE),
            Map.entry("/customers/block", Permission.CUSTOMER_VIEW),
            Map.entry("/customers/block/save", Permission.CREDIT_MANAGE),
            Map.entry("/customers/delivery-addresses", Permission.CUSTOMER_VIEW),
            Map.entry("/customers/delivery-addresses/save", Permission.CUSTOMER_MANAGE),
            Map.entry("/customers/delivery-addresses/action", Permission.CUSTOMER_MANAGE),
            Map.entry("/price-lists", Permission.PRODUCT_VIEW),
            Map.entry("/price-history", Permission.PRODUCT_VIEW),
            Map.entry("/discount-policies", Permission.PRODUCT_VIEW),
            Map.entry("/discount-policies/save", Permission.PRODUCT_MANAGE),
            Map.entry("/discount-policies/action", Permission.PRODUCT_MANAGE),
            Map.entry("/price-lists/new", Permission.PRODUCT_MANAGE),
            Map.entry("/price-lists/edit", Permission.PRODUCT_MANAGE),
            Map.entry("/price-lists/version", Permission.PRODUCT_MANAGE),
            Map.entry("/price-lists/delete", Permission.PRODUCT_MANAGE),
            Map.entry("/products", Permission.PRODUCT_VIEW),
            Map.entry("/suppliers", Permission.INVENTORY_VIEW),
            Map.entry("/suppliers/new", Permission.INVENTORY_MANAGE),
            Map.entry("/suppliers/edit", Permission.INVENTORY_MANAGE),
            Map.entry("/suppliers/status", Permission.INVENTORY_MANAGE),
            Map.entry("/suppliers/delete", Permission.INVENTORY_MANAGE),
            // Kho và vị trí lưu trong kho (S5-03): ai xem tồn kho cũng xem được, chỉ Quản lý kho và Admin khai báo
            Map.entry("/warehouses", Permission.INVENTORY_VIEW),
            Map.entry("/warehouses/export", Permission.INVENTORY_VIEW),
            Map.entry("/warehouses/new", Permission.WAREHOUSE_MANAGE),
            Map.entry("/warehouses/edit", Permission.WAREHOUSE_MANAGE),
            Map.entry("/warehouses/locations", Permission.INVENTORY_VIEW),
            Map.entry("/warehouses/locations/save", Permission.WAREHOUSE_MANAGE),
            Map.entry("/warehouses/locations/status", Permission.WAREHOUSE_MANAGE),
            Map.entry("/units", Permission.PRODUCT_VIEW),
            Map.entry("/units/new", Permission.PRODUCT_MANAGE),
            Map.entry("/units/edit", Permission.PRODUCT_MANAGE),
            Map.entry("/units/delete", Permission.PRODUCT_MANAGE),
            // Phiếu nhập kho (S5-04): ai xem tồn kho cũng xem phiếu; nhân viên kho, quản lý kho lập và ghi sổ
            Map.entry("/goods-receipts", Permission.INVENTORY_VIEW),
            Map.entry("/goods-receipts/export", Permission.INVENTORY_VIEW),
            Map.entry("/goods-receipts/view", Permission.INVENTORY_VIEW),
            Map.entry("/goods-receipts/new", Permission.INVENTORY_MANAGE),
            Map.entry("/goods-receipts/edit", Permission.INVENTORY_MANAGE),
            Map.entry("/transfers", Permission.INVENTORY_VIEW),
            Map.entry("/transfers/view", Permission.INVENTORY_VIEW),
            Map.entry("/transfers/new", Permission.INVENTORY_MANAGE),
            Map.entry("/transfers/line/add", Permission.INVENTORY_MANAGE),
            Map.entry("/transfers/line/remove", Permission.INVENTORY_MANAGE),
            Map.entry("/transfers/ship", Permission.INVENTORY_MANAGE),
            Map.entry("/transfers/receive", Permission.INVENTORY_MANAGE),
            Map.entry("/transfers/cancel", Permission.INVENTORY_MANAGE),
            Map.entry("/orders", Permission.ORDER_VIEW),
            Map.entry("/orders/new", Permission.ORDER_MANAGE),
            Map.entry("/orders/customer-data", Permission.ORDER_MANAGE),
            Map.entry("/orders/history", Permission.ORDER_MANAGE),
            Map.entry("/orders/quote", Permission.ORDER_MANAGE),
            Map.entry("/portal/orders", Permission.ORDER_VIEW),
            Map.entry("/portal/orders/view", Permission.ORDER_VIEW),
            Map.entry("/portal/order/new", Permission.ORDER_MANAGE),
            Map.entry("/portal/orders/reorder", Permission.ORDER_MANAGE),
            Map.entry("/orders/view", Permission.ORDER_VIEW),
            Map.entry("/orders/print", Permission.ORDER_VIEW),
            Map.entry("/orders/cancel", Permission.ORDER_MANAGE),
            // Hoá đơn bán hàng (S7-01): xem, in cần INVOICE_VIEW (phạm vi ALL); phát hành cần INVOICE_MANAGE
            Map.entry("/invoices", Permission.INVOICE_VIEW),
            Map.entry("/invoices/view", Permission.INVOICE_VIEW),
            Map.entry("/invoices/print", Permission.INVOICE_VIEW),
            Map.entry("/invoices/issue", Permission.INVOICE_MANAGE),
            // Phiếu thu và đối trừ công nợ (S7-02): xem cần INVOICE_VIEW (phạm vi ALL), ghi nhận cần INVOICE_MANAGE
            Map.entry("/payments", Permission.INVOICE_VIEW),
            Map.entry("/payments/new", Permission.INVOICE_MANAGE),
            Map.entry("/payments/print", Permission.INVOICE_VIEW),
            // Thu tiền theo tuyến (S7-05): NV kinh doanh ghi nhận (PAYMENT_COLLECT), kế toán xác nhận/huỷ và đối chiếu
            Map.entry("/collections", Permission.PAYMENT_COLLECT),
            Map.entry("/collections/new", Permission.PAYMENT_COLLECT),
            Map.entry("/collections/reconcile", Permission.INVOICE_VIEW),
            Map.entry("/payments/confirm", Permission.INVOICE_MANAGE),
            Map.entry("/payments/void", Permission.INVOICE_MANAGE),
            Map.entry("/payments/receipt", AUTHENTICATED),
            // Biên bản đối chiếu công nợ (S7-06): lập và gửi cần INVOICE_MANAGE (kiểm tra trong Servlet POST)
            // Trả hàng (S8-01): quyền chi tiết (lập, nhận hàng, phạm vi) kiểm tra trong ReturnService.access
            Map.entry("/returns", AUTHENTICATED),
            Map.entry("/returns/new", AUTHENTICATED),
            Map.entry("/returns/view", AUTHENTICATED),
            Map.entry("/returns/receive", AUTHENTICATED),
            Map.entry("/returns/cancel", AUTHENTICATED),
            // Chứng từ điều chỉnh giảm trừ công nợ (S8-02): xem cần INVOICE_VIEW (ALL), lập cần INVOICE_MANAGE
            Map.entry("/credit-notes", Permission.INVOICE_VIEW),
            Map.entry("/credit-notes/view", Permission.INVOICE_VIEW),
            Map.entry("/credit-notes/issue", Permission.INVOICE_MANAGE),
            Map.entry("/statements", Permission.INVOICE_VIEW),
            Map.entry("/statements/view", Permission.INVOICE_VIEW),
            Map.entry("/statements/print", Permission.INVOICE_VIEW),
            Map.entry("/portal/statements", Permission.INVOICE_VIEW),
            Map.entry("/portal/statements/respond", Permission.INVOICE_VIEW),
            Map.entry("/portal/statements/print", Permission.INVOICE_VIEW),
            // Sổ công nợ theo đại lý và tuổi nợ (S7-03)
            Map.entry("/debts", Permission.INVOICE_VIEW),
            Map.entry("/debts/export", Permission.INVOICE_VIEW),
            // Sổ chi tiết công nợ một đại lý (S8-10)
            Map.entry("/debts/detail", Permission.INVOICE_VIEW),
            Map.entry("/debts/detail/export", Permission.INVOICE_VIEW),
            Map.entry("/debts/detail/print", Permission.INVOICE_VIEW),
            // Báo cáo công nợ quá hạn theo tuổi nợ (S8-09)
            Map.entry("/debts/overdue", Permission.INVOICE_VIEW),
            Map.entry("/debts/overdue/export", Permission.INVOICE_VIEW),
            // Báo cáo (EP-09): trang tổng hợp và báo cáo tồn kho, vòng quay hàng (S8-07)
            Map.entry("/reports", Permission.REPORT_VIEW),
            Map.entry("/reports/stock", Permission.INVENTORY_VIEW),
            Map.entry("/reports/stock/export", Permission.INVENTORY_VIEW),
            // Báo cáo doanh số nhiều chiều (S8-06)
            Map.entry("/reports/sales", Permission.REPORT_VIEW),
            Map.entry("/reports/sales/export", Permission.REPORT_VIEW),
            // Chỉ tiêu doanh số tháng (S8-04): đặt chỉ tiêu cần SALES_TARGET_MANAGE, xem tiến độ của mình cần REPORT_VIEW
            Map.entry("/targets", Permission.SALES_TARGET_MANAGE),
            Map.entry("/targets/my", Permission.REPORT_VIEW),
            // Đại lý xem công nợ và hoá đơn của chính mình (S7-07)
            Map.entry("/portal/debts", Permission.INVOICE_VIEW),
            Map.entry("/portal/invoices/print", Permission.INVOICE_VIEW),
            // Nhắc hạn thanh toán và thông báo trong ứng dụng (S7-08)
            Map.entry("/invoices/reminders", Permission.INVOICE_MANAGE),
            Map.entry("/notifications", AUTHENTICATED),
            Map.entry("/orders/confirm", Permission.ORDER_MANAGE),
            Map.entry("/orders/copy", Permission.ORDER_MANAGE),
            Map.entry("/approvals", Permission.ORDER_APPROVAL_VIEW),
            Map.entry("/approvals/view", Permission.ORDER_APPROVAL_VIEW),
            Map.entry("/approvals/decide", Permission.ORDER_APPROVE),
            // Sổ tồn kho (S5-05): ai xem tồn kho cũng xem được (nhân viên kinh doanh xem tồn khả dụng)
            Map.entry("/stock", Permission.INVENTORY_VIEW),
            Map.entry("/stock/export", Permission.INVENTORY_VIEW),
            Map.entry("/stock/min-stock", Permission.INVENTORY_MANAGE),
            Map.entry("/products/image", Permission.PRODUCT_VIEW),
            Map.entry("/products/new", Permission.PRODUCT_MANAGE),
            Map.entry("/products/edit", Permission.PRODUCT_MANAGE),
            Map.entry("/products/delete", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import/template", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import/preview", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import/result", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import/errors", Permission.PRODUCT_MANAGE),
            // Giữ chỗ tồn (S5-06): ai xem tồn kho cũng xem được; nhân viên kinh doanh chỉ thấy đơn của đại lý mình
            // (lọc theo phạm vi ORDER_VIEW trong StockReservationService)
            Map.entry("/stock/reservations", Permission.INVENTORY_VIEW),
            Map.entry("/stocktakes", Permission.INVENTORY_VIEW),
            Map.entry("/stocktakes/view", Permission.INVENTORY_VIEW),
            Map.entry("/stocktakes/new", Permission.INVENTORY_MANAGE),
            Map.entry("/stocktakes/count", Permission.INVENTORY_MANAGE),
            Map.entry("/stocktakes/cancel", Permission.INVENTORY_MANAGE),
            Map.entry("/stocktakes/finalize", Permission.STOCKTAKE_FINALIZE),
            // Điều chỉnh tồn kho (S8-03): lập cần INVENTORY_MANAGE, duyệt/từ chối cần quyền chốt kho của Quản lý kho
            Map.entry("/adjustments", Permission.INVENTORY_VIEW),
            Map.entry("/adjustments/view", Permission.INVENTORY_VIEW),
            Map.entry("/adjustments/new", Permission.INVENTORY_MANAGE),
            Map.entry("/adjustments/approve", Permission.STOCKTAKE_FINALIZE),
            Map.entry("/adjustments/reject", Permission.STOCKTAKE_FINALIZE),
            Map.entry("/stock/reservations/view", Permission.INVENTORY_VIEW),
            // Phiếu soạn hàng (S6-03): ai xem giao hàng cũng xem phiếu; nhân viên kho, quản lý kho lập phiếu và ghi nhận soạn
            Map.entry("/pick-lists", Permission.DELIVERY_VIEW),
            Map.entry("/pick-lists/view", Permission.DELIVERY_VIEW),
            Map.entry("/pick-lists/new", Permission.DELIVERY_MANAGE),
            Map.entry("/pick-lists/pick", Permission.DELIVERY_MANAGE),
            Map.entry("/pick-lists/cancel", Permission.DELIVERY_MANAGE),
            // Xác nhận xuất kho & giao thiếu (S6-05): ai xem giao hàng cũng xem phiếu xuất; nhân viên kho, quản lý kho xác nhận
            Map.entry("/delivery-notes/view", Permission.DELIVERY_VIEW),
            Map.entry("/delivery-notes/confirm", Permission.DELIVERY_MANAGE),
            // Xác nhận giao hàng (S6-08): ai xem giao hàng cũng xem điểm giao, ảnh biên bản; nhân viên kho, quản lý kho ghi nhận
            Map.entry("/deliveries", Permission.DELIVERY_VIEW),
            Map.entry("/deliveries/point", Permission.DELIVERY_VIEW),
            Map.entry("/deliveries/proof", Permission.DELIVERY_VIEW),
            Map.entry("/deliveries/confirm", Permission.DELIVERY_MANAGE),
            Map.entry("/products/status", Permission.PRODUCT_MANAGE));

    private AccessRules() {
    }

    public static boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path) || path.startsWith("/assets/");
    }

    public static boolean isAllowed(String path, Set<String> permissions) {
        String required = REQUIRED_PERMISSIONS.get(path);
        if (required == null) {
            return false;
        }
        return required.isEmpty() || permissions.contains(required);
    }
}
