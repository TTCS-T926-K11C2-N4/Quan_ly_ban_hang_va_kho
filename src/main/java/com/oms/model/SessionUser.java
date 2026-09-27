package com.oms.model;

import com.oms.util.NameUtil;

// Thông tin người dùng đang đăng nhập, hiển thị ở sidebar và thẻ "Phạm vi truy cập"
public class SessionUser {

    private final String fullName;
    private final String roleName;
    private final String warehouseName;
    private final String areaName;

    public SessionUser(String fullName, String roleName, String warehouseName, String areaName) {
        this.fullName = fullName;
        this.roleName = roleName;
        this.warehouseName = warehouseName;
        this.areaName = areaName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getAreaName() {
        return areaName;
    }

    public String getInitials() {
        return NameUtil.initials(fullName);
    }
}
