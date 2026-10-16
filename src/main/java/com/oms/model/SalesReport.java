package com.oms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Báo cáo doanh số nhiều chiều (S8-06): doanh số, sản lượng, biên lợi nhuận gộp theo nhân viên, khu vực, đại lý, nhóm hàng
// hoặc SKU, so với kỳ liền trước cùng độ dài
public class SalesReport {

    public static final Map<String, String> DIMENSIONS = Map.of("REP", "Nhân viên", "REGION", "Khu vực",
            "CUSTOMER", "Đại lý", "CATEGORY", "Nhóm hàng", "SKU", "SKU");

    private final String dimension;
    private final LocalDate from;
    private final LocalDate to;
    private final LocalDate previousFrom;
    private final LocalDate previousTo;
    private final boolean showMargin;
    private final List<Row> rows;
    private final Row total;

    private SalesReport(String dimension, LocalDate from, LocalDate to, LocalDate previousFrom, LocalDate previousTo,
                        boolean showMargin, List<Row> rows, Row total) {
        this.dimension = dimension;
        this.from = from;
        this.to = to;
        this.previousFrom = previousFrom;
        this.previousTo = previousTo;
        this.showMargin = showMargin;
        this.rows = rows;
        this.total = total;
    }

    // Kỳ liền trước dài bằng kỳ hiện tại, kết thúc ngay trước ngày bắt đầu
    public static LocalDate[] previousPeriod(LocalDate from, LocalDate to) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        return new LocalDate[]{from.minusDays(days), from.minusDays(1)};
    }

    // Tỉ lệ thay đổi so với kỳ trước, làm tròn 1 số lẻ (%); null khi kỳ trước không có doanh số
    public static BigDecimal changePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() <= 0) {
            return null;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 1,
                RoundingMode.HALF_UP);
    }

    // Biên lợi nhuận gộp (%) = lợi nhuận / doanh số; null khi chưa có doanh số
    public static BigDecimal marginPercent(BigDecimal profit, BigDecimal revenue) {
        if (profit == null || revenue == null || revenue.signum() <= 0) {
            return null;
        }
        return profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP);
    }

    public static SalesReport build(String dimension, LocalDate from, LocalDate to, boolean showMargin,
                                    Map<String, BigDecimal[]> current, Map<String, BigDecimal[]> previous) {
        LocalDate[] prev = previousPeriod(from, to);
        List<Row> rows = new ArrayList<>();
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        BigDecimal prevRevenue = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal[]> entry : current.entrySet()) {
            BigDecimal[] prevValues = previous.get(entry.getKey());
            rows.add(new Row(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                    showMargin ? entry.getValue()[2] : null, prevValues == null ? BigDecimal.ZERO : prevValues[0]));
            revenue = revenue.add(entry.getValue()[0]);
            qty = qty.add(entry.getValue()[1]);
            profit = profit.add(entry.getValue()[2]);
        }
        for (BigDecimal[] values : previous.values()) {
            prevRevenue = prevRevenue.add(values[0]);
        }
        // Mục chỉ có ở kỳ trước cũng hiện (doanh số kỳ này bằng 0) để thấy mục đã mất doanh số
        for (Map.Entry<String, BigDecimal[]> entry : previous.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                rows.add(new Row(entry.getKey(), BigDecimal.ZERO, BigDecimal.ZERO, showMargin ? BigDecimal.ZERO : null,
                        entry.getValue()[0]));
            }
        }
        rows.sort((x, y) -> y.revenue.compareTo(x.revenue));
        Row total = new Row("Tổng cộng", revenue, qty, showMargin ? profit : null, prevRevenue);
        return new SalesReport(dimension, from, to, prev[0], prev[1], showMargin, rows, total);
    }

    public String getDimension() {
        return dimension;
    }

    public String getDimensionLabel() {
        return DIMENSIONS.getOrDefault(dimension, dimension);
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public LocalDate getPreviousFrom() {
        return previousFrom;
    }

    public LocalDate getPreviousTo() {
        return previousTo;
    }

    public boolean isShowMargin() {
        return showMargin;
    }

    public List<Row> getRows() {
        return rows;
    }

    public Row getTotal() {
        return total;
    }

    // Một mục của chiều đang xem
    public static class Row {
        private final String name;
        private final BigDecimal revenue;
        private final BigDecimal qty;
        private final BigDecimal profit;
        private final BigDecimal previousRevenue;

        public Row(String name, BigDecimal revenue, BigDecimal qty, BigDecimal profit, BigDecimal previousRevenue) {
            this.name = name;
            this.revenue = revenue;
            this.qty = qty;
            this.profit = profit;
            this.previousRevenue = previousRevenue;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public BigDecimal getProfit() {
            return profit;
        }

        public BigDecimal getMarginPercent() {
            return marginPercent(profit, revenue);
        }

        public BigDecimal getPreviousRevenue() {
            return previousRevenue;
        }

        public BigDecimal getChangePercent() {
            return changePercent(revenue, previousRevenue);
        }
    }
}
