package com.oms.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StockLevelTest {

    private static StockLevel level(String available, String minStock) {
        return StockLevel.of(new BigDecimal(available), new BigDecimal(minStock));
    }

    // S5-05 AC2: trạng thái tồn tính theo tồn khả dụng (đã trừ giữ chỗ), không theo tồn thực tế
    @Test
    void outOfStockWhenNothingAvailable() {
        assertEquals(StockLevel.OUT, level("0", "0"));
        assertEquals(StockLevel.OUT, level("0", "20"));
    }

    @Test
    void lowWhenAvailableNotAboveMinimum() {
        assertEquals(StockLevel.LOW, level("15", "20"));
        assertEquals(StockLevel.LOW, level("20", "20"));
        assertEquals(StockLevel.IN, level("21", "20"));
    }

    @Test
    void inStockWhenNoMinimumDeclared() {
        assertEquals(StockLevel.IN, level("1", "0"));
    }

    @Test
    void ledgerRowUsesAvailableQuantity() {
        StockLedgerRow row = new StockLedgerRow(21, "SP021", "Nước rửa chén 750ml", 3, "KHO-BN", "Kho miền Bắc",
                new BigDecimal("85"), new BigDecimal("85"), BigDecimal.ZERO, BigDecimal.ZERO, "Chai", "Gia dụng");
        assertEquals(StockLevel.OUT, row.getLevel());
        assertEquals("Hết hàng", row.getLevel().getLabel());
        assertNull(StockLevel.fromCode("ALL"));
    }
}
