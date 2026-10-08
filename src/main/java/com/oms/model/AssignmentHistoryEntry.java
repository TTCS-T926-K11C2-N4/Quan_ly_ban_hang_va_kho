package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDateTime;

// Một lần đổi người phụ trách đại lý (customer_assignment_history, S3-06); fromName null = trước đó chưa có ai
public class AssignmentHistoryEntry {

    private final String fromName;
    private final String toName;
    private final String reason;
    private final LocalDateTime createdAt;
    private final String actorName;

    public AssignmentHistoryEntry(String fromName, String toName, String reason, LocalDateTime createdAt,
                                  String actorName) {
        this.fromName = fromName;
        this.toName = toName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.actorName = actorName;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }

    public String getReason() {
        return reason;
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public String getActorName() {
        return actorName;
    }
}
