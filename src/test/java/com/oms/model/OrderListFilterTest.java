package com.oms.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderListFilterTest {

    private static OrderListFilter filter(String status, LocalDate from, LocalDate to, String sort, int size) {
        return new OrderListFilter(null, status, null, null, null, from, to, sort, size);
    }

    // Tham số lạ trên link không được lọt vào SQL: trạng thái, cách sắp xếp, số dòng đều chỉ nhận giá trị đã biết
    @Test
    void unknownValuesFallBackToDefaults() {
        OrderListFilter filter = filter("DROP TABLE", null, null, "zzz", 999);
        assertNull(filter.getStatus());
        assertEquals(OrderListFilter.SORT_NEWEST, filter.getSort());
        assertEquals(OrderListFilter.DEFAULT_PAGE_SIZE, filter.getPageSize());
        assertFalse(filter.isFiltered());
    }

    @Test
    void acceptsKnownStatusSortAndPageSize() {
        OrderListFilter filter = filter(OrderStatus.CANCELLED, null, null, "total_desc", 50);
        assertEquals(OrderStatus.CANCELLED, filter.getStatus());
        assertEquals("total_desc", filter.getSort());
        assertEquals(50, filter.getPageSize());
        assertTrue(filter.isFiltered());
        assertEquals(1, filter.getActiveFilterCount());
    }

    // Khoảng ngày đảo ngược thì đổi chỗ để vẫn có kết quả
    @Test
    void swapsReversedDateRange() {
        OrderListFilter filter = filter(null, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1), null, 10);
        assertEquals(LocalDate.of(2026, 10, 1), filter.getDateFrom());
        assertEquals(LocalDate.of(2026, 10, 20), filter.getDateTo());
        assertEquals(2, filter.getActiveFilterCount());
    }

    @Test
    void everyStatusCodeIsKnownAndLabelled() {
        for (String code : OrderStatus.codes()) {
            assertTrue(OrderStatus.isKnown(code));
        }
        assertEquals(9, OrderStatus.codes().size());
    }
}
