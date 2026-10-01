package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDateTime;

// Một dòng trên trang Nhật ký thao tác (S2-04)
public class AuditLogRecord {

    private final long id;
    private final LocalDateTime occurredAt;
    private final String actorName;
    private final String actorUsername;
    private final String action;
    private final String entityType;
    private final Long entityId;
    private final String oldValues;
    private final String newValues;
    private final String reason;
    private final String ipAddress;

    public AuditLogRecord(long id, LocalDateTime occurredAt, String actorName, String actorUsername, String action,
                          String entityType, Long entityId, String oldValues, String newValues, String reason,
                          String ipAddress) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.actorName = actorName;
        this.actorUsername = actorUsername;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.reason = reason;
        this.ipAddress = ipAddress;
    }

    public long getId() {
        return id;
    }

    public String getOccurredAtText() {
        return DateTimeUtil.formatDateTime(occurredAt);
    }

    // null khi thao tác không có người đăng nhập (vd tự đăng ký tài khoản)
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
        return AuditLogEntry.labelOf(action);
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityTypeLabel() {
        AuditEntityType type = AuditEntityType.fromCode(entityType);
        return type == null ? entityType : type.getLabel();
    }

    public Long getEntityId() {
        return entityId;
    }

    // Giá trị trước và sau dạng JSON, vd {"quantity":120}
    public String getOldValues() {
        return oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public String getReason() {
        return reason;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
