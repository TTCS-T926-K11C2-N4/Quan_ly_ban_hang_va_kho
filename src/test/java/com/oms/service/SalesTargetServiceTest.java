package com.oms.service;

import com.oms.model.SalesTarget;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesTargetServiceTest {

    // AC3: tháng đã qua thì khoá
    @Test
    void pastMonthIsLocked() {
        LocalDate today = LocalDate.of(2026, 10, 16);
        assertTrue(SalesTargetService.isLocked(LocalDate.of(2026, 9, 1), today));
        assertFalse(SalesTargetService.isLocked(LocalDate.of(2026, 10, 1), today));
        assertFalse(SalesTargetService.isLocked(LocalDate.of(2026, 11, 1), today));
    }

    @Test
    void parsesMonthOrFallsBackToCurrent() {
        LocalDate today = LocalDate.of(2026, 10, 16);
        assertEquals(LocalDate.of(2026, 8, 1), SalesTargetService.parseMonth("2026-08", today));
        assertEquals(LocalDate.of(2026, 10, 1), SalesTargetService.parseMonth("abc", today));
        assertEquals(LocalDate.of(2026, 10, 1), SalesTargetService.parseMonth(null, today));
    }

    // AC2: tiến độ so với phần kỳ đã trôi qua
    @Test
    void progressAgainstElapsedPartOfMonth() {
        LocalDate today = LocalDate.of(2026, 10, 16);
        int expected = SalesTarget.expectedPercent(LocalDate.of(2026, 10, 1), today);
        assertEquals(51, expected);
        assertEquals(100, SalesTarget.expectedPercent(LocalDate.of(2026, 9, 1), today));
        assertEquals(0, SalesTarget.expectedPercent(LocalDate.of(2026, 11, 1), today));
        assertEquals(40, SalesTarget.percent(new BigDecimal("400"), new BigDecimal("1000")));
        assertNull(SalesTarget.percent(new BigDecimal("400"), BigDecimal.ZERO));
        SalesTarget behind = new SalesTarget("u:1", "An", new BigDecimal("1000"), new BigDecimal("400"), false, 51);
        assertTrue(behind.isBehind());
        assertEquals(40, behind.getBarPercent());
    }
}
