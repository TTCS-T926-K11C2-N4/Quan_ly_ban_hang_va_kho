package com.oms.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentReminderServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    // AC1: nhắc trước hạn đúng 3 ngày
    @Test
    void remindsThreeDaysBeforeDue() {
        assertEquals(PaymentReminderService.BEFORE_DUE, PaymentReminderService.reminderType(TODAY.plusDays(3), TODAY));
        assertNull(PaymentReminderService.reminderType(TODAY.plusDays(4), TODAY));
        assertNull(PaymentReminderService.reminderType(TODAY.plusDays(1), TODAY));
        assertNull(PaymentReminderService.reminderType(TODAY, TODAY));
    }

    // AC1: nhắc khi quá hạn, ngày đầu tiên rồi mỗi 7 ngày
    @Test
    void remindsWhenOverdueThenWeekly() {
        assertEquals(PaymentReminderService.OVERDUE, PaymentReminderService.reminderType(TODAY.minusDays(1), TODAY));
        assertNull(PaymentReminderService.reminderType(TODAY.minusDays(2), TODAY));
        assertEquals(PaymentReminderService.OVERDUE, PaymentReminderService.reminderType(TODAY.minusDays(8), TODAY));
        assertEquals(PaymentReminderService.OVERDUE, PaymentReminderService.reminderType(TODAY.minusDays(15), TODAY));
    }
}
