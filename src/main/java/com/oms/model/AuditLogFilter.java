package com.oms.model;

import java.time.LocalDate;

// Điều kiện lọc nhật ký thao tác (S2-04); null nghĩa là không lọc theo tiêu chí đó.
// fromDate/toDate là ngày theo giờ Việt Nam, tính cả hai đầu.
public class AuditLogFilter {

    private final Long actorUserId;
    private final AuditEntityType entityType;
    private final LocalDate fromDate;
    private final LocalDate toDate;

    public AuditLogFilter(Long actorUserId, AuditEntityType entityType, LocalDate fromDate, LocalDate toDate) {
        this.actorUserId = actorUserId;
        this.entityType = entityType;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }
}
