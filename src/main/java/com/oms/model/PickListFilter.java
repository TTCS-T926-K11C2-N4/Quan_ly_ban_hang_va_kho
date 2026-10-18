package com.oms.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

// Bộ lọc màn phiếu soạn hàng (S6-03): từ khoá (mã phiếu, mã đơn, khách hàng, kho), kho, ngày giao áp cho cả
// hai bảng; trạng thái chỉ áp cho bảng phiếu soạn; "Đơn chờ soạn" (hạn giao) chỉ áp cho bảng đơn chờ soạn.
// Giá trị không hợp lệ coi như "Tất cả".
public class PickListFilter {

    public static final String DUE_OVERDUE = "OVERDUE";
    public static final String DUE_TODAY = "TODAY";
    public static final String DUE_UPCOMING = "UPCOMING";

    private static final Map<String, String> DUE_LABELS = new LinkedHashMap<>();

    static {
        DUE_LABELS.put(DUE_OVERDUE, "Đã quá ngày giao");
        DUE_LABELS.put(DUE_TODAY, "Giao hôm nay");
        DUE_LABELS.put(DUE_UPCOMING, "Giao các ngày tới");
    }

    private final String keyword;
    private final Long warehouseId;
    private final LocalDate deliveryDate;
    private final String status;
    private final String due;

    public PickListFilter(String keyword, Long warehouseId, String deliveryDate, String status, String due) {
        this.keyword = keyword == null ? "" : keyword.trim();
        this.warehouseId = warehouseId;
        this.deliveryDate = parseDate(deliveryDate);
        this.status = PickListStatus.isKnown(status) ? status : null;
        this.due = DUE_LABELS.containsKey(due) ? due : null;
    }

    private static LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static Map<String, String> dueLabels() {
        return DUE_LABELS;
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public String getStatus() {
        return status;
    }

    public String getDue() {
        return due;
    }

    public boolean isFiltered() {
        return !keyword.isEmpty() || warehouseId != null || deliveryDate != null || status != null || due != null;
    }
}
