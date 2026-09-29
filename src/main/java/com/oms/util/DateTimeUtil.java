package com.oms.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

// DB lưu DATETIME theo UTC (xem đầu OMS_schema_mysql.sql); hiển thị theo giờ Việt Nam
public final class DateTimeUtil {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateTimeUtil() {
    }

    public static String formatDate(LocalDateTime utc) {
        return utc == null ? null : toDisplayZone(utc).format(DATE);
    }

    public static String formatDateTime(LocalDateTime utc) {
        return utc == null ? null : toDisplayZone(utc).format(DATE_TIME);
    }

    private static ZonedDateTime toDisplayZone(LocalDateTime utc) {
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(DISPLAY_ZONE);
    }
}
