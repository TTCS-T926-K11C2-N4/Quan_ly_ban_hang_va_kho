package com.oms.service;

import com.oms.model.DebtAgingRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebtLedgerServiceTest {

    private static DebtAgingRow row(String receivable, String paid, String b0, String b31, String b61, String b90,
                                    String overdue) {
        BigDecimal outstanding = new BigDecimal(receivable).subtract(new BigDecimal(paid));
        return new DebtAgingRow(1, "DL", "Đại lý", new BigDecimal(receivable), new BigDecimal(paid), outstanding,
                new BigDecimal(b0), new BigDecimal(b31), new BigDecimal(b61), new BigDecimal(b90),
                new BigDecimal(overdue));
    }

    // AC3: chân bảng cộng đúng từng cột
    @Test
    void totalRowSumsEveryColumn() {
        DebtAgingRow total = DebtAgingRow.total(List.of(
                row("1000", "400", "100", "200", "300", "0", "300"),
                row("500", "0", "0", "0", "0", "500", "500")));
        assertEquals(0, new BigDecimal("1500").compareTo(total.getReceivable()));
        assertEquals(0, new BigDecimal("400").compareTo(total.getPaid()));
        assertEquals(0, new BigDecimal("1100").compareTo(total.getOutstanding()));
        assertEquals(0, new BigDecimal("100").compareTo(total.getBucket0To30()));
        assertEquals(0, new BigDecimal("500").compareTo(total.getBucketOver90()));
        assertEquals(0, new BigDecimal("800").compareTo(total.getOverdue()));
    }

    @Test
    void totalOfNothingIsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(DebtAgingRow.total(List.of()).getOutstanding()));
    }
}
