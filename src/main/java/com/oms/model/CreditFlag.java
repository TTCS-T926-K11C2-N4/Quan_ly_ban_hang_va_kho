package com.oms.model;

import java.util.Map;

// Cờ công nợ tự động của đại lý (customers.credit_status, S7-04): hiển thị nhãn và màu trong danh sách, hồ sơ đại lý
public final class CreditFlag {

    private static final Map<String, String> LABELS = Map.of("OK", "Bình thường", "OVER_LIMIT", "Vượt hạn mức",
            "OVERDUE", "Quá hạn");
    private static final Map<String, String> TONES = Map.of("OK", "success", "OVER_LIMIT", "warning",
            "OVERDUE", "danger");

    private CreditFlag() {
    }

    // Mã lạ hoặc rỗng coi như bình thường để màn hình không lỗi
    public static String label(String code) {
        return LABELS.getOrDefault(normalize(code), LABELS.get("OK"));
    }

    // Map.of không nhận khoá null
    private static String normalize(String code) {
        return code == null ? "OK" : code;
    }

    public static String tone(String code) {
        return TONES.getOrDefault(normalize(code), TONES.get("OK"));
    }
}
