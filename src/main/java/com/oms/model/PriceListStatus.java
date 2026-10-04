package com.oms.model;

import java.time.LocalDate;

// Trạng thái hiệu lực của bảng giá (S2-10), tính theo ngày hôm nay (giờ Việt Nam) chứ không lưu,
// để không cần tác vụ chạy định kỳ đổi trạng thái khi qua ngày
public enum PriceListStatus {
    UPCOMING("Sắp hiệu lực"),
    ACTIVE("Đang hiệu lực"),
    EXPIRED("Hết hạn");

    private final String label;

    PriceListStatus(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static PriceListStatus of(LocalDate validFrom, LocalDate validTo, LocalDate today) {
        if (today.isBefore(validFrom)) {
            return UPCOMING;
        }
        return validTo != null && today.isAfter(validTo) ? EXPIRED : ACTIVE;
    }
}
