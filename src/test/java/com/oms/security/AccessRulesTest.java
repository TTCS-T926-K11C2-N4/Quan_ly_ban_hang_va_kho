package com.oms.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S1-05: kiểm quyền cho các vai trò, với quyền lấy đúng từ database/seed_permissions.sql (file nạp vào DB)
class AccessRulesTest {

    private static final Path SEED_FILE = Path.of("database", "seed_permissions.sql");

    private static Map<String, Set<String>> rolePermissions;

    @BeforeAll
    static void loadSeed() throws IOException {
        String sql = Files.readString(SEED_FILE, StandardCharsets.UTF_8);
        rolePermissions = new HashMap<>();

        // Chỉ dòng dữ liệu (bắt đầu bằng ROW), bỏ qua ví dụ trong phần chú thích đầu file
        Matcher row = Pattern.compile("(?m)^\\s*ROW\\('([A-Z_]+)', '([A-Z_]+)', '([A-Z]+)'\\)").matcher(sql);
        while (row.find()) {
            rolePermissions.computeIfAbsent(row.group(1), role -> new HashSet<>()).add(row.group(2));
        }

        // ADMIN: mọi quyền trừ COST_PRICE_VIEW (câu INSERT cuối file seed)
        Set<String> allPermissions = new HashSet<>();
        Matcher permission = Pattern.compile("(?m)^\\s*\\('([A-Z_]+)', '").matcher(sql);
        while (permission.find()) {
            allPermissions.add(permission.group(1));
        }
        allPermissions.remove("COST_PRICE_VIEW");
        rolePermissions.put("ADMIN", allPermissions);
    }

    private static Set<String> permissionsOf(String role) {
        return rolePermissions.getOrDefault(role, Set.of());
    }

    @Test
    void seedHasAllSevenRoles() {
        assertEquals(Set.of("CUSTOMER", "SALES_REP", "SALES_MANAGER", "WAREHOUSE", "WH_MANAGER", "ACCOUNTANT", "ADMIN"),
                rolePermissions.keySet());
    }

    @Test
    void salesRepCannotManageUsersOrChangeStock() {
        Set<String> salesRep = permissionsOf("SALES_REP");
        assertFalse(AccessRules.isAllowed("/accounts", salesRep));
        assertFalse(AccessRules.isAllowed("/accounts/new", salesRep));
        assertFalse(salesRep.contains("INVENTORY_MANAGE"), "Nhân viên kinh doanh không được sửa tồn kho");
        assertTrue(salesRep.contains("INVENTORY_VIEW"), "Nhân viên kinh doanh được xem tồn khả dụng");
    }

    @Test
    void warehouseCannotSeeCostPriceOrAccounts() {
        Set<String> warehouse = permissionsOf("WAREHOUSE");
        assertFalse(warehouse.contains("COST_PRICE_VIEW"), "Thủ kho không được xem giá vốn");
        assertFalse(AccessRules.isAllowed("/accounts", warehouse));
        assertTrue(warehouse.contains("INVENTORY_MANAGE"));
    }

    @Test
    void salesManagerCanViewButNotManageAccounts() {
        Set<String> salesManager = permissionsOf("SALES_MANAGER");
        assertTrue(AccessRules.isAllowed("/accounts", salesManager));
        assertTrue(AccessRules.isAllowed("/accounts/view", salesManager));
        assertFalse(AccessRules.isAllowed("/accounts/new", salesManager));
        assertFalse(AccessRules.isAllowed("/accounts/edit", salesManager));
        assertFalse(AccessRules.isAllowed("/accounts/lock", salesManager));
        assertFalse(AccessRules.isAllowed("/accounts/unlock", salesManager));
    }

    @Test
    void adminCanManageAccounts() {
        Set<String> admin = permissionsOf("ADMIN");
        for (String path : new String[] {"/accounts", "/accounts/view", "/accounts/new", "/accounts/edit",
                "/accounts/lock", "/accounts/unlock"}) {
            assertTrue(AccessRules.isAllowed(path, admin), path);
        }
    }

    @Test
    void customerAndAccountantCannotOpenAccounts() {
        assertFalse(AccessRules.isAllowed("/accounts", permissionsOf("CUSTOMER")));
        assertFalse(AccessRules.isAllowed("/accounts", permissionsOf("ACCOUNTANT")));
    }

    @Test
    void onlySalesManagerSeesCostPrice() {
        rolePermissions.forEach((role, permissions) ->
                assertEquals("SALES_MANAGER".equals(role), permissions.contains("COST_PRICE_VIEW"), role));
    }

    @Test
    void everyLoggedInUserCanOpenDashboardAndChangePassword() {
        assertTrue(AccessRules.isAllowed("/dashboard", Set.of()));
        assertTrue(AccessRules.isAllowed("/change-password", Set.of()));
    }

    // S2-02: mọi vai trò, kể cả Đại lý, đều tự xem và sửa được hồ sơ của mình
    @Test
    void everyLoggedInUserCanOpenOwnProfile() {
        assertTrue(AccessRules.isAllowed("/profile", Set.of()));
        assertTrue(AccessRules.isAllowed("/profile", permissionsOf("CUSTOMER")));
    }

    @Test
    void undeclaredPathIsDeniedEvenForAdmin() {
        assertFalse(AccessRules.isAllowed("/chuc-nang-moi", permissionsOf("ADMIN")));
    }

    @Test
    void authPagesAndAssetsArePublic() {
        for (String path : new String[] {"/login", "/logout", "/register", "/forgot-password",
                "/forgot-password/sent", "/reset-password", "/session-expired", "/error", "/assets/css/app.css"}) {
            assertTrue(AccessRules.isPublic(path), path);
        }
        assertFalse(AccessRules.isPublic("/dashboard"));
        assertFalse(AccessRules.isPublic("/accounts"));
    }
}
