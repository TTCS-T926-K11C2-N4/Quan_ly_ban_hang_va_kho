package com.oms.service;

import com.oms.model.OpenInvoice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentServiceTest {

    private static OpenInvoice invoice(long id, String outstanding) {
        BigDecimal value = new BigDecimal(outstanding);
        return new OpenInvoice(id, "HD-" + id, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), "ISSUED", value,
                value);
    }

    // AC2: hoá đơn cũ nhất (đứng trước trong danh sách) được trừ trước
    @Test
    void oldestInvoiceIsSettledFirst() {
        List<PaymentService.Share> shares = PaymentService.allocate(new BigDecimal("1500000"),
                List.of(invoice(1, "1000000"), invoice(2, "1000000")));
        assertEquals(2, shares.size());
        assertEquals(0, new BigDecimal("1000000").compareTo(shares.get(0).amount()));
        assertEquals(0, new BigDecimal("500000").compareTo(shares.get(1).amount()));
    }

    // AC3: thu một phần chỉ chạm hoá đơn đầu
    @Test
    void partialPaymentTouchesOnlyFirstInvoice() {
        List<PaymentService.Share> shares = PaymentService.allocate(new BigDecimal("300000"),
                List.of(invoice(1, "1000000"), invoice(2, "1000000")));
        assertEquals(1, shares.size());
        assertEquals(1, shares.get(0).invoice().getId());
    }

    // AC4: thu vượt tổng còn nợ bị từ chối
    @Test
    void overpaymentIsRejected() {
        assertNull(PaymentService.allocate(new BigDecimal("2000001"),
                List.of(invoice(1, "1000000"), invoice(2, "1000000"))));
    }

    @Test
    void parsesAmountWithThousandSeparators() {
        assertEquals(0, new BigDecimal("1500000").compareTo(PaymentService.parseAmount("1.500.000")));
        assertNull(PaymentService.parseAmount("0"));
        assertNull(PaymentService.parseAmount("-5"));
        assertNull(PaymentService.parseAmount("12abc"));
        assertNull(PaymentService.parseAmount(null));
    }
}
