package com.oms.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Bộ lọc danh sách đơn đang giữ chỗ tồn (S5-06): mã đơn hoặc đại lý, ngày duyệt, trạng thái đơn
// (mặc định Đã duyệt; Chờ duyệt vì đơn giữ chỗ ngay khi gửi, S4-03; ALL là mọi trạng thái),
// và SKU tại một kho khi mở từ nút "Xem" của sổ tồn kho.
public class ReservationFilter {

    public static final String ALL = "ALL";

    // Trạng thái đơn chọn được ở bộ lọc, theo thứ tự hiện
    public static final Map<String, String> STATUS_LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(OrderStatus.APPROVED, OrderStatus.label(OrderStatus.APPROVED));
        labels.put(OrderStatus.PENDING_APPROVAL, OrderStatus.label(OrderStatus.PENDING_APPROVAL));
        labels.put(OrderStatus.PICKING, OrderStatus.label(OrderStatus.PICKING));
        labels.put(ALL, "Tất cả trạng thái");
        STATUS_LABELS = Collections.unmodifiableMap(labels);
    }

    private final String keyword;
    private final LocalDate approvedDate;
    private final String status;
    private final Long productId;
    private final Long warehouseId;

    public ReservationFilter(String keyword, String approvedDate, String status, Long productId, Long warehouseId) {
        this.keyword = keyword == null ? "" : keyword;
        this.approvedDate = parseDate(approvedDate);
        this.status = STATUS_LABELS.containsKey(status) ? status : OrderStatus.APPROVED;
        this.productId = productId;
        this.warehouseId = warehouseId;
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

    public LocalDate getApprovedDate() {
        return approvedDate;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return STATUS_LABELS.get(status);
    }

    // Trạng thái đơn cần khớp; null là mọi trạng thái
    public String getOrderStatus() {
        return ALL.equals(status) ? null : status;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public boolean isProductFiltered() {
        return productId != null;
    }

    public boolean isFiltered() {
        return !keyword.isEmpty() || approvedDate != null || !OrderStatus.APPROVED.equals(status) || productId != null
                || warehouseId != null;
    }
}
