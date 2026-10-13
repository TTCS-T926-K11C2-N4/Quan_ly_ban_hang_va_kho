package com.oms.model;

// Tình trạng giữ chỗ của một đơn hoặc một dòng hàng (S5-06), tính từ các dòng stock_reservations:
// ACTIVE = đang giữ, RELEASED = đã nhả (huỷ đơn hoặc quá hạn), FULFILLED = đã xuất kho.
public enum ReservationState {
    HELD("Đã giữ chỗ", "Đang giữ chỗ", "success"),
    PARTIAL("Giữ chỗ một phần", "Giữ chỗ một phần", "warning"),
    EXPIRED("Hết hạn giữ chỗ", "Hết hạn giữ chỗ", "danger"),
    RELEASED("Đã nhả giữ chỗ", "Đã nhả giữ chỗ", "muted"),
    FULFILLED("Đã xuất kho", "Đã xuất kho", "muted"),
    NONE("Chưa giữ chỗ", "Chưa giữ chỗ", "muted");

    public static final String REASON_EXPIRED = "EXPIRED";
    public static final String REASON_ORDER_CANCELLED = "ORDER_CANCELLED";

    private final String label;
    private final String note;
    private final String tone;

    ReservationState(String label, String note, String tone) {
        this.label = label;
        this.note = note;
        this.tone = tone;
    }

    // Tổng hợp theo số dòng hàng của đơn: đang giữ, hết hạn, đã nhả (gồm hết hạn), đã xuất
    public static ReservationState of(int lines, int active, int expired, int released, int fulfilled) {
        if (active > 0) {
            return active == lines ? HELD : PARTIAL;
        }
        if (fulfilled > 0 && fulfilled == lines) {
            return FULFILLED;
        }
        if (expired > 0) {
            return EXPIRED;
        }
        return released > 0 ? RELEASED : NONE;
    }

    // Một dòng hàng: trạng thái giữ chỗ mới nhất của dòng đó
    public static ReservationState ofLine(String status, String releaseReason) {
        if (status == null) {
            return NONE;
        }
        return switch (status) {
            case "ACTIVE" -> HELD;
            case "FULFILLED" -> FULFILLED;
            default -> REASON_EXPIRED.equals(releaseReason) ? EXPIRED : RELEASED;
        };
    }

    public String getLabel() {
        return label;
    }

    // Chữ ở cột Ghi chú của danh sách đơn
    public String getNote() {
        return note;
    }

    // Hậu tố class màu (inv-pill--success | warning | danger | muted)
    public String getTone() {
        return tone;
    }
}
