package com.oms.service;

import com.oms.model.DebtDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebtDetailServiceTest {

    // AC1: số dư luỹ kế chạy từ số dư đầu kỳ, cộng Nợ trừ Có từng bút toán
    @Test
    void runningBalanceFollowsEntries() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        DebtDetail detail = DebtDetail.build(1, "DL", "Đại lý", d, d.plusDays(10), new BigDecimal("1000"), List.of(
                new DebtDetail.Entry(d, "INVOICE", "HD1", new BigDecimal("500"), BigDecimal.ZERO),
                new DebtDetail.Entry(d.plusDays(1), "PAYMENT", "PT1", BigDecimal.ZERO, new BigDecimal("300")),
                new DebtDetail.Entry(d.plusDays(2), "CREDIT_NOTE", "CN1", BigDecimal.ZERO, new BigDecimal("100"))));
        assertEquals(3, detail.getRows().size());
        assertEquals(0, new BigDecimal("1500").compareTo(detail.getRows().get(0).getBalance()));
        assertEquals(0, new BigDecimal("1200").compareTo(detail.getRows().get(1).getBalance()));
        assertEquals(0, new BigDecimal("1100").compareTo(detail.getClosing()));
        assertEquals(0, new BigDecimal("500").compareTo(detail.getTotalDebit()));
        assertEquals(0, new BigDecimal("400").compareTo(detail.getTotalCredit()));
    }
}
