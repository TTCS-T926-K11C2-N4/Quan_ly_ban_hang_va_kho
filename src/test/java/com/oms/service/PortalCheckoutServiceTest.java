package com.oms.service;

import com.oms.model.Customer;
import com.oms.model.OrderForm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PortalCheckoutServiceTest {

    // AC1 của S4-10: đại lý lấy từ tài khoản, không tự đặt giá, không dính đơn nháp nào
    @Test
    void sanitizeForcesOwnCustomerAndDropsManualPrice() {
        Customer customer = new Customer(7, "DL00007", "Đại lý thử", null, null, null, 8L, 1, 1L,
                java.math.BigDecimal.ZERO, 30, false, null, "ACTIVE", 0);
        OrderForm submitted = new OrderForm("999", "5", "2026-10-20", "ghi chú", 12L, "3", List.of(
                new OrderForm.Line("10", "SP001", "2", "4", "1")));
        OrderForm clean = PortalCheckoutService.sanitize(submitted, customer);
        assertEquals("7", clean.getCustomerId());
        assertNull(clean.getDraftId());
        assertNull(clean.getVersion());
        assertNull(clean.getLines().get(0).getPrice());
        assertEquals("4", clean.getLines().get(0).getQty());
        assertEquals("5", clean.getDeliveryAddressId());
    }
}
