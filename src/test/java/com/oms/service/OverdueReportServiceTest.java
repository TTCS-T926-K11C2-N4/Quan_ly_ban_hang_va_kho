package com.oms.service;

import com.oms.model.OverdueReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverdueReportServiceTest {

    private static OverdueReport.Row row(long id, String rep, String overdue, int days) {
        BigDecimal amount = new BigDecimal(overdue);
        return new OverdueReport.Row(id, "DL" + id, "Đại lý " + id, rep, amount,
                days <= 30 ? amount : BigDecimal.ZERO, days > 30 && days <= 60 ? amount : BigDecimal.ZERO,
                days > 60 && days <= 90 ? amount : BigDecimal.ZERO, days > 90 ? amount : BigDecimal.ZERO, days);
    }

    @Test
    void riskLevelsFollowOldestOverdueDays() {
        assertEquals(OverdueReport.LOW, OverdueReport.riskOf(30));
        assertEquals(OverdueReport.MEDIUM, OverdueReport.riskOf(31));
        assertEquals(OverdueReport.MEDIUM, OverdueReport.riskOf(90));
        assertEquals(OverdueReport.HIGH, OverdueReport.riskOf(91));
    }

    // AC2: đại lý rủi ro cao nhất đứng đầu; cùng mức thì số tiền lớn hơn đứng trước
    @Test
    void highestRiskCustomerComesFirst() {
        OverdueReport report = OverdueReport.build(List.of(
                row(1, "An", "9000000", 20), row(2, "Bình", "1000000", 120), row(3, "An", "5000000", 100)));
        assertEquals(3, report.getRows().size());
        assertEquals(3, report.getHighestRisk().getCustomerId());
        assertEquals(2, report.getRows().get(1).getCustomerId());
        assertEquals(1, report.getRows().get(2).getCustomerId());
    }

    // AC1: tổng hợp theo nhóm tuổi nợ và theo nhân viên phụ trách
    @Test
    void totalsByBucketAndRep() {
        OverdueReport report = OverdueReport.build(List.of(
                row(1, "An", "9000000", 20), row(2, "Bình", "1000000", 120), row(3, "An", "5000000", 100)));
        assertEquals(0, new BigDecimal("15000000").compareTo(report.getTotal()));
        assertEquals(0, new BigDecimal("9000000").compareTo(report.getBucket1To30()));
        assertEquals(0, new BigDecimal("6000000").compareTo(report.getBucketOver90()));
        assertEquals("An", report.getRepTotals().get(0).getName());
        assertEquals(0, new BigDecimal("14000000").compareTo(report.getRepTotals().get(0).getAmount()));
    }
}
