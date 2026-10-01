package com.oms.util;

import java.time.LocalDate;
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

    // Đầu ngày (00:00 giờ Việt Nam) đổi sang UTC để so với cột DATETIME, vd 01/10 -> 30/09 17:00 UTC
    public static LocalDateTime startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(DISPLAY_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static ZonedDateTime toDisplayZone(LocalDateTime utc) {
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(DISPLAY_ZONE);
    }
}
