package com.oms.model;

import java.util.LinkedHashMap;
import java.util.Map;

// Trạng thái phiếu xuất kho (khớp CHECK của delivery_notes.status) và cách xử lý phần xuất thiếu (S6-05).
// Phiếu lập lúc xác nhận xuất kho ở trạng thái Đã xuất (ISSUED); giao xong là DELIVERED, giao không thành công
// là FAILED (S6-08).
public final class DeliveryNoteStatus {

    public static final String ISSUED = "ISSUED";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";

    // Xử lý phần thiếu: giao bổ sung sau (phần thiếu thành hàng còn nợ) / đóng phần thiếu (không giao nữa)
    public static final String BACKORDER = "BACKORDER";
    public static final String CLOSE = "CLOSE";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    private static final Map<String, String> SHORTAGE_LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(ISSUED, "Chờ giao");
        LABELS.put(IN_TRANSIT, "Đang giao");
        LABELS.put(DELIVERED, "Đã giao");
        LABELS.put(FAILED, "Giao không thành công");
        SHORTAGE_LABELS.put(BACKORDER, "Giao bổ sung sau");
        SHORTAGE_LABELS.put(CLOSE, "Đóng phần thiếu");
    }

    private DeliveryNoteStatus() {
    }

    public static String label(String status) {
        return LABELS.getOrDefault(status, status);
    }

    // Tên class màu của nhãn trạng thái (delivery-pill--*)
    public static String tone(String status) {
        return status == null ? "" : status.toLowerCase().replace('_', '-');
    }

    // Còn chờ xác nhận giao (S6-08)
    public static boolean isPending(String status) {
        return ISSUED.equals(status) || IN_TRANSIT.equals(status);
    }

    public static Map<String, String> shortageLabels() {
        return SHORTAGE_LABELS;
    }

    public static String shortageLabel(String action) {
        return action == null ? null : SHORTAGE_LABELS.getOrDefault(action, action);
    }

    public static boolean isShortageAction(String action) {
        return SHORTAGE_LABELS.containsKey(action);
    }
}
