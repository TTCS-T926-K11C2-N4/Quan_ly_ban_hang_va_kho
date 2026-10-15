package com.oms.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LotStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    // AC3: cảnh báo theo ngưỡng khai báo được
    @Test
    void classifiesByExpiryAndThreshold() {
        assertEquals(LotStatus.EXPIRED, LotStatus.of(LocalDate.of(2026, 10, 14), TODAY, 30));
        assertEquals(LotStatus.NEAR, LotStatus.of(LocalDate.of(2026, 10, 15), TODAY, 30));
        assertEquals(LotStatus.NEAR, LotStatus.of(LocalDate.of(2026, 11, 14), TODAY, 30));
        assertEquals(LotStatus.OK, LotStatus.of(LocalDate.of(2026, 11, 15), TODAY, 30));
        assertEquals(LotStatus.NONE, LotStatus.of(null, TODAY, 30));
    }

    @Test
    void thresholdChangesWhichLotsAreNear() {
        LocalDate expiry = LocalDate.of(2026, 12, 1);
        assertEquals(LotStatus.OK, LotStatus.of(expiry, TODAY, 30));
        assertEquals(LotStatus.NEAR, LotStatus.of(expiry, TODAY, 60));
    }

    @Test
    void parsesFilterCode() {
        assertEquals(LotStatus.EXPIRED, LotStatus.fromCode("EXPIRED"));
        assertNull(LotStatus.fromCode("khác"));
        assertNull(LotStatus.fromCode(null));
    }
}
