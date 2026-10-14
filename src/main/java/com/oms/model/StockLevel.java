package com.oms.model;

import java.math.BigDecimal;

// Trạng thái tồn ở sổ tồn kho (S5-05), tính theo tồn khả dụng (thực tế - giữ chỗ) của SKU tại một kho:
// hết hàng khi khả dụng <= 0; sắp hết khi có khai báo tồn tối thiểu và khả dụng không vượt quá mức đó.
public enum StockLevel {
    OUT("Hết hàng", "danger"),
    LOW("Sắp hết", "warning"),
    IN("Còn hàng", "success");

    private final String label;
    private final String tone;

    StockLevel(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public static StockLevel of(BigDecimal available, BigDecimal minStock) {
        if (available.signum() <= 0) {
            return OUT;
        }
        return minStock.signum() > 0 && available.compareTo(minStock) <= 0 ? LOW : IN;
    }

    // null nếu không khớp (bộ lọc "Tất cả")
    public static StockLevel fromCode(String code) {
        for (StockLevel level : values()) {
            if (level.name().equals(code)) {
                return level;
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

    // Hậu tố class màu của nhãn (inv-pill--danger | warning | success)
    public String getTone() {
        return tone;
    }
}
