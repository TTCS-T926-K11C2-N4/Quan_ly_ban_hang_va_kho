package com.oms.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockCardTest {

    private static StockCardEntry entry(String type, String qty, String refType, String documentNo) {
        return new StockCardEntry(LocalDateTime.of(2026, 10, 15, 3, 0), "KHO-TT", "L01", type, new BigDecimal(qty),
                new BigDecimal("100"), refType, 7, documentNo, null, "Quản lý kho");
    }

    // AC2: mỗi dòng có liên kết tới chứng từ gốc theo loại chứng từ
    @Test
    void mapsEachDocumentTypeToItsViewer() {
        assertEquals("/goods-receipts/view", entry("RECEIPT", "10", "GOODS_RECEIPT", "PN-1").getDocumentPath());
        assertEquals("/delivery-notes/view", entry("ISSUE", "-5", "DELIVERY_NOTE", "PX-1").getDocumentPath());
        assertEquals("/transfers/view", entry("TRANSFER_OUT", "-3", "STOCK_TRANSFER", "CK-1").getDocumentPath());
        assertEquals("/stocktakes/view", entry("STOCKTAKE", "2", "STOCKTAKE", "KK-1").getDocumentPath());
        assertNull(entry("ADJUSTMENT", "1", "TEST_RESTORE", null).getDocumentPath());
    }

    @Test
    void labelsMovementAndMarksDirection() {
        StockCardEntry in = entry("TRANSFER_IN", "8", "STOCK_TRANSFER", "CK-1");
        assertEquals("Chuyển đến", in.getMovementLabel());
        assertTrue(in.isIncrease());
        assertFalse(entry("ISSUE", "-5", "DELIVERY_NOTE", "PX-1").isIncrease());
    }

    // Chứng từ không còn ở bảng nguồn thì vẫn hiện được mã tạm theo id
    @Test
    void fallsBackToIdWhenDocumentNumberIsMissing() {
        assertEquals("#7", entry("ADJUSTMENT", "1", "TEST_RESTORE", null).getDocumentNo());
        assertEquals("PN-1", entry("RECEIPT", "10", "GOODS_RECEIPT", "PN-1").getDocumentNo());
    }

    // AC3: khoảng ngày bị đảo thì đổi chỗ
    @Test
    void swapsReversedDateRange() {
        StockCardFilter filter = new StockCardFilter(1L, null, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1));
        assertEquals(LocalDate.of(2026, 10, 1), filter.getDateFrom());
        assertEquals(LocalDate.of(2026, 10, 20), filter.getDateTo());
    }
}
