package com.oms.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Tình trạng hạn dùng của một lô (S6-01): quá hạn, sắp hết hạn (trong ngưỡng khai báo), còn hạn, hoặc không có hạn
public enum LotStatus {
    EXPIRED("Quá hạn", "danger"),
    NEAR("Sắp hết hạn", "warning"),
    OK("Còn hạn", "success"),
    NONE("Không có hạn", "neutral");

    private final String label;
    private final String tone;

    LotStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    // nearDays: lô còn không quá nearDays ngày (kể cả đúng ngày) là sắp hết hạn; hạn trong quá khứ là quá hạn
    public static LotStatus of(LocalDate expiryDate, LocalDate today, int nearDays) {
        if (expiryDate == null) {
            return NONE;
        }
        if (expiryDate.isBefore(today)) {
            return EXPIRED;
        }
        return ChronoUnit.DAYS.between(today, expiryDate) <= nearDays ? NEAR : OK;
    }

    // null nếu không khớp (bộ lọc "Tất cả")
    public static LotStatus fromCode(String code) {
        for (LotStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public String getTone() {
        return tone;
    }
}
