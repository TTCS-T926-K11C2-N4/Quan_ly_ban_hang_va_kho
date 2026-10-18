package com.oms.service;

import com.oms.model.CustomerCredit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S4-02, S7-04: cờ công nợ tự động; đại lý quá hạn bị chặn hẳn, đại lý vượt hạn mức thì đơn mới chuyển duyệt
class CreditFlagTest {

    private static CustomerCredit credit(String limit, String ledger, String overdue) {
        return new CustomerCredit(new BigDecimal(limit), 30, new BigDecimal(ledger), BigDecimal.ZERO,
                new BigDecimal(overdue), overdue.equals("0") ? null : LocalDate.of(2026, 9, 1));
    }

    // AC1: quá hạn hoặc vượt hạn mức thì gắn cờ, quá hạn ưu tiên
    @Test
    void flagFollowsOverdueThenOverLimit() {
        assertEquals("OK", credit("10000000", "1000000", "0").getFlag());
        assertEquals("OVER_LIMIT", credit("10000000", "10000001", "0").getFlag());
        assertEquals("OVERDUE", credit("10000000", "1000000", "500000").getFlag());
        assertEquals("OVERDUE", credit("10000000", "12000000", "500000").getFlag());
    }

    // AC3 của S4-02: còn nợ quá hạn thì chặn hẳn đơn mới, kể cả khi còn hạn mức
    @Test
    void overdueCustomerIsBlocked() {
        assertTrue(credit("10000000", "1000000", "500000").isBlocking());
        assertTrue(credit("10000000", "12000000", "500000").isBlocking());
    }

    // Vượt hạn mức không chặn: đơn được tạo nhưng chuyển sang chờ duyệt (AC2 của S4-02)
    @Test
    void overLimitCustomerIsNotBlockedButNeedsApproval() {
        CustomerCredit over = credit("10000000", "10000001", "0");
        assertFalse(over.isBlocking());
        assertTrue(over.exceeds(new BigDecimal("1")));
    }

    // Đại lý sạch không bị chặn và đơn trong hạn mức không cần duyệt
    @Test
    void cleanCustomerIsNotAffected() {
        CustomerCredit clean = credit("10000000", "1000000", "0");
        assertFalse(clean.isBlocking());
        assertFalse(clean.exceeds(new BigDecimal("1000")));
    }
}
