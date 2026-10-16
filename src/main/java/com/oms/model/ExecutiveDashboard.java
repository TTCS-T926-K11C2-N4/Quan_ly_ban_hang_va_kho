package com.oms.model;

import java.math.BigDecimal;
import java.util.List;

// Dashboard điều hành bán hàng và kho (S8-05): thẻ chỉ số, doanh số theo ngày trong tháng và theo khu vực, danh sách đại lý
// quá hạn công nợ
public class ExecutiveDashboard {

    private final BigDecimal todayRevenue;
    private final BigDecimal monthRevenue;
    private final int pendingApproval;
    private final int waitingShipment;
    private final BigDecimal overdueDebt;
    private final int belowMinStock;
    private final List<Bar> daily;
    private final List<Bar> byRegion;
    private final List<OverdueReport.Row> overdueCustomers;

    public ExecutiveDashboard(BigDecimal todayRevenue, BigDecimal monthRevenue, int pendingApproval,
                              int waitingShipment, BigDecimal overdueDebt, int belowMinStock, List<Bar> daily,
                              List<Bar> byRegion, List<OverdueReport.Row> overdueCustomers) {
        this.todayRevenue = todayRevenue;
        this.monthRevenue = monthRevenue;
        this.pendingApproval = pendingApproval;
        this.waitingShipment = waitingShipment;
        this.overdueDebt = overdueDebt;
        this.belowMinStock = belowMinStock;
        this.daily = daily;
        this.byRegion = byRegion;
        this.overdueCustomers = overdueCustomers;
    }

    // Chiều rộng cột (0–100) theo giá trị lớn nhất của dãy; dãy toàn 0 thì cột rỗng
    public static List<Bar> scale(List<Bar> bars) {
        BigDecimal max = BigDecimal.ZERO;
        for (Bar bar : bars) {
            max = max.max(bar.value);
        }
        List<Bar> scaled = new java.util.ArrayList<>();
        for (Bar bar : bars) {
            int percent = max.signum() == 0 ? 0
                    : bar.value.multiply(BigDecimal.valueOf(100)).divide(max, 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
            scaled.add(new Bar(bar.label, bar.value, percent));
        }
        return scaled;
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public BigDecimal getMonthRevenue() {
        return monthRevenue;
    }

    public int getPendingApproval() {
        return pendingApproval;
    }

    public int getWaitingShipment() {
        return waitingShipment;
    }

    public BigDecimal getOverdueDebt() {
        return overdueDebt;
    }

    public int getBelowMinStock() {
        return belowMinStock;
    }

    public List<Bar> getDaily() {
        return daily;
    }

    public List<Bar> getByRegion() {
        return byRegion;
    }

    public List<OverdueReport.Row> getOverdueCustomers() {
        return overdueCustomers;
    }

    // Một cột biểu đồ: nhãn, giá trị và chiều rộng theo phần trăm
    public static class Bar {
        private final String label;
        private final BigDecimal value;
        private final int percent;

        public Bar(String label, BigDecimal value, int percent) {
            this.label = label;
            this.value = value;
            this.percent = percent;
        }

        public String getLabel() {
            return label;
        }

        public BigDecimal getValue() {
            return value;
        }

        public int getPercent() {
            return percent;
        }
    }
}
