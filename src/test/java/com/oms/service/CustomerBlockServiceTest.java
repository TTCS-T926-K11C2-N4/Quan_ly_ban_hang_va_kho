package com.oms.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomerBlockServiceTest {

    private final CustomerBlockService service = new CustomerBlockService();

    // S3-07 AC2: bắt buộc nhập lý do khoá; mở lại giao dịch cũng phải có lý do (customer_block_history.reason)
    @Test
    void reasonIsRequiredForBothBlockAndUnblock() {
        assertEquals("Vui lòng nhập lý do khoá giao dịch.", service.validateReason(null, true));
        assertEquals("Vui lòng nhập lý do mở lại giao dịch.", service.validateReason(null, false));
    }

    @Test
    void reasonLengthIsLimited() {
        assertEquals("Lý do tối đa 500 ký tự.", service.validateReason("x".repeat(501), true));
        assertNull(service.validateReason("Nợ quá hạn 45 ngày", true));
    }
}
