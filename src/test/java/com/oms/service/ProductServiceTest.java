package com.oms.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductServiceTest {

    @Test
    void costPriceAcceptsThousandSeparators() {
        assertEquals(new BigDecimal("30000"), ProductService.parseCostPrice("30.000"));
        assertEquals(new BigDecimal("1250000"), ProductService.parseCostPrice("1,250,000"));
        assertEquals(new BigDecimal("15000"), ProductService.parseCostPrice("15 000"));
        assertEquals(BigDecimal.ZERO, ProductService.parseCostPrice(null));
    }

    @Test
    void costPriceRejectsNegativeTextAndTooManyDigits() {
        assertNull(ProductService.parseCostPrice("-5000"));
        assertNull(ProductService.parseCostPrice("ba mươi nghìn"));
        assertNull(ProductService.parseCostPrice("12345678901234567"));
    }

    @Test
    void skuMustBeTwoToFiftyAllowedCharacters() {
        assertNull(ProductService.skuError("SP001"));
        assertNull(ProductService.skuError("BIA-HN.330_24"));
        assertNotNull(ProductService.skuError(null));
        assertNotNull(ProductService.skuError("S"));
        assertNotNull(ProductService.skuError("SỮA 01"));
        assertNotNull(ProductService.skuError("A".repeat(51)));
    }
}
