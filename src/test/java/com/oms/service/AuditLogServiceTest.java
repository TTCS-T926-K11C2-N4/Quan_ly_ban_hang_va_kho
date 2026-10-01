package com.oms.service;

import com.oms.model.AuditEntityType;
import com.oms.model.AuditLogFilter;
import com.oms.util.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S2-04: lọc nhật ký theo người dùng, loại đối tượng, khoảng thời gian
class AuditLogServiceTest {

    private final AuditLogService auditLogService = new AuditLogService();

    @Test
    void dateRangeMustNotBeReversed() {
        LocalDate from = LocalDate.of(2026, 10, 5);
        assertEquals("Ngày kết thúc phải từ ngày bắt đầu trở đi.", auditLogService.validate(
                new AuditLogFilter(null, null, from, from.minusDays(1))).get("toDate"));
        assertTrue(auditLogService.validate(new AuditLogFilter(null, null, from, from)).isEmpty());
        assertTrue(auditLogService.validate(new AuditLogFilter(null, null, from, null)).isEmpty());
    }

    // occurred_at lưu UTC: ngày 01/10 giờ Việt Nam bắt đầu lúc 17:00 UTC ngày 30/09
    @Test
    void vietnamDayStartsAt1700UtcPreviousDay() {
        assertEquals(LocalDateTime.of(2026, 9, 30, 17, 0), DateTimeUtil.startOfDayUtc(LocalDate.of(2026, 10, 1)));
    }

    @Test
    void entityTypeFilterAcceptsOnlyKnownCodes() {
        assertEquals(AuditEntityType.STOCK, AuditEntityType.fromCode("STOCK"));
        assertEquals(AuditEntityType.CREDIT_LIMIT, AuditEntityType.fromCode("CREDIT_LIMIT"));
        assertNull(AuditEntityType.fromCode("stock"));
        assertNull(AuditEntityType.fromCode("' OR 1=1 --"));
        assertNull(AuditEntityType.fromCode(null));
    }
}
