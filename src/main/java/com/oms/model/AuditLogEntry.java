package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDateTime;
import java.util.Map;

public class AuditLogEntry {

    // Mã thao tác ghi vào audit_logs.action cho entity_type = USER
    public static final String USER_REGISTER = "USER_REGISTER";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_ACTIVATE = "USER_ACTIVATE";
    public static final String USER_LOCK = "USER_LOCK";
    public static final String USER_UNLOCK = "USER_UNLOCK";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";

    private static final Map<String, String> LABELS = Map.of(
            USER_REGISTER, "Đăng ký tài khoản",
            USER_CREATE, "Tạo tài khoản",
            USER_UPDATE, "Cập nhật thông tin tài khoản",
            USER_ACTIVATE, "Kích hoạt tài khoản",
            USER_LOCK, "Khóa tài khoản",
            USER_UNLOCK, "Mở khóa tài khoản",
            PASSWORD_CHANGE, "Đổi mật khẩu",
            PASSWORD_RESET, "Đặt lại mật khẩu qua email");

    private final String action;
    private final String reason;
    private final LocalDateTime occurredAt;

    public AuditLogEntry(String action, String reason, LocalDateTime occurredAt) {
        this.action = action;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public String getLabel() {
        return LABELS.getOrDefault(action, action);
    }

    public String getReason() {
        return reason;
    }

    public String getOccurredAtText() {
        return DateTimeUtil.formatDateTime(occurredAt);
    }
}
