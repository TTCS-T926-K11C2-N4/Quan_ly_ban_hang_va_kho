package com.oms.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PriceHistoryRowTest {

    private static PriceHistoryRow row(String oldPrice, String newPrice, LocalDate validFrom, LocalDate validTo) {
        return new PriceHistoryRow(1, validFrom, LocalDateTime.of(2026, 10, 2, 11, 0), "SP001", "Nước suối",
                "Nước uống", "Thùng", "BG-001", 2, "Đại lý cấp 1", oldPrice == null ? null : new BigDecimal(oldPrice),
                new BigDecimal(newPrice), null, new BigDecimal("1000"), "Quản lý", validFrom, validTo);
    }

    // S3-02: % tăng/giảm giá bán để giải thích với đại lý vì sao giá tháng này khác tháng trước
    @Test
    void changePercentIsRoundedToOneDecimal() {
        assertEquals(new BigDecimal("3.6"), row("14000", "14500", LocalDate.of(2026, 1, 1), null).getChangePercent());
        assertEquals(new BigDecimal("-4.2"), row("12000", "11500", LocalDate.of(2026, 1, 1), null).getChangePercent());
    }

    @Test
    void newPriceHasNoChangePercent() {
        assertNull(row(null, "14500", LocalDate.of(2026, 1, 1), null).getChangePercent());
        assertNull(row("0", "14500", LocalDate.of(2026, 1, 1), null).getChangePercent());
    }

    // Trạng thái theo hiệu lực của bảng giá chứa lần đổi giá
    @Test
    void statusFollowsPriceListValidity() {
        assertEquals(PriceListStatus.EXPIRED,
                row("1", "2", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)).getStatus());
        assertEquals(PriceListStatus.UPCOMING, row("1", "2", LocalDate.of(2999, 1, 1), null).getStatus());
    }
}
