package com.oms.model;

import java.math.BigDecimal;

// Đại lý (bảng customers) ở mức các màn hạn mức công nợ, khoá giao dịch và tạo đơn cần.
// Hồ sơ đầy đủ (thêm, sửa, nhóm, khu vực) thuộc S3-03.
public class Customer {

    public static final String ACTIVE = "ACTIVE";

    private final long id;
    private final String code;
    private final String name;
    private final String phone;
    private final String address;
    private final String contactName;
    private final Long salesRepId;
    private final long customerGroupId;
    private final Long defaultWarehouseId;
    private final BigDecimal creditLimit;
    private final int maxDebtDays;
    private final boolean blocked;
    private final String blockReason;
    private final String status;
    private final long version;

    public Customer(long id, String code, String name, String phone, String address, String contactName,
                    Long salesRepId, long customerGroupId, Long defaultWarehouseId, BigDecimal creditLimit,
                    int maxDebtDays, boolean blocked, String blockReason, String status, long version) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.contactName = contactName;
        this.salesRepId = salesRepId;
        this.customerGroupId = customerGroupId;
        this.defaultWarehouseId = defaultWarehouseId;
        this.creditLimit = creditLimit;
        this.maxDebtDays = maxDebtDays;
        this.blocked = blocked;
        this.blockReason = blockReason;
        this.status = status;
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

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    // customers chưa có cột người liên hệ: lấy người nhận ở điểm giao mặc định; null nếu chưa khai báo
    public String getContactName() {
        return contactName;
    }

    public Long getSalesRepId() {
        return salesRepId;
    }

    public long getCustomerGroupId() {
        return customerGroupId;
    }

    public Long getDefaultWarehouseId() {
        return defaultWarehouseId;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public int getMaxDebtDays() {
        return maxDebtDays;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public long getVersion() {
        return version;
    }

    // Trạng thái hiện trên thẻ thông tin: khoá giao dịch được ưu tiên vì ảnh hưởng ngay tới việc bán hàng
    public String getStatusLabel() {
        if (blocked) {
            return "Đang khoá giao dịch";
        }
        return isActive() ? "Đang hoạt động" : "Ngừng giao dịch";
    }
}
