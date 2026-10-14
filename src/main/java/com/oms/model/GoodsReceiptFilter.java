package com.oms.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Bộ lọc danh sách phiếu nhập (S5-04): mã phiếu / nhà cung cấp / số chứng từ, trạng thái, kho nhận, ngày nhập.
// Giá trị không hợp lệ coi như "Tất cả".
public class GoodsReceiptFilter {

    private final String keyword;
    private final String status;
    private final Long warehouseId;
    private final LocalDate receiptDate;

    public GoodsReceiptFilter(String keyword, String status, Long warehouseId, String receiptDate) {
        this.keyword = keyword == null ? "" : keyword;
        this.status = GoodsReceiptRow.STATUS_LABELS.containsKey(status) ? status : null;
        this.warehouseId = warehouseId;
        this.receiptDate = parseDate(receiptDate);
    }

    private static LocalDate parseDate(String value) {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public String getKeyword() {
        return keyword;
    }

    public String getStatus() {
        return status;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public boolean isFiltered() {
        return !keyword.isEmpty() || status != null || warehouseId != null || receiptDate != null;
    }
}
