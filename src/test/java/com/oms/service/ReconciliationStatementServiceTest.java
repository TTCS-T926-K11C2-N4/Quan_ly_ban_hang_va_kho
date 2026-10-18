package com.oms.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconciliationStatementServiceTest {

    // AC1: số dư cuối kỳ = đầu kỳ + phát sinh tăng - phát sinh giảm
    @Test
    void closingBalanceIsOpeningPlusDebitMinusCredit() {
        assertEquals(0, new BigDecimal("1300000").compareTo(ReconciliationStatementService.closing(
                new BigDecimal("1000000"), new BigDecimal("500000"), new BigDecimal("200000"))));
        assertEquals(0, new BigDecimal("-100").compareTo(ReconciliationStatementService.closing(
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"))));
    }
}
