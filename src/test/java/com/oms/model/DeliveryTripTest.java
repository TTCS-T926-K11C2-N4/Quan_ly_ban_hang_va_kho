package com.oms.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryTripTest {

    private static DeliveryTrip.Stop stop(String status, String value) {
        return new DeliveryTrip.Stop(1, "PX-1", status, 1, "DH-1", "Đại lý", "Kho", "Địa chỉ", "Người nhận", "0909",
                null, new BigDecimal(value));
    }

    private static DeliveryTrip trip(String status, LocalDateTime expectedReturn, DeliveryTrip.Stop... stops) {
        int done = (int) java.util.Arrays.stream(stops).filter(DeliveryTrip.Stop::isDone).count();
        return new DeliveryTrip(1, "CG-1", 1, "Kho", null, "Tài xế", null, "51C-1", status, null, expectedReturn,
                null, null, null, stops.length, done, List.of(stops));
    }

    // AC1: số điểm đã giao và còn lại; AC2: hàng chưa giao xong vẫn nằm trên đường
    @Test
    void countsDoneRemainingAndOnRoadValue() {
        DeliveryTrip trip = trip("RUNNING", null, stop("DELIVERED", "100"), stop("IN_TRANSIT", "250"),
                stop("FAILED", "50"));
        assertEquals(2, trip.getDoneCount());
        assertEquals(1, trip.getRemainingCount());
        assertEquals(0, new BigDecimal("250").compareTo(trip.getOnRoadValue()));
        assertEquals(1, trip.getFailedStops().size());
        assertEquals(0, new BigDecimal("50").compareTo(trip.getFailedValue()));
    }

    // AC3: cảnh báo chuyến quá thời gian dự kiến
    @Test
    void flagsRunningTripPastExpectedReturn() {
        LocalDateTime expected = LocalDateTime.of(2026, 10, 15, 10, 0);
        DeliveryTrip running = trip("RUNNING", expected, stop("IN_TRANSIT", "1"));
        assertTrue(running.isOverdueAt(LocalDateTime.of(2026, 10, 15, 10, 1)));
        assertFalse(running.isOverdueAt(LocalDateTime.of(2026, 10, 15, 9, 59)));
        assertFalse(trip("COMPLETED", expected, stop("DELIVERED", "1")).isOverdueAt(LocalDateTime.of(2026, 10, 16, 0, 0)));
        assertFalse(trip("RUNNING", null, stop("IN_TRANSIT", "1")).isOverdueAt(LocalDateTime.of(2026, 10, 16, 0, 0)));
    }
}
