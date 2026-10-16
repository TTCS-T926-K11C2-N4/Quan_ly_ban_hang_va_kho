package com.oms.service;

import com.oms.model.SalesReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SalesReportServiceTest {

    // AC4: kỳ trước dài bằng kỳ hiện tại và liền ngay trước
    @Test
    void previousPeriodHasSameLengthAndEndsBeforeStart() {
        LocalDate[] prev = SalesReport.previousPeriod(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 30)}, prev);
    }

    @Test
    void changeAndMarginPercentages() {
        assertEquals(0, new BigDecimal("25.0").compareTo(
                SalesReport.changePercent(new BigDecimal("1250"), new BigDecimal("1000"))));
        assertNull(SalesReport.changePercent(new BigDecimal("1250"), BigDecimal.ZERO));
        assertEquals(0, new BigDecimal("20.0").compareTo(
                SalesReport.marginPercent(new BigDecimal("200"), new BigDecimal("1000"))));
        assertNull(SalesReport.marginPercent(new BigDecimal("200"), BigDecimal.ZERO));
    }

    // AC3: không có quyền giá vốn thì không có lợi nhuận; mục chỉ có ở kỳ trước vẫn hiện với doanh số kỳ này bằng 0
    @Test
    void marginHiddenWithoutCostPermissionAndLostItemsShown() {
        Map<String, BigDecimal[]> current = new LinkedHashMap<>();
        current.put("An", new BigDecimal[]{new BigDecimal("1000"), new BigDecimal("10"), new BigDecimal("300")});
        Map<String, BigDecimal[]> previous = new LinkedHashMap<>();
        previous.put("An", new BigDecimal[]{new BigDecimal("800"), new BigDecimal("8"), new BigDecimal("200")});
        previous.put("Bình", new BigDecimal[]{new BigDecimal("500"), new BigDecimal("5"), new BigDecimal("100")});
        SalesReport hidden = SalesReport.build("REP", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), false,
                current, previous);
        assertNull(hidden.getRows().get(0).getProfit());
        assertEquals(2, hidden.getRows().size());
        assertEquals("Bình", hidden.getRows().get(1).getName());
        assertEquals(0, new BigDecimal("1300").compareTo(hidden.getTotal().getPreviousRevenue()));
        SalesReport shown = SalesReport.build("REP", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), true,
                current, previous);
        assertEquals(0, new BigDecimal("30.0").compareTo(shown.getRows().get(0).getMarginPercent()));
    }
}
