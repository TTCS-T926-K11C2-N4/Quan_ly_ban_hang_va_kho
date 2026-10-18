package com.oms.service;

import com.oms.model.StockTransfer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockTransferServiceTest {

    private static StockTransfer.Item item(String sent, String received) {
        return new StockTransfer.Item(1, 10, 5, "SP001", "Nước", "L01", "Lon", new BigDecimal(sent),
                received == null ? null : new BigDecimal(received), null);
    }

    @Test
    void sentQuantityMustBePositiveWithUpToThreeDecimals() {
        assertEquals(0, new BigDecimal("12.5").compareTo(StockTransferService.parseQty("12,5")));
        assertNull(StockTransferService.parseQty("0"));
        assertNull(StockTransferService.parseQty("-2"));
        assertNull(StockTransferService.parseQty("1.2345"));
        assertNull(StockTransferService.parseQty("abc"));
    }

    // Số nhận được phép bằng 0 (hàng không tới)
    @Test
    void receivedQuantityMayBeZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(StockTransferService.parseReceived("0")));
        assertNull(StockTransferService.parseReceived("-1"));
    }

    // AC3, AC4: thiếu so với số đã xuất thì đánh dấu thiếu
    @Test
    void shortageIsSentMinusReceived() {
        assertEquals(0, new BigDecimal("3").compareTo(item("10", "7").getShortage()));
        assertTrue(item("10", "7").isMissing());
        assertFalse(item("10", "10").isMissing());
        assertFalse(item("10", null).isMissing());
        assertFalse(item("10", null).isReceived());
    }

    @Test
    void parsesItemIdsAndMarksInvalidOnes() {
        assertEquals(java.util.List.of(5L, -1L, 9L),
                StockTransferService.parseIds(new String[] {"5", "x", " 9 "}));
        assertTrue(StockTransferService.parseIds(null).isEmpty());
    }
}
