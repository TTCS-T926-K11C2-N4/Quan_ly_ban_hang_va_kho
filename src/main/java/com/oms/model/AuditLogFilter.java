package com.oms.model;

import java.time.LocalDateTime;

// Bộ lọc nhật ký thao tác; null = không lọc theo tiêu chí đó. Thời gian theo UTC như cột occurred_at.
public class AuditLogFilter {

    private final LocalDateTime fromUtc;
    private final LocalDateTime toUtcExclusive;
    private final Long actorUserId;
    private final String entityType;
    private final String actionGroup;

    public AuditLogFilter(LocalDateTime fromUtc, LocalDateTime toUtcExclusive, Long actorUserId, String entityType,
                          String actionGroup) {
        this.fromUtc = fromUtc;
        this.toUtcExclusive = toUtcExclusive;
        this.actorUserId = actorUserId;
        this.entityType = entityType;
        this.actionGroup = actionGroup;
    }

    public LocalDateTime getFromUtc() {
        return fromUtc;
    }

    public LocalDateTime getToUtcExclusive() {
        return toUtcExclusive;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getEntityType() {
        return entityType;
    }

    // Một nhóm trong AuditCatalog.groups()
    public String getActionGroup() {
        return actionGroup;
    }
}
