package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDateTime;

// Một lần khoá hoặc mở giao dịch đại lý (customer_block_history, S3-07). createdAt lưu theo UTC.
public class CustomerBlockEntry {

    public static final String BLOCK = "BLOCK";
    public static final String UNBLOCK = "UNBLOCK";

    private final String action;
    private final String reason;
    private final LocalDateTime createdAt;
    private final String actorName;

    public CustomerBlockEntry(String action, String reason, LocalDateTime createdAt, String actorName) {
        this.action = action;
        this.reason = reason;
        this.createdAt = createdAt;
        this.actorName = actorName;
    }

    public String getAction() {
        return action;
    }

    public boolean isBlock() {
        return BLOCK.equals(action);
    }

    public String getReason() {
        return reason;
    }

    // Giờ Việt Nam để hiện trên trang
    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public String getActorName() {
        return actorName;
    }
}
