package com.oms.model;

import java.time.LocalDate;

// Bộ lọc thẻ kho (S6-02): sản phẩm bắt buộc; kho và khoảng ngày (theo giờ Việt Nam) tuỳ chọn
public class StockCardFilter {

    private final Long productId;
    private final Long warehouseId;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;

    // Khoảng ngày bị đảo thì đổi chỗ để vẫn có kết quả
    public StockCardFilter(Long productId, Long warehouseId, LocalDate dateFrom, LocalDate dateTo) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            this.dateFrom = dateTo;
            this.dateTo = dateFrom;
        } else {
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
        }
    }

    public Long getProductId() {
        return productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public String getDateFromText() {
        return dateFrom == null ? "" : dateFrom.toString();
    }

    public String getDateToText() {
        return dateTo == null ? "" : dateTo.toString();
    }
}
