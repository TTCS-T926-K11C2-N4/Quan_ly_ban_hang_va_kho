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

        // ADMIN: mọi quyền trừ COST_PRICE_VIEW và CREDIT_MANAGE (câu INSERT cuối file seed)
        Set<String> allPermissions = new HashSet<>();
        Matcher permission = Pattern.compile("(?m)^\\s*\\('([A-Z_]+)', '").matcher(sql);
        while (permission.find()) {
            allPermissions.add(permission.group(1));
        }
        allPermissions.remove("COST_PRICE_VIEW");
        allPermissions.remove("CREDIT_MANAGE");
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
                "/accounts/lock", "/accounts/unlock", "/accounts/import", "/accounts/import/template",
                "/accounts/import/preview", "/accounts/import/result", "/accounts/import/errors"}) {
            assertTrue(AccessRules.isAllowed(path, admin), path);
        }
    }

    // S2-02: ai đã đăng nhập cũng xem và sửa được hồ sơ của chính mình
    @Test
    void everyRoleCanOpenOwnProfile() {
        for (String role : new String[] {"CUSTOMER", "SALES_REP", "SALES_MANAGER", "WAREHOUSE", "WH_MANAGER",
                "ACCOUNTANT", "ADMIN"}) {
            assertTrue(AccessRules.isAllowed("/profile", permissionsOf(role)), role);
        }
    }

    // S2-01: nhập người dùng từ Excel tạo tài khoản hàng loạt nên cần quyền quản lý tài khoản
    @Test
    void onlyUserManagersCanImportAccounts() {
        Set<String> salesManager = permissionsOf("SALES_MANAGER");
        for (String path : new String[] {"/accounts/import", "/accounts/import/template", "/accounts/import/preview",
                "/accounts/import/result", "/accounts/import/errors"}) {
            assertFalse(AccessRules.isAllowed(path, salesManager), path);
            assertFalse(AccessRules.isAllowed(path, permissionsOf("WAREHOUSE")), path);
        }
    }

    // S2-04: chỉ Quản trị hệ thống xem và xuất nhật ký thao tác
    @Test
    void onlyAdminCanViewAuditLogs() {
        for (String path : new String[] {"/audit-logs", "/audit-logs/export"}) {
            assertTrue(AccessRules.isAllowed(path, permissionsOf("ADMIN")), path);
            for (String role : new String[] {"CUSTOMER", "SALES_REP", "SALES_MANAGER", "WAREHOUSE", "WH_MANAGER",
                    "ACCOUNTANT"}) {
                assertFalse(AccessRules.isAllowed(path, permissionsOf(role)), role + " " + path);
            }
        }
    }

    // S2-07: vai trò nội bộ xem được đơn vị tính; chỉ Quản lý kinh doanh và Admin thêm/sửa/xoá
    @Test
    void onlyProductManagersCanChangeUnits() {
        for (String path : new String[] {"/units/new", "/units/edit", "/units/delete"}) {
            assertTrue(AccessRules.isAllowed(path, permissionsOf("SALES_MANAGER")), path);
            assertTrue(AccessRules.isAllowed(path, permissionsOf("ADMIN")), path);
            assertFalse(AccessRules.isAllowed(path, permissionsOf("WAREHOUSE")), path);
        }
        assertTrue(AccessRules.isAllowed("/units", permissionsOf("WAREHOUSE")));
    }

    // S2-09: ai xem kho thì xem được nhà cung cấp; chỉ nhân viên kho, quản lý kho, Admin thêm/sửa/xoá/ngừng giao dịch
    @Test
    void onlyWarehouseStaffCanManageSuppliers() {
        for (String path : new String[] {"/suppliers/new", "/suppliers/edit", "/suppliers/status", "/suppliers/delete"}) {
            for (String role : new String[] {"WAREHOUSE", "WH_MANAGER", "ADMIN"}) {
                assertTrue(AccessRules.isAllowed(path, permissionsOf(role)), role + " " + path);
            }
            for (String role : new String[] {"SALES_REP", "SALES_MANAGER", "ACCOUNTANT", "CUSTOMER"}) {
                assertFalse(AccessRules.isAllowed(path, permissionsOf(role)), role + " " + path);
            }
        }
        assertTrue(AccessRules.isAllowed("/suppliers", permissionsOf("ACCOUNTANT")));
        assertFalse(AccessRules.isAllowed("/suppliers", permissionsOf("CUSTOMER")));
    }

    // S2-08: nhập sản phẩm từ Excel thêm và sửa sản phẩm nên chỉ ai quản lý sản phẩm mới vào được
    @Test
    void onlyProductManagersCanImportProducts() {
        for (String path : new String[] {"/products/import", "/products/import/template", "/products/import/preview",
                "/products/import/result", "/products/import/errors"}) {
            assertTrue(AccessRules.isAllowed(path, permissionsOf("SALES_MANAGER")), path);
            assertTrue(AccessRules.isAllowed(path, permissionsOf("ADMIN")), path);
            for (String role : new String[] {"CUSTOMER", "SALES_REP", "WAREHOUSE", "WH_MANAGER", "ACCOUNTANT"}) {
                assertFalse(AccessRules.isAllowed(path, permissionsOf(role)), role + " " + path);
            }
        }
    }

    // S2-10: vai trò nội bộ xem được bảng giá; chỉ Quản lý kinh doanh và Admin tạo/sửa/xoá/tạo phiên bản
    @Test
    void onlyProductManagersCanChangePriceLists() {
        String[] managePaths = {"/price-lists/new", "/price-lists/edit", "/price-lists/version", "/price-lists/delete"};
        for (String path : managePaths) {
            assertTrue(AccessRules.isAllowed(path, permissionsOf("SALES_MANAGER")), path);
            assertTrue(AccessRules.isAllowed(path, permissionsOf("ADMIN")), path);
            assertFalse(AccessRules.isAllowed(path, permissionsOf("SALES_REP")), path);
            assertFalse(AccessRules.isAllowed(path, permissionsOf("WAREHOUSE")), path);
        }
        assertTrue(AccessRules.isAllowed("/price-lists", permissionsOf("SALES_REP")));
    }

    // S2-05: mọi vai trò nội bộ xem được danh mục sản phẩm; chỉ Quản lý kinh doanh và Admin thêm/sửa/xoá/ngừng
    @Test
    void onlyProductManagersCanChangeProducts() {
        String[] managePaths = {"/products/new", "/products/edit", "/products/delete", "/products/status"};
        for (String role : new String[] {"SALES_MANAGER", "ADMIN"}) {
            for (String path : managePaths) {
                assertTrue(AccessRules.isAllowed(path, permissionsOf(role)), role + " " + path);
            }
        }
        for (String role : new String[] {"SALES_REP", "WAREHOUSE", "WH_MANAGER", "ACCOUNTANT"}) {
            assertTrue(AccessRules.isAllowed("/products", permissionsOf(role)), role);
            assertTrue(AccessRules.isAllowed("/products/image", permissionsOf(role)), role);
            for (String path : managePaths) {
                assertFalse(AccessRules.isAllowed(path, permissionsOf(role)), role + " " + path);
            }
        }
    }

    // S2-05: giá vốn chỉ Quản lý kinh doanh xem và sửa được, Admin cũng không
    @Test
    void onlySalesManagerCanSeeCostPriceOfProducts() {
        assertTrue(permissionsOf("SALES_MANAGER").contains("COST_PRICE_VIEW"));
        assertFalse(permissionsOf("ADMIN").contains("COST_PRICE_VIEW"));
    }

    // S2-06: mọi vai trò nội bộ xem được nhóm hàng; chỉ Quản lý kinh doanh và Admin được thêm/sửa/xoá/chuyển
    @Test
    void onlyProductManagersCanChangeCategories() {
        String[] managePaths = {"/categories/new", "/categories/edit", "/categories/delete", "/categories/status",
                "/categories/products/move"};
        for (String role : new String[] {"SALES_MANAGER", "ADMIN"}) {
            assertTrue(AccessRules.isAllowed("/categories", permissionsOf(role)), role);
            for (String path : managePaths) {
                assertTrue(AccessRules.isAllowed(path, permissionsOf(role)), role + " " + path);
            }
        }
        for (String role : new String[] {"SALES_REP", "WAREHOUSE", "WH_MANAGER", "ACCOUNTANT"}) {
            assertTrue(AccessRules.isAllowed("/categories", permissionsOf(role)), role);
            assertTrue(AccessRules.isAllowed("/categories/products", permissionsOf(role)), role);
            for (String path : managePaths) {
                assertFalse(AccessRules.isAllowed(path, permissionsOf(role)), role + " " + path);
            }
        }
    }

    // S3-07: chỉ Kế toán công nợ và Quản lý kinh doanh khoá/mở giao dịch đại lý
    @Test
    void onlyAccountantAndSalesManagerCanBlockCustomers() {
        rolePermissions.forEach((role, permissions) -> assertEquals(
                "ACCOUNTANT".equals(role) || "SALES_MANAGER".equals(role),
                AccessRules.isAllowed("/customers/block/save", permissions), role));
        assertTrue(AccessRules.isAllowed("/customers/block", permissionsOf("SALES_REP")));
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

    @Test
    void undeclaredPathIsDeniedEvenForAdmin() {
        assertFalse(AccessRules.isAllowed("/chuc-nang-moi", permissionsOf("ADMIN")));
    }

    @Test
    void authPagesAndAssetsArePublic() {
        for (String path : new String[] {"/login", "/logout", "/forgot-password",
                "/forgot-password/sent", "/reset-password", "/session-expired", "/error", "/assets/css/app.css"}) {
            assertTrue(AccessRules.isPublic(path), path);
        }
        assertFalse(AccessRules.isPublic("/dashboard"));
        assertFalse(AccessRules.isPublic("/accounts"));
    }
}
