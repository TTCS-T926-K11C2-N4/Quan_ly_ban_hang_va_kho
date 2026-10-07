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
            Map.entry("/customers/credit-limit", Permission.CUSTOMER_VIEW),
            Map.entry("/customers/credit-limit/edit", Permission.CREDIT_MANAGE),
            Map.entry("/categories", Permission.PRODUCT_VIEW),
            Map.entry("/categories/products", Permission.PRODUCT_VIEW),
            Map.entry("/categories/new", Permission.PRODUCT_MANAGE),
            Map.entry("/categories/edit", Permission.PRODUCT_MANAGE),
            Map.entry("/categories/delete", Permission.PRODUCT_MANAGE),
            Map.entry("/categories/status", Permission.PRODUCT_MANAGE),
            Map.entry("/categories/products/move", Permission.PRODUCT_MANAGE),
            Map.entry("/price-lists", Permission.PRODUCT_VIEW),
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
            Map.entry("/units", Permission.PRODUCT_VIEW),
            Map.entry("/units/new", Permission.PRODUCT_MANAGE),
            Map.entry("/units/edit", Permission.PRODUCT_MANAGE),
            Map.entry("/units/delete", Permission.PRODUCT_MANAGE),
            Map.entry("/products/image", Permission.PRODUCT_VIEW),
            Map.entry("/products/new", Permission.PRODUCT_MANAGE),
            Map.entry("/products/edit", Permission.PRODUCT_MANAGE),
            Map.entry("/products/delete", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import/template", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import/preview", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import/result", Permission.PRODUCT_MANAGE),
            Map.entry("/products/import/errors", Permission.PRODUCT_MANAGE),
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
