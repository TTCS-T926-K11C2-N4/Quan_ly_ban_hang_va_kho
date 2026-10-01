package com.oms.model;

// Giá trị cột audit_logs.entity_type. S2-04 yêu cầu ghi mọi thao tác trên tồn kho, giá, hạn mức công nợ
// và hoá đơn: các module đó (làm ở sprint sau) ghi nhật ký bằng AuditLogDao.insert với loại tương ứng.
public enum AuditEntityType {

    USER("Tài khoản"),
    STOCK("Tồn kho"),
    PRICE("Giá bán"),
    CREDIT_LIMIT("Hạn mức công nợ"),
    INVOICE("Hoá đơn");

    private final String label;

    AuditEntityType(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    // null nếu mã không hợp lệ (vd tham số lọc bị sửa tay)
    public static AuditEntityType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditEntityType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
