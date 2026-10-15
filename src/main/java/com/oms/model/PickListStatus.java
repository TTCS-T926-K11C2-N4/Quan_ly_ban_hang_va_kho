package com.oms.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Trạng thái phiếu soạn hàng (S6-03), khớp ràng buộc CHECK của pick_lists.status:
// Chờ soạn → Đang soạn (đã ghi nhận ít nhất một lần) → Đã soạn (hoàn tất); huỷ khi chưa hoàn tất.
public final class PickListStatus {

    public static final String OPEN = "OPEN";
    public static final String PICKING = "PICKING";
    public static final String DONE = "DONE";
    public static final String CANCELLED = "CANCELLED";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(OPEN, "Chờ soạn");
        LABELS.put(PICKING, "Đang soạn");
        LABELS.put(DONE, "Đã soạn");
        LABELS.put(CANCELLED, "Đã huỷ");
    }

    // Lý do soạn thiếu / xuất thiếu dùng chung cho màn soạn hàng (S6-03) và xác nhận xuất kho (S6-05)
    public static final List<String> SHORT_REASONS = List.of("Hết hàng tại vị trí", "Không tìm thấy hàng",
            "Hàng hỏng, lỗi bao bì", "Hàng cận hạn sử dụng", "Lý do khác");

    private PickListStatus() {
    }

    public static boolean isShortReason(String reason) {
        return reason != null && SHORT_REASONS.contains(reason);
    }

    public static Map<String, String> labels() {
        return LABELS;
    }

    public static String label(String status) {
        return LABELS.getOrDefault(status, status);
    }

    public static boolean isKnown(String status) {
        return LABELS.containsKey(status);
    }

    // Còn ghi nhận soạn / huỷ được
    public static boolean isEditable(String status) {
        return OPEN.equals(status) || PICKING.equals(status);
    }

    // Tên class màu của nhãn trạng thái (pick-pill--*)
    public static String tone(String status) {
        return status == null ? "" : status.toLowerCase();
    }
}
