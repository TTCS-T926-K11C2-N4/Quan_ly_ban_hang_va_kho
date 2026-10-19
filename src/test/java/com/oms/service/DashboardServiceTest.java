package com.oms.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DashboardServiceTest {

    @Test
    void changePercentComparesWithYesterday() {
        assertEquals(50.0, DashboardService.changePercent(new BigDecimal("150"), new BigDecimal("100")));
        assertEquals(-25.0, DashboardService.changePercent(new BigDecimal("3"), new BigDecimal("4")));
        assertEquals(0.0, DashboardService.changePercent(new BigDecimal("7"), new BigDecimal("7")));
        assertEquals(33.3, DashboardService.changePercent(new BigDecimal("4"), new BigDecimal("3")));
    }

    @Test
    void changePercentIsEmptyWhenYesterdayIsZero() {
        assertNull(DashboardService.changePercent(new BigDecimal("5"), BigDecimal.ZERO));
        assertNull(DashboardService.changePercent(BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
