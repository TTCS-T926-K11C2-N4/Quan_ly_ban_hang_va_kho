package com.oms.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalOrderTest {

    private static PortalOrder.Item item(String qty, String shipped, boolean shippedKnown) {
        return new PortalOrder.Item(1, "SP001", "Nước", "Thùng", new BigDecimal(qty), new BigDecimal(shipped),
                shippedKnown, new BigDecimal("240000"), BigDecimal.ZERO, new BigDecimal("2400000"));
    }

    private static PortalOrder order(PortalOrder.Item... items) {
        return new PortalOrder(1, "DH261015-0001", "SHIPPED", null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, "Kho", List.of(items));
    }

    // AC2: giao thiếu so với số đặt thì đánh dấu để đại lý thấy số thực giao
    @Test
    void marksLinesDeliveredShort() {
        assertTrue(item("10", "8", true).isShortDelivered());
        assertFalse(item("10", "10", true).isShortDelivered());
    }

    // Đơn chưa xuất kho thì chưa có số thực giao, không báo thiếu
    @Test
    void doesNotReportShortageBeforeShipping() {
        assertFalse(item("10", "0", false).isShortDelivered());
        assertFalse(order(item("10", "0", false)).isShortDelivery());
    }

    @Test
    void orderIsShortWhenAnyLineIsShort() {
        assertTrue(order(item("10", "10", true), item("5", "4", true)).isShortDelivery());
        assertFalse(order(item("10", "10", true)).isShortDelivery());
    }
}
