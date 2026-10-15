package com.oms.service;

import com.oms.model.CustomerCredit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S7-04: cờ công nợ tự động và cách chặn / chuyển duyệt đơn mới theo credit.overdue_mode
class CreditFlagTest {

    private static CustomerCredit credit(String limit, String ledger, String overdue, String mode) {
        return new CustomerCredit(new BigDecimal(limit), 30, new BigDecimal(ledger), BigDecimal.ZERO,
                new BigDecimal(overdue), overdue.equals("0") ? null : LocalDate.of(2026, 9, 1), mode);
    }

    // AC1: quá hạn hoặc vượt hạn mức thì gắn cờ, quá hạn ưu tiên
    @Test
    void flagFollowsOverdueThenOverLimit() {
        assertEquals("OK", credit("10000000", "1000000", "0", "BLOCK").getFlag());
        assertEquals("OVER_LIMIT", credit("10000000", "10000001", "0", "BLOCK").getFlag());
        assertEquals("OVERDUE", credit("10000000", "1000000", "500000", "BLOCK").getFlag());
        assertEquals("OVERDUE", credit("10000000", "12000000", "500000", "BLOCK").getFlag());
    }

    // AC2: chế độ BLOCK chặn đại lý bị gắn cờ; REQUIRE_APPROVAL không chặn mà chuyển duyệt
    @Test
    void modeDecidesBlockOrApproval() {
        CustomerCredit blocked = credit("10000000", "1000000", "500000", "BLOCK");
        assertTrue(blocked.isBlocking());
        assertFalse(blocked.requiresApprovalByFlag());
        CustomerCredit approval = credit("10000000", "1000000", "500000", "REQUIRE_APPROVAL");
        assertFalse(approval.isBlocking());
        assertTrue(approval.requiresApprovalByFlag());
        assertTrue(approval.exceeds(new BigDecimal("1000")));
        // Chế độ BLOCK chặn cả đại lý vượt hạn mức; không cấu hình thì chỉ chặn quá hạn như S4-02
        assertTrue(credit("10000000", "10000001", "0", "BLOCK").isBlocking());
        assertFalse(credit("10000000", "10000001", "0", null).isBlocking());
        assertTrue(credit("10000000", "1000000", "500000", null).isBlocking());
    }

    // Đại lý sạch không bị ảnh hưởng ở cả hai chế độ
    @Test
    void cleanCustomerIsNotAffected() {
        for (String mode : new String[]{"BLOCK", "REQUIRE_APPROVAL"}) {
            CustomerCredit clean = credit("10000000", "1000000", "0", mode);
            assertFalse(clean.isBlocking());
            assertFalse(clean.exceeds(new BigDecimal("1000")));
        }
    }
}
