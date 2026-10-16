package com.oms.service;

import com.oms.model.StockReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockReportServiceTest {

    // 900 tồn, bán 90 trong 90 ngày = 1/ngày → 900 ngày tồn
    @Test
    void daysOfStockIsOnHandOverDailySales() {
        assertEquals(0, new BigDecimal("900.0").compareTo(
                StockReport.daysOfStock(new BigDecimal("900"), new BigDecimal("90"), 90)));
        assertNull(StockReport.daysOfStock(new BigDecimal("900"), BigDecimal.ZERO, 90));
        assertNull(StockReport.daysOfStock(new BigDecimal("900"), null, 90));
    }

    // AC2: chỉ ra hàng chậm luân chuyển
    @Test
    void slowMovingWhenNoSalesOrOverThreshold() {
        assertTrue(StockReport.isSlow(new BigDecimal("10"), null, 90));
        assertTrue(StockReport.isSlow(new BigDecimal("10"), new BigDecimal("91.0"), 90));
        assertFalse(StockReport.isSlow(new BigDecimal("10"), new BigDecimal("90.0"), 90));
        assertFalse(StockReport.isSlow(BigDecimal.ZERO, null, 90));
    }
}
