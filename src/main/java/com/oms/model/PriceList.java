package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDate;

// Một bảng giá (price_lists) kèm tên nhóm khách hàng và cờ đã có đơn dùng
public class PriceList {

    private final long id;
    private final String code;
    private final String name;
    private final long customerGroupId;
    private final String customerGroupName;
    private final int versionNo;
    private final Long previousVersionId;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final boolean locked;
    private final int itemCount;

    public PriceList(long id, String code, String name, long customerGroupId, String customerGroupName,
                     int versionNo, Long previousVersionId, LocalDate validFrom, LocalDate validTo, boolean locked,
                     int itemCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.customerGroupId = customerGroupId;
        this.customerGroupName = customerGroupName;
        this.versionNo = versionNo;
        this.previousVersionId = previousVersionId;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.locked = locked;
        this.itemCount = itemCount;
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

    public long getCustomerGroupId() {
        return customerGroupId;
    }

    public String getCustomerGroupName() {
        return customerGroupName;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public Long getPreviousVersionId() {
        return previousVersionId;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public String getValidFromText() {
        return DateTimeUtil.formatDay(validFrom);
    }

    public String getValidToText() {
        return validTo == null ? "—" : DateTimeUtil.formatDay(validTo);
    }

    // Đã có đơn hàng dùng giá của bảng này: không sửa/xoá, chỉ tạo phiên bản mới (S2-10)
    public boolean isLocked() {
        return locked;
    }

    public int getItemCount() {
        return itemCount;
    }

    public PriceListStatus getStatus() {
        return PriceListStatus.of(validFrom, validTo, DateTimeUtil.today());
    }
}
