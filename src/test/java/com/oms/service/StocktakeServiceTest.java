package com.oms.service;

import com.oms.model.Stocktake;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StocktakeServiceTest {

    private static Stocktake.Item item(String system, String counted) {
        return new Stocktake.Item(1, 10, "SP001", "Nước", "L01", "Lon", new BigDecimal(system),
                counted == null ? null : new BigDecimal(counted));
    }

    // AC2: chênh lệch = số đếm - số sổ, chưa đếm thì chưa có chênh lệch
    @Test
    void varianceIsCountedMinusSystem() {
        assertEquals(0, new BigDecimal("-3").compareTo(item("100", "97").getVariance()));
        assertEquals(0, new BigDecimal("5.5").compareTo(item("10", "15.5").getVariance()));
        assertTrue(item("100", "97").isDiffers());
        assertFalse(item("100", "100").isDiffers());
        assertNull(item("100", null).getVariance());
        assertFalse(item("100", null).isCounted());
    }

    @Test
    void parsesCountsWithUpToThreeDecimals() {
        assertEquals(0, new BigDecimal("12.5").compareTo(StocktakeService.parseCount("12,5")));
        assertEquals(0, BigDecimal.ZERO.compareTo(StocktakeService.parseCount("0")));
        assertNull(StocktakeService.parseCount("-1"));
        assertNull(StocktakeService.parseCount("1.2345"));
        assertNull(StocktakeService.parseCount("abc"));
    }

    @Test
    void openStatesAllowCountingOnly() {
        Stocktake draft = stocktake(Stocktake.DRAFT);
        assertTrue(draft.isOpen());
        assertTrue(stocktake(Stocktake.COUNTING).isOpen());
        assertFalse(stocktake(Stocktake.COMPLETED).isOpen());
        assertFalse(stocktake(Stocktake.CANCELLED).isOpen());
    }

    private static Stocktake stocktake(String status) {
        return new Stocktake(1, "KK261015-0001", 1, "Kho", "WAREHOUSE", null, status, null, null, null, null, null,
                0, 0, 0, List.of());
    }
}
