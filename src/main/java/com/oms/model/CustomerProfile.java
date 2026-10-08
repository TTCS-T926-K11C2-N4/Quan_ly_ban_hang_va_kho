package com.oms.model;

// Hồ sơ đại lý đầy đủ cho trang Chi tiết đại lý, tab Hồ sơ (S3-03). hasTransactions: đại lý đã có đơn hàng,
// hoá đơn, phiếu thu, công nợ, tài khoản đại lý hoặc lịch sử hạn mức / khoá / phân công -> không xoá được.
public class CustomerProfile {

    private final long id;
    private final String code;
    private final String name;
    private final String taxCode;
    private final String phone;
    private final String email;
    private final String address;
    private final long customerGroupId;
    private final String customerGroupName;
    private final long regionId;
    private final String regionName;
    private final Long salesRepId;
    private final String salesRepName;
    private final Long defaultWarehouseId;
    private final String defaultWarehouseName;
    private final String status;
    private final boolean blocked;
    private final boolean hasTransactions;
    private final long version;

    public CustomerProfile(long id, String code, String name, String taxCode, String phone, String email,
                           String address, long customerGroupId, String customerGroupName, long regionId,
                           String regionName, Long salesRepId, String salesRepName, Long defaultWarehouseId,
                           String defaultWarehouseName, String status, boolean blocked, boolean hasTransactions,
                           long version) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.taxCode = taxCode;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.customerGroupId = customerGroupId;
        this.customerGroupName = customerGroupName;
        this.regionId = regionId;
        this.regionName = regionName;
        this.salesRepId = salesRepId;
        this.salesRepName = salesRepName;
        this.defaultWarehouseId = defaultWarehouseId;
        this.defaultWarehouseName = defaultWarehouseName;
        this.status = status;
        this.blocked = blocked;
        this.hasTransactions = hasTransactions;
        this.version = version;
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

    public String getTaxCode() {
        return taxCode;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public long getCustomerGroupId() {
        return customerGroupId;
    }

    public String getCustomerGroupName() {
        return customerGroupName;
    }

    public long getRegionId() {
        return regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public Long getSalesRepId() {
        return salesRepId;
    }

    public String getSalesRepName() {
        return salesRepName;
    }

    public Long getDefaultWarehouseId() {
        return defaultWarehouseId;
    }

    public String getDefaultWarehouseName() {
        return defaultWarehouseName;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return Customer.ACTIVE.equals(status);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isHasTransactions() {
        return hasTransactions;
    }

    public long getVersion() {
        return version;
    }

    // Trạng thái hiện trên thẻ: khoá giao dịch (S3-07) ưu tiên hơn trạng thái hồ sơ, giống danh sách đại lý
    public CustomerListStatus getDisplayStatus() {
        return CustomerListStatus.of(blocked, isActive());
    }
}
