package com.oms.model;

// Trạng thái hiện trên danh sách đại lý (S3-08): gộp customers.status với cờ khoá giao dịch (S3-07).
// Khoá giao dịch được ưu tiên như thẻ thông tin ở trang chi tiết, vì ảnh hưởng ngay tới việc bán hàng.
public enum CustomerListStatus {
    ACTIVE("Hoạt động"),
    BLOCKED("Đang khoá giao dịch"),
    INACTIVE("Ngừng giao dịch");

    private final String label;

    CustomerListStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // EL không gọi được name() của enum
    public String getCode() {
        return name();
    }

    public static CustomerListStatus of(boolean blocked, boolean active) {
        if (blocked) {
            return BLOCKED;
        }
        return active ? ACTIVE : INACTIVE;
    }

    public static CustomerListStatus fromCode(String code) {
        for (CustomerListStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
