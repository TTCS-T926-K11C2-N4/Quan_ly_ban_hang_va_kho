package com.oms.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MinStockServiceTest {

    @Test
    void acceptsNonNegativeQuantitiesWithUpToThreeDecimals() {
        assertEquals(0, new BigDecimal("120").compareTo(MinStockService.parseQty("120")));
        assertEquals(0, new BigDecimal("12.5").compareTo(MinStockService.parseQty("12,5")));
        assertEquals(0, new BigDecimal("0.125").compareTo(MinStockService.parseQty(" 0.125 ")));
        assertEquals(0, BigDecimal.ZERO.compareTo(MinStockService.parseQty("0")));
    }

    @Test
    void rejectsNegativeOrMalformedQuantities() {
        assertNull(MinStockService.parseQty("-5"));
        assertNull(MinStockService.parseQty("1.2345"));
        assertNull(MinStockService.parseQty("abc"));
        assertNull(MinStockService.parseQty(""));
        assertNull(MinStockService.parseQty(null));
        assertNull(MinStockService.parseQty("1e3"));
    }
}
