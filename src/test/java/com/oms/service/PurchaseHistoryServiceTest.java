package com.oms.service;

import com.oms.dao.PurchaseHistoryDao.Row;
import com.oms.model.PurchaseHistory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseHistoryServiceTest {

    private static final long CASE = 2;
    private static final long CAN = 1;

    private static Row row(long orderId, int day, long productId, long unitId, String qty) {
        return new Row(orderId, "DH" + orderId, LocalDateTime.of(2026, 10, day, 5, 0), productId, "SP" + productId,
                "Hàng " + productId, unitId, unitId == CASE ? "Thùng" : "Lon", new BigDecimal(qty));
    }

    @Test
    void noPurchasesGivesEmptyHistory() {
        PurchaseHistory history = PurchaseHistoryService.summarize(List.of());
        assertTrue(history.getItems().isEmpty());
        assertNull(history.getLastOrderNo());
        assertTrue(history.getLastOrderLines().isEmpty());
    }

    // AC1: bình quân theo từng mặt hàng; hàng mua nhiều đơn nhất lên đầu
    @Test
    void averagesQuantityPerProductAndRanksByFrequency() {
        PurchaseHistory history = PurchaseHistoryService.summarize(List.of(
                row(3, 12, 10, CASE, "10"), row(3, 12, 20, CASE, "4"),
                row(2, 8, 10, CASE, "20"),
                row(1, 2, 10, CASE, "6")));
        assertEquals(2, history.getItems().size());
        PurchaseHistory.Item first = history.getItems().get(0);
        assertEquals(10, first.getProductId());
        assertEquals(3, first.getOrderCount());
        assertEquals(0, new BigDecimal("12.0").compareTo(first.getAvgQty()));
        assertEquals(1, history.getItems().get(1).getOrderCount());
    }

    // Không cộng lẫn đơn vị: bình quân tính theo đơn vị mua nhiều đơn nhất
    @Test
    void averagesInMostUsedUnitOnly() {
        PurchaseHistory history = PurchaseHistoryService.summarize(List.of(
                row(3, 12, 10, CAN, "5"), row(2, 8, 10, CASE, "10"), row(1, 2, 10, CASE, "20")));
        PurchaseHistory.Item item = history.getItems().get(0);
        assertEquals(CASE, item.getUnitId());
        assertEquals(0, new BigDecimal("15.0").compareTo(item.getAvgQty()));
        assertEquals(3, item.getOrderCount());
    }

    // AC2: các dòng của lần mua gần nhất là dòng của đơn đứng đầu danh sách
    @Test
    void lastOrderLinesComeFromMostRecentOrder() {
        PurchaseHistory history = PurchaseHistoryService.summarize(List.of(
                row(3, 12, 10, CASE, "10"), row(3, 12, 20, CASE, "4"), row(2, 8, 30, CASE, "1")));
        assertEquals("DH3", history.getLastOrderNo());
        assertEquals(2, history.getLastOrderLines().size());
        assertEquals(20, history.getLastOrderLines().get(1).getProductId());
    }
}
