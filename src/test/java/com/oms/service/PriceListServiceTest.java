package com.oms.service;

import com.oms.model.PriceListStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PriceListServiceTest {

    @Test
    void versionCodeKeepsBaseCode() {
        assertEquals("BG001-V2", PriceListService.versionCode("BG001", 2));
        assertEquals("BG001-V3", PriceListService.versionCode("BG001-V2", 3));
    }

    @Test
    void priceMustBeEntered() {
        assertNull(PriceListService.parseMoney(null));
        assertEquals(new BigDecimal("15500000"), PriceListService.parseMoney("15.500.000"));
        assertNull(PriceListService.parseMoney("-1"));
    }

    @Test
    void statusFollowsValidityDates() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        assertEquals(PriceListStatus.UPCOMING, PriceListStatus.of(from, to, LocalDate.of(2026, 9, 30)));
        assertEquals(PriceListStatus.ACTIVE, PriceListStatus.of(from, to, from));
        assertEquals(PriceListStatus.ACTIVE, PriceListStatus.of(from, to, to));
        assertEquals(PriceListStatus.EXPIRED, PriceListStatus.of(from, to, LocalDate.of(2027, 1, 1)));
    }
}
