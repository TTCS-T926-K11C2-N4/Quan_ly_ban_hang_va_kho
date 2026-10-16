package com.oms.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AdjustmentServiceTest {

    // AC2: giá trị tổn thất = số lượng x giá vốn, làm tròn tới đồng
    @Test
    void lossIsQuantityTimesCost() {
        assertEquals(0, new BigDecimal("30000").compareTo(
                AdjustmentService.loss(new BigDecimal("5"), new BigDecimal("6000.0000"))));
        assertEquals(0, new BigDecimal("1667").compareTo(
                AdjustmentService.loss(new BigDecimal("0.5"), new BigDecimal("3333.3333"))));
    }

    @Test
    void parsesPositiveQuantityOnly() {
        assertEquals(0, new BigDecimal("2.5").compareTo(AdjustmentService.parseQty("2,5")));
        assertNull(AdjustmentService.parseQty("0"));
        assertNull(AdjustmentService.parseQty("-1"));
        assertNull(AdjustmentService.parseQty("1.2345"));
        assertNull(AdjustmentService.parseQty("abc"));
    }
}
