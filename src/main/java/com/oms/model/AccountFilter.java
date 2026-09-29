package com.oms.model;

// Điều kiện lọc danh sách tài khoản; null nghĩa là không lọc theo tiêu chí đó
public class AccountFilter {

    private final String keyword;
    private final String roleCode;
    private final AccountStatus status;

    public AccountFilter(String keyword, String roleCode, AccountStatus status) {
        this.keyword = keyword;
        this.roleCode = roleCode;
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
