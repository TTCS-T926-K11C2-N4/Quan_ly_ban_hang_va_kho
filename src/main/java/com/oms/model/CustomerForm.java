package com.oms.model;

// Dữ liệu form thêm/sửa hồ sơ đại lý (S3-03), giữ nguyên chữ người dùng gõ để hiện lại khi có lỗi.
// id = null: thêm đại lý mới. version: chặn hai người cùng sửa ghi đè nhau.
public class CustomerForm {

    private final Long id;
    private final String code;
    private final String name;
    private final String taxCode;
    private final String phone;
    private final String email;
    private final String address;
    private final String customerGroupId;
    private final String regionId;
    private final String salesRepId;
    private final String defaultWarehouseId;
    private final String status;
    private final String version;

    public CustomerForm(Long id, String code, String name, String taxCode, String phone, String email, String address,
                        String customerGroupId, String regionId, String salesRepId, String defaultWarehouseId,
                        String status, String version) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.taxCode = taxCode;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.customerGroupId = customerGroupId;
        this.regionId = regionId;
        this.salesRepId = salesRepId;
        this.defaultWarehouseId = defaultWarehouseId;
        this.status = status;
        this.version = version;
    }

    // Form trống, mã đại lý gợi ý sẵn mã kế tiếp
    public static CustomerForm empty(String suggestedCode) {
        return new CustomerForm(null, suggestedCode, null, null, null, null, null, null, null, null, null,
                Customer.ACTIVE, null);
    }

    public static CustomerForm of(CustomerProfile profile) {
        return new CustomerForm(profile.getId(), profile.getCode(), profile.getName(), profile.getTaxCode(),
                profile.getPhone(), profile.getEmail(), profile.getAddress(), String.valueOf(profile.getCustomerGroupId()),
                String.valueOf(profile.getRegionId()),
                profile.getSalesRepId() == null ? null : String.valueOf(profile.getSalesRepId()),
                profile.getDefaultWarehouseId() == null ? null : String.valueOf(profile.getDefaultWarehouseId()),
                profile.getStatus(), String.valueOf(profile.getVersion()));
    }

    public Long getId() {
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

    public String getCustomerGroupId() {
        return customerGroupId;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getSalesRepId() {
        return salesRepId;
    }

    public String getDefaultWarehouseId() {
        return defaultWarehouseId;
    }

    public String getStatus() {
        return status;
    }

    public String getVersion() {
        return version;
    }
}
