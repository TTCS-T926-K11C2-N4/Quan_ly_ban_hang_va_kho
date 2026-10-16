package com.oms.service;

import com.oms.model.ExecutiveDashboard;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutiveDashboardServiceTest {

    @Test
    void barWidthsScaleToLargestValue() {
        List<ExecutiveDashboard.Bar> bars = ExecutiveDashboard.scale(List.of(
                new ExecutiveDashboard.Bar("01", new BigDecimal("200"), 0),
                new ExecutiveDashboard.Bar("02", new BigDecimal("50"), 0),
                new ExecutiveDashboard.Bar("03", BigDecimal.ZERO, 0)));
        assertEquals(100, bars.get(0).getPercent());
        assertEquals(25, bars.get(1).getPercent());
        assertEquals(0, bars.get(2).getPercent());
    }

    @Test
    void allZeroSeriesHasEmptyBars() {
        List<ExecutiveDashboard.Bar> bars = ExecutiveDashboard.scale(List.of(
                new ExecutiveDashboard.Bar("01", BigDecimal.ZERO, 0)));
        assertEquals(0, bars.get(0).getPercent());
    }
}
