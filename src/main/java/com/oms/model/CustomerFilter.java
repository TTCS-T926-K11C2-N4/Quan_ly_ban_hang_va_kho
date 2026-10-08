package com.oms.model;

// Điều kiện tìm kiếm, lọc danh sách đại lý (S3-08); null nghĩa là không lọc theo tiêu chí đó
public class CustomerFilter {

    private final String keyword;
    private final Long regionId;
    private final Long customerGroupId;
    private final Long salesRepId;
    private final CustomerListStatus status;

    public CustomerFilter(String keyword, Long regionId, Long customerGroupId, Long salesRepId,
                          CustomerListStatus status) {
        this.keyword = keyword;
        this.regionId = regionId;
        this.customerGroupId = customerGroupId;
        this.salesRepId = salesRepId;
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

    public CustomerListStatus getStatus() {
        return status;
    }

    // Có ít nhất một điều kiện tìm kiếm/lọc (tên không dùng "empty" vì đó là từ khoá của EL)
    public boolean isFiltered() {
        return keyword != null || regionId != null || customerGroupId != null || salesRepId != null || status != null;
    }
}
