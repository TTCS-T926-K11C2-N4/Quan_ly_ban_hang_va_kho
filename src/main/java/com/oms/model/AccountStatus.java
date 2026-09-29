package com.oms.model;

// Trạng thái hiển thị: gộp users.status với users.locked_until (khóa tạm sau nhiều lần sai mật khẩu)
public enum AccountStatus {
    ACTIVE("Hoạt động"),
    TEMP_LOCKED("Tạm khóa"),
    LOCKED("Bị khóa"),
    PENDING("Chờ kích hoạt");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // EL không gọi được name() của enum
    public String getCode() {
        return name();
    }

    public boolean isLocked() {
        return this == LOCKED || this == TEMP_LOCKED;
    }

    public static AccountStatus fromCode(String code) {
        for (AccountStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
