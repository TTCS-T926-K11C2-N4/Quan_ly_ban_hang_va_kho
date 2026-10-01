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
            "/login", "/logout", "/register", "/forgot-password", "/forgot-password/sent", "/reset-password",
            "/session-expired", "/error");

    private static final Map<String, String> REQUIRED_PERMISSIONS = Map.ofEntries(
            Map.entry("/dashboard", AUTHENTICATED),
            Map.entry("/portal", AUTHENTICATED),
            Map.entry("/change-password", AUTHENTICATED),
            Map.entry("/profile", AUTHENTICATED),
            Map.entry("/accounts", Permission.USER_VIEW),
            Map.entry("/accounts/view", Permission.USER_VIEW),
            Map.entry("/accounts/new", Permission.USER_MANAGE),
            Map.entry("/accounts/edit", Permission.USER_MANAGE),
            Map.entry("/accounts/lock", Permission.USER_MANAGE),
            Map.entry("/accounts/unlock", Permission.USER_MANAGE));

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
