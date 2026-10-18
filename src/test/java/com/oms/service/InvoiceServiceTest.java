package com.oms.service;

import com.oms.dao.InvoiceDao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceServiceTest {

    // Dòng đơn: 10 thùng (24 lon/thùng) = 240 lon, đơn giá 240.000/thùng, chiết khấu 100.000, thành tiền 2.300.000
    private static InvoiceDao.Source source(String deliveredBase) {
        return new InvoiceDao.Source(1, "PX-001", "DELIVERED", 1, "DH-1", 1, 1, 1, 1, 1, new BigDecimal("24"),
                new BigDecimal(deliveredBase), new BigDecimal("240"), new BigDecimal("240000.00"),
                new BigDecimal("100000.00"), new BigDecimal("2300000.00"), new BigDecimal("8000"), false);
    }

    // AC1: giao đủ thì hoá đơn đúng bằng dòng đơn
    @Test
    void fullDeliveryMatchesOrderLine() {
        InvoiceService.Line line = InvoiceService.price(source("240"));
        assertEquals(0, new BigDecimal("10").compareTo(line.qty()));
        assertEquals(0, new BigDecimal("100000").compareTo(line.discount()));
        assertEquals(0, new BigDecimal("2300000").compareTo(line.lineTotal()));
    }

    // AC1: giao thiếu thì hoá đơn theo số thực giao (8 thùng), chiết khấu theo tỉ lệ
    @Test
    void shortDeliveryIsBilledOnDeliveredQuantity() {
        InvoiceService.Line line = InvoiceService.price(source("192"));
        assertEquals(0, new BigDecimal("8").compareTo(line.qty()));
        assertEquals(0, new BigDecimal("80000").compareTo(line.discount()));
        // 8 thùng x 240.000 - 80.000
        assertEquals(0, new BigDecimal("1840000").compareTo(line.lineTotal()));
    }
}
