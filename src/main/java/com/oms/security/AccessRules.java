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
            Map.entry("/stock/reservations/view", Permission.INVENTORY_VIEW),
            // Phiếu soạn hàng (S6-03): ai xem giao hàng cũng xem phiếu; nhân viên kho, quản lý kho lập phiếu và ghi nhận soạn
            Map.entry("/pick-lists", Permission.DELIVERY_VIEW),
            Map.entry("/pick-lists/view", Permission.DELIVERY_VIEW),
            Map.entry("/pick-lists/new", Permission.DELIVERY_MANAGE),
            Map.entry("/pick-lists/pick", Permission.DELIVERY_MANAGE),
            // Đổi lô xuất của dòng soạn kèm lý do (S6-04)
            Map.entry("/pick-lists/lot", Permission.DELIVERY_MANAGE),
            Map.entry("/pick-lists/cancel", Permission.DELIVERY_MANAGE),
            // Lô và hạn sử dụng (S6-01): ai xem tồn kho cũng xem lô; bật / tắt quản lý lô theo SKU cần INVENTORY_MANAGE;
            // cho phép xuất lô quá hạn và đặt ngưỡng cảnh báo chỉ Quản lý kho (WAREHOUSE_MANAGE)
            Map.entry("/lots", Permission.INVENTORY_VIEW),
            Map.entry("/lots/track", Permission.INVENTORY_MANAGE),
            Map.entry("/lots/override", Permission.WAREHOUSE_MANAGE),
            Map.entry("/lots/threshold", Permission.WAREHOUSE_MANAGE),
            // Xác nhận xuất kho & giao thiếu (S6-05): ai xem giao hàng cũng xem phiếu xuất; nhân viên kho, quản lý kho xác nhận
            Map.entry("/delivery-notes/view", Permission.DELIVERY_VIEW),
            Map.entry("/delivery-notes/confirm", Permission.DELIVERY_MANAGE),
            // Chuyến giao hàng (S6-06, S6-07): ai xem giao hàng cũng xem chuyến; nhân viên kho, quản lý kho lập và điều hành
            Map.entry("/trips", Permission.DELIVERY_VIEW),
            Map.entry("/trips/view", Permission.DELIVERY_VIEW),
            Map.entry("/trips/print", Permission.DELIVERY_VIEW),
            Map.entry("/trips/monitor", Permission.DELIVERY_VIEW),
            Map.entry("/trips/new", Permission.DELIVERY_MANAGE),
            Map.entry("/trips/add", Permission.DELIVERY_MANAGE),
            Map.entry("/trips/remove", Permission.DELIVERY_MANAGE),
            Map.entry("/trips/depart", Permission.DELIVERY_MANAGE),
            Map.entry("/trips/complete", Permission.DELIVERY_MANAGE),
            Map.entry("/trips/cancel", Permission.DELIVERY_MANAGE),
            // Xác nhận giao hàng (S6-08): ai xem giao hàng cũng xem điểm giao, ảnh biên bản; nhân viên kho, quản lý kho ghi nhận
            Map.entry("/deliveries", Permission.DELIVERY_VIEW),
            Map.entry("/deliveries/point", Permission.DELIVERY_VIEW),
            Map.entry("/deliveries/proof", Permission.DELIVERY_VIEW),
            Map.entry("/deliveries/confirm", Permission.DELIVERY_MANAGE),
            // Thẻ kho (S6-02): ai xem tồn kho cũng xem được
            Map.entry("/stock/card", Permission.INVENTORY_VIEW),
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
