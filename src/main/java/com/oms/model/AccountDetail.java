package com.oms.model;

import com.oms.util.DateTimeUtil;
import com.oms.util.NameUtil;

import java.time.LocalDateTime;
import java.util.List;

// Dữ liệu trang Chi tiết tài khoản (S1-10A)
public class AccountDetail {

    private final long id;
    private final String fullName;
    private final String username;
    private final String email;
    private final String phone;
    private final AccountStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLoginAt;
    private final List<Role> roles;
    private final List<String> warehouseNames;
    private final List<String> regionNames;
    private final int customerCount;
    private final AuditLogEntry latestActivity;

    public AccountDetail(long id, String fullName, String username, String email, String phone,
                         AccountStatus status, LocalDateTime createdAt, LocalDateTime lastLoginAt,
                         List<Role> roles, List<String> warehouseNames, List<String> regionNames,
                         int customerCount, AuditLogEntry latestActivity) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.roles = roles;
        this.warehouseNames = warehouseNames;
        this.regionNames = regionNames;
        this.customerCount = customerCount;
        this.latestActivity = latestActivity;
    }

    public long getId() {
        return id;
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

    public AccountStatus getStatus() {
        return status;
    }

    public String getInitials() {
        return NameUtil.initials(fullName);
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDate(createdAt);
    }

    public String getLastLoginAtText() {
        return DateTimeUtil.formatDateTime(lastLoginAt);
    }

    public List<Role> getRoles() {
        return roles;
    }

    public String getWarehouseNamesText() {
        return String.join(", ", warehouseNames);
    }

    public String getRegionNamesText() {
        return String.join(", ", regionNames);
    }

    public int getCustomerCount() {
        return customerCount;
    }

    public AuditLogEntry getLatestActivity() {
        return latestActivity;
    }

    // Đang phụ trách địa bàn hoặc đại lý thì phải bàn giao cho người khác trước khi khóa
    public boolean isHandoverRequired() {
        return !regionNames.isEmpty() || customerCount > 0;
    }
}
