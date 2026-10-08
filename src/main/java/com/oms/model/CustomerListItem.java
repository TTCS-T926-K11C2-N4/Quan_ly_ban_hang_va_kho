package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDateTime;

// Một dòng của danh sách đại lý (S3-08), kèm tên khu vực, nhóm khách hàng, người phụ trách để hiện ngay trong bảng
public class CustomerListItem {

    private final long id;
    private final String code;
    private final String name;
    private final String phone;
    private final String regionName;
    private final String customerGroupName;
    private final Long salesRepId;
    private final String salesRepName;
    private final LocalDateTime assignedAt;
    private final CustomerListStatus status;

    // assignedAt: lần phân công / chuyển giao gần nhất (UTC), null nếu chưa từng đổi người phụ trách
    public CustomerListItem(long id, String code, String name, String phone, String regionName,
                            String customerGroupName, Long salesRepId, String salesRepName, LocalDateTime assignedAt,
                            CustomerListStatus status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.regionName = regionName;
        this.customerGroupName = customerGroupName;
        this.salesRepId = salesRepId;
        this.salesRepName = salesRepName;
        this.assignedAt = assignedAt;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getCustomerGroupName() {
        return customerGroupName;
    }

    public Long getSalesRepId() {
        return salesRepId;
    }

    public String getSalesRepName() {
        return salesRepName;
    }

    public String getAssignedAtText() {
        return assignedAt == null ? null : DateTimeUtil.formatDate(assignedAt);
    }

    public CustomerListStatus getStatus() {
        return status;
    }
}
