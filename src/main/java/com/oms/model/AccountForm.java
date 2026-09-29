package com.oms.model;

import java.util.List;

// Dữ liệu người dùng nhập ở form tạo tài khoản (đã trim), dùng để kiểm tra và hiển thị lại khi có lỗi
public class AccountForm {

    private final String fullName;
    private final String username;
    private final String email;
    private final String phone;
    private final List<String> roleCodes;
    private final Long warehouseId;
    private final Long regionId;

    public AccountForm(String fullName, String username, String email, String phone,
                       List<String> roleCodes, Long warehouseId, Long regionId) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.roleCodes = roleCodes;
        this.warehouseId = warehouseId;
        this.regionId = regionId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public boolean hasRole(String code) {
        return roleCodes.contains(code);
    }
}
