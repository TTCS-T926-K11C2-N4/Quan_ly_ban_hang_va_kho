package com.oms.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

// S1-01: đăng nhập xong vào trang chủ tương ứng với vai trò
class SessionUserTest {

    private static SessionUser userWithRoles(String... roleCodes) {
        return new SessionUser(1, "u", "Người dùng", "", "", false, Set.of(roleCodes), Set.of());
    }

    @Test
    void customerGoesToPortal() {
        assertEquals("/portal", userWithRoles("CUSTOMER").getHomePath());
    }

    @Test
    void internalStaffGoesToDashboard() {
        for (String role : new String[] {"SALES_REP", "SALES_MANAGER", "WAREHOUSE", "WH_MANAGER", "ACCOUNTANT", "ADMIN"}) {
            assertEquals("/dashboard", userWithRoles(role).getHomePath(), role);
        }
        assertEquals("/dashboard", userWithRoles("SALES_REP", "WH_MANAGER").getHomePath());
    }

    // Tài khoản chưa được gán vai trò (vừa kích hoạt) vẫn vào trang nội bộ, chỉ thấy mục Tổng quan
    @Test
    void userWithoutRoleGoesToDashboard() {
        assertEquals("/dashboard", userWithRoles().getHomePath());
    }
}
