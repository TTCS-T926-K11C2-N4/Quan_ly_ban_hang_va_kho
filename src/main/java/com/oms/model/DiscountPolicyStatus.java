package com.oms.model;

import java.time.LocalDate;

// Trạng thái chính sách chiết khấu (S3-01): công tắc "đang áp dụng" (is_active) cùng thời gian hiệu lực,
// tính theo ngày hôm nay chứ không lưu, giống trạng thái bảng giá
public enum DiscountPolicyStatus {
    ACTIVE("Đang áp dụng"),
    UPCOMING("Sắp áp dụng"),
    EXPIRED("Hết hạn"),
    INACTIVE("Ngừng áp dụng");

    private final String label;

    DiscountPolicyStatus(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static DiscountPolicyStatus of(boolean active, LocalDate validFrom, LocalDate validTo, LocalDate today) {
        if (!active) {
            return INACTIVE;
        }
        if (today.isBefore(validFrom)) {
            return UPCOMING;
        }
        return validTo != null && today.isAfter(validTo) ? EXPIRED : ACTIVE;
    }

    public static DiscountPolicyStatus fromCode(String code) {
        for (DiscountPolicyStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
