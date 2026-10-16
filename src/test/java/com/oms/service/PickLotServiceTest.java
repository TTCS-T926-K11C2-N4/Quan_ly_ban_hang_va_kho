package com.oms.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PickLotServiceTest {

    // AC2 của S6-04: chọn lô khác với gợi ý phải nhập lý do
    @Test
    void reasonIsRequired() {
        assertNotNull(PickLotService.validateReason(null));
        assertNotNull(PickLotService.validateReason("   "));
        assertNull(PickLotService.validateReason("Khách yêu cầu lô mới hơn"));
    }

    @Test
    void reasonHasMaximumLength() {
        assertNull(PickLotService.validateReason("a".repeat(PickLotService.REASON_MAX_LENGTH)));
        assertNotNull(PickLotService.validateReason("a".repeat(PickLotService.REASON_MAX_LENGTH + 1)));
        assertEquals("Vui lòng nhập lý do khi chọn lô khác với gợi ý.", PickLotService.validateReason(""));
    }
}
