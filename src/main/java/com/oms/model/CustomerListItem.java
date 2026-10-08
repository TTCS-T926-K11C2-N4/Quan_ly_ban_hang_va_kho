package com.oms.model;

// Một dòng của danh sách đại lý (S3-08), kèm tên khu vực, nhóm khách hàng, người phụ trách để hiện ngay trong bảng
public class CustomerListItem {

    private final long id;
    private final String code;
    private final String name;
    private final String phone;
    private final String regionName;
    private final String customerGroupName;
    private final String salesRepName;
    private final CustomerListStatus status;

    public CustomerListItem(long id, String code, String name, String phone, String regionName,
                            String customerGroupName, String salesRepName, CustomerListStatus status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.regionName = regionName;
        this.customerGroupName = customerGroupName;
        this.salesRepName = salesRepName;
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

    public String getSalesRepName() {
        return salesRepName;
    }

    public CustomerListStatus getStatus() {
        return status;
    }
}
