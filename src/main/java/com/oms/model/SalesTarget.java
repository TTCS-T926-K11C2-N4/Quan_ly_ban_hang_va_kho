package com.oms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

// Chỉ tiêu doanh số tháng của một nhân viên hoặc một khu vực kèm doanh số thực tế và tiến độ (S8-04)
public class SalesTarget {

    private final String key;
    private final String name;
    private final BigDecimal target;
    private final BigDecimal actual;
    private final boolean locked;
    private final int expectedPercent;

    public SalesTarget(String key, String name, BigDecimal target, BigDecimal actual, boolean locked,
                       int expectedPercent) {
        this.key = key;
        this.name = name;
        this.target = target;
        this.actual = actual;
        this.locked = locked;
        this.expectedPercent = expectedPercent;
    }

    // Phần trăm kỳ đã trôi qua của tháng month tính tới ngày today: tháng đã qua 100, tháng chưa tới 0
    public static int expectedPercent(LocalDate month, LocalDate today) {
        LocalDate first = month.withDayOfMonth(1);
        LocalDate currentFirst = today.withDayOfMonth(1);
        if (first.isBefore(currentFirst)) {
            return 100;
        }
        if (first.isAfter(currentFirst)) {
            return 0;
        }
        return today.getDayOfMonth() * 100 / month.lengthOfMonth();
    }

    // Phần trăm hoàn thành chỉ tiêu; null khi chưa đặt chỉ tiêu
    public static Integer percent(BigDecimal actual, BigDecimal target) {
        if (target == null || target.signum() <= 0) {
            return null;
        }
        return actual.multiply(BigDecimal.valueOf(100)).divide(target, 0, RoundingMode.HALF_UP).intValue();
    }

    // "u:12" nhân viên hoặc "r:3" khu vực
    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTarget() {
        return target;
    }

    public BigDecimal getActual() {
        return actual;
    }

    public boolean isLocked() {
        return locked;
    }

    public Integer getPercent() {
        return percent(actual, target);
    }

    public int getExpectedPercent() {
        return expectedPercent;
    }

    // Chậm tiến độ: thấp hơn mức kỳ đã trôi qua
    public boolean isBehind() {
        Integer percent = getPercent();
        return percent != null && percent < expectedPercent;
    }

    public int getBarPercent() {
        Integer percent = getPercent();
        return percent == null ? 0 : Math.min(percent, 100);
    }
}
