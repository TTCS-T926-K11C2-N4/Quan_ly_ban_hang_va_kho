package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDateTime;
import java.util.Locale;

// Một dòng ở trang Nhật ký thao tác (S2-04)
public class AuditLogRow {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

    private final long id;
    private final LocalDateTime occurredAtUtc;
    private final String actorName;
    private final String actorUsername;
    private final String action;
    private final String entityType;
    private final Long entityId;
    private final String entityRef;
    private final String ipAddress;
    private final String oldValues;
    private final String newValues;
    private final String reason;

    // entityRef: mã dễ nhận ra của đối tượng (tên đăng nhập, SKU, mã nhóm); null nếu không xác định được
    public AuditLogRow(long id, LocalDateTime occurredAtUtc, String actorName, String actorUsername, String action,
                       String entityType, Long entityId, String entityRef, String ipAddress, String oldValues,
                       String newValues, String reason) {
        this.id = id;
        this.occurredAtUtc = occurredAtUtc;
        this.actorName = actorName;
        this.actorUsername = actorUsername;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityRef = entityRef;
        this.ipAddress = ipAddress;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.reason = reason;
    }

    public long getId() {
        return id;
    }

    public String getOccurredAtText() {
        return DateTimeUtil.formatDateTimeSeconds(occurredAtUtc);
    }

    // null khi không có người đăng nhập (vd tự đăng ký tài khoản)
    public String getActorName() {
        return actorName;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getAction() {
        return action;
    }

    public String getActionLabel() {
        return AuditCatalog.actionLabel(action);
    }

    public String getActionGroup() {
        return AuditCatalog.actionGroup(action);
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityLabel() {
        return AuditCatalog.entityLabel(entityType);
    }

    // vd "Sửa sản phẩm SP007", "Khoá tài khoản thaihv"
    public String getSummary() {
        String ref = entityRef != null ? entityRef : entityId != null ? "#" + entityId : "";
        return (getActionLabel() + " " + getEntityLabel().toLowerCase(VIETNAMESE)
                + " " + ref).trim();
    }

    public String getIpAddress() {
        return ipAddress;
    }

    // JSON giá trị trước/sau; null nếu thao tác không có
    public String getOldValues() {
        return oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public String getReason() {
        return reason;
    }
}
