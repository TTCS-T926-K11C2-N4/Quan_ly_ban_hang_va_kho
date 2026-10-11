package com.oms.model;

// Điều kiện tìm kiếm, lọc danh sách đại lý (S3-08); null nghĩa là không lọc theo tiêu chí đó
public class CustomerFilter {

    private final String keyword;
    private final Long regionId;
    private final Long customerGroupId;
    private final Long salesRepId;
    private final boolean unassigned;
    private final CustomerListStatus status;

    public CustomerFilter(String keyword, Long regionId, Long customerGroupId, Long salesRepId,
                          CustomerListStatus status) {
        this(keyword, regionId, customerGroupId, salesRepId, false, status);
    }

    // unassigned = true: chỉ đại lý chưa có người phụ trách (màn phân công S3-06); khi đó bỏ qua salesRepId
    public CustomerFilter(String keyword, Long regionId, Long customerGroupId, Long salesRepId, boolean unassigned,
                          CustomerListStatus status) {
        this.keyword = keyword;
        this.regionId = regionId;
        this.customerGroupId = customerGroupId;
        this.salesRepId = unassigned ? null : salesRepId;
        this.unassigned = unassigned;
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getRegionId() {
        return regionId;
    }

    public Long getCustomerGroupId() {
        return customerGroupId;
    }

    public Long getSalesRepId() {
        return salesRepId;
    }

    public boolean isUnassigned() {
        return unassigned;
    }

    public CustomerListStatus getStatus() {
        return status;
    }

    // Có ít nhất một điều kiện tìm kiếm/lọc (tên không dùng "empty" vì đó là từ khoá của EL)
    public boolean isFiltered() {
        return keyword != null || regionId != null || customerGroupId != null || salesRepId != null || unassigned
                || status != null;
    }
}
