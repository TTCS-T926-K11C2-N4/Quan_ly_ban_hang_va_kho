package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDateTime;

// Thông báo trong ứng dụng gửi cho một người dùng (nhắc hạn thanh toán S7-08...)
public class Notification {

    private final long id;
    private final String type;
    private final String title;
    private final String body;
    private final String refType;
    private final Long refId;
    private final LocalDateTime readAt;
    private final LocalDateTime createdAt;

    public Notification(long id, String type, String title, String body, String refType, Long refId,
                        LocalDateTime readAt, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.body = body;
        this.refType = refType;
        this.refId = refId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getRefType() {
        return refType;
    }

    public Long getRefId() {
        return refId;
    }

    public boolean isUnread() {
        return readAt == null;
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }
}
