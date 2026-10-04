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
    private static final DateTimeFormatter DATE_TIME_SECONDS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private DateTimeUtil() {
    }

    public static String formatDate(LocalDateTime utc) {
        return utc == null ? null : toDisplayZone(utc).format(DATE);
    }

    // Ngày không có giờ (cột DATE như ngày hiệu lực bảng giá), không đổi múi giờ
    public static String formatDay(LocalDate date) {
        return date == null ? null : date.format(DATE);
    }

    public static String formatDateTime(LocalDateTime utc) {
        return utc == null ? null : toDisplayZone(utc).format(DATE_TIME);
    }

    public static String formatDateTimeSeconds(LocalDateTime utc) {
        return utc == null ? null : toDisplayZone(utc).format(DATE_TIME_SECONDS);
    }

    // 00:00 của một ngày theo giờ Việt Nam, đổi sang UTC để so với cột DATETIME
    public static LocalDateTime startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(DISPLAY_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDate today() {
        return LocalDate.now(DISPLAY_ZONE);
    }

    private static ZonedDateTime toDisplayZone(LocalDateTime utc) {
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(DISPLAY_ZONE);
    }
}
