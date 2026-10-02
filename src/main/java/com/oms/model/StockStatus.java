package com.oms.model;

// Trạng thái hiện ở danh sách sản phẩm (S2-05): ngừng kinh doanh ưu tiên hơn tình trạng tồn kho.
// Sắp hết hàng = còn hàng nhưng không vượt quá tồn tối thiểu (stock_balances.min_stock_base).
public enum StockStatus {
    IN_STOCK("Còn hàng"),
    LOW_STOCK("Sắp hết hàng"),
    OUT_OF_STOCK("Hết hàng"),
    DISCONTINUED("Ngừng kinh doanh");

    private final String label;

    StockStatus(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    // null nếu không khớp (bộ lọc "Tất cả")
    public static StockStatus fromCode(String code) {
        for (StockStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
