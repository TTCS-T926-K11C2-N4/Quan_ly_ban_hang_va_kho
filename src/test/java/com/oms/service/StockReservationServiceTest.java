package com.oms.service;

import com.oms.model.OrderStatus;
import com.oms.model.ReservationFilter;
import com.oms.model.ReservationState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockReservationServiceTest {

    // S5-06 AC1: thiếu tồn khả dụng thì không giữ chỗ, báo rõ SKU nào thiếu bao nhiêu
    @Test
    void reportsShortageBySkuAndWarehouse() {
        assertNull(StockReservationService.shortage("SP011", "KHO-TT", new BigDecimal("24"), new BigDecimal("24")));
        assertEquals("SP011 tại KHO-TT cần 24, khả dụng 10",
                StockReservationService.shortage("SP011", "KHO-TT", new BigDecimal("24.000"), new BigDecimal("10.000")));
        assertEquals("SP021 tại KHO-HN cần 12, khả dụng 0",
                StockReservationService.shortage("SP021", "KHO-HN", new BigDecimal("12"), null));
    }

    @Test
    void summarizesOrderReservationState() {
        assertEquals(ReservationState.HELD, ReservationState.of(3, 3, 0, 0, 0));
        assertEquals(ReservationState.PARTIAL, ReservationState.of(3, 1, 2, 2, 0));
        // S5-06 AC2: quá hạn giữ chỗ thì tồn được nhả, đơn hiện "Hết hạn giữ chỗ"
        assertEquals(ReservationState.EXPIRED, ReservationState.of(2, 0, 2, 2, 0));
        assertEquals(ReservationState.RELEASED, ReservationState.of(2, 0, 0, 2, 0));
        assertEquals(ReservationState.FULFILLED, ReservationState.of(2, 0, 0, 0, 2));
    }

    @Test
    void lineStateFollowsLatestReservation() {
        assertEquals(ReservationState.HELD, ReservationState.ofLine("ACTIVE", null));
        assertEquals(ReservationState.EXPIRED, ReservationState.ofLine("RELEASED", ReservationState.REASON_EXPIRED));
        assertEquals(ReservationState.RELEASED,
                ReservationState.ofLine("RELEASED", ReservationState.REASON_ORDER_CANCELLED));
        assertEquals(ReservationState.NONE, ReservationState.ofLine(null, null));
        assertEquals("Đã giữ chỗ", ReservationState.HELD.getLabel());
    }

    @Test
    void listDefaultsToApprovedOrders() {
        ReservationFilter filter = new ReservationFilter(null, null, null, null, null);
        assertEquals(OrderStatus.APPROVED, filter.getOrderStatus());
        assertFalse(filter.isFiltered());
        ReservationFilter all = new ReservationFilter("DH", "2026-10-12", ReservationFilter.ALL, 11L, 1L);
        assertNull(all.getOrderStatus());
        assertTrue(all.isFiltered());
        assertTrue(all.isProductFiltered());
        assertNull(new ReservationFilter(null, "12/10/2026", null, null, null).getApprovedDate());
    }
}
