package com.oms.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeliveryTripServiceTest {

    // Giờ nhập theo giờ Việt Nam (UTC+7) được đổi sang UTC để lưu
    @Test
    void convertsVietnamTimeToUtc() {
        assertEquals(LocalDateTime.of(2026, 10, 16, 1, 30),
                DeliveryTripService.parseLocalDateTime("2026-10-16T08:30"));
    }

    @Test
    void blankTimeMeansNotSet() {
        assertNull(DeliveryTripService.parseLocalDateTime(""));
        assertNull(DeliveryTripService.parseLocalDateTime(null));
    }

    @Test
    void rejectsMalformedTime() {
        assertThrows(IllegalArgumentException.class, () -> DeliveryTripService.parseLocalDateTime("ngày mai"));
    }
}
