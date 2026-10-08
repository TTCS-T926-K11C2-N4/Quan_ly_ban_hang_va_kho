package com.oms.model;

import java.time.LocalDate;

// Điều kiện lọc lịch sử thay đổi giá (S3-02); null nghĩa là không lọc theo tiêu chí đó.
// from / to lọc theo ngày bắt đầu áp dụng giá mới.
public class PriceHistoryFilter {

    private final String keyword;
    private final String categoryPath;
    private final Long customerGroupId;
    private final Long priceListId;
    private final PriceListStatus status;
    private final LocalDate from;
    private final LocalDate to;

    public PriceHistoryFilter(String keyword, String categoryPath, Long customerGroupId, Long priceListId,
                              PriceListStatus status, LocalDate from, LocalDate to) {
        this.keyword = keyword;
        this.categoryPath = categoryPath;
        this.customerGroupId = customerGroupId;
        this.priceListId = priceListId;
        this.status = status;
        this.from = from;
        this.to = to;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public Long getCustomerGroupId() {
        return customerGroupId;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public PriceListStatus getStatus() {
        return status;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }
}
