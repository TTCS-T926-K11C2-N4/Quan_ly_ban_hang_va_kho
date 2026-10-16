package com.oms.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Báo cáo công nợ quá hạn (S8-09): theo nhóm tuổi nợ (số ngày quá hạn so với hạn thanh toán), theo nhân viên phụ trách
// và danh sách đại lý xếp theo mức rủi ro
public class OverdueReport {

    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String LOW = "LOW";

    private final List<Row> rows;
    private final BigDecimal total;
    private final BigDecimal bucket1To30;
    private final BigDecimal bucket31To60;
    private final BigDecimal bucket61To90;
    private final BigDecimal bucketOver90;
    private final List<RepTotal> repTotals;

    private OverdueReport(List<Row> rows, BigDecimal total, BigDecimal b1, BigDecimal b2, BigDecimal b3,
                          BigDecimal b4, List<RepTotal> repTotals) {
        this.rows = rows;
        this.total = total;
        this.bucket1To30 = b1;
        this.bucket31To60 = b2;
        this.bucket61To90 = b3;
        this.bucketOver90 = b4;
        this.repTotals = repTotals;
    }

    // Mức rủi ro: cao khi có khoản quá hạn trên 90 ngày; trung bình khi quá hạn 31–90 ngày; thấp còn lại
    public static String riskOf(int oldestDaysOverdue) {
        if (oldestDaysOverdue > 90) {
            return HIGH;
        }
        return oldestDaysOverdue > 30 ? MEDIUM : LOW;
    }

    // Xếp đại lý theo mức rủi ro rồi số tiền quá hạn giảm dần; đại lý đầu tiên là rủi ro cao nhất
    public static OverdueReport build(List<Row> input) {
        List<Row> sorted = new ArrayList<>(input);
        sorted.sort((x, y) -> {
            int byRisk = Integer.compare(y.riskRank(), x.riskRank());
            return byRisk != 0 ? byRisk : y.overdue.compareTo(x.overdue);
        });
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal b1 = BigDecimal.ZERO;
        BigDecimal b2 = BigDecimal.ZERO;
        BigDecimal b3 = BigDecimal.ZERO;
        BigDecimal b4 = BigDecimal.ZERO;
        Map<String, BigDecimal> byRep = new LinkedHashMap<>();
        for (Row row : sorted) {
            total = total.add(row.overdue);
            b1 = b1.add(row.bucket1To30);
            b2 = b2.add(row.bucket31To60);
            b3 = b3.add(row.bucket61To90);
            b4 = b4.add(row.bucketOver90);
            byRep.merge(row.repName, row.overdue, BigDecimal::add);
        }
        List<RepTotal> reps = new ArrayList<>();
        byRep.forEach((name, amount) -> reps.add(new RepTotal(name, amount)));
        reps.sort((x, y) -> y.amount.compareTo(x.amount));
        return new OverdueReport(sorted, total, b1, b2, b3, b4, reps);
    }

    public List<Row> getRows() {
        return rows;
    }

    public Row getHighestRisk() {
        return rows.isEmpty() ? null : rows.get(0);
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getBucket1To30() {
        return bucket1To30;
    }

    public BigDecimal getBucket31To60() {
        return bucket31To60;
    }

    public BigDecimal getBucket61To90() {
        return bucket61To90;
    }

    public BigDecimal getBucketOver90() {
        return bucketOver90;
    }

    public List<RepTotal> getRepTotals() {
        return repTotals;
    }

    // Một đại lý có công nợ quá hạn
    public static class Row {
        private final long customerId;
        private final String code;
        private final String name;
        private final String repName;
        private final BigDecimal overdue;
        private final BigDecimal bucket1To30;
        private final BigDecimal bucket31To60;
        private final BigDecimal bucket61To90;
        private final BigDecimal bucketOver90;
        private final int oldestDays;

        public Row(long customerId, String code, String name, String repName, BigDecimal overdue, BigDecimal b1,
                   BigDecimal b2, BigDecimal b3, BigDecimal b4, int oldestDays) {
            this.customerId = customerId;
            this.code = code;
            this.name = name;
            this.repName = repName == null ? "Chưa phân công" : repName;
            this.overdue = overdue;
            this.bucket1To30 = b1;
            this.bucket31To60 = b2;
            this.bucket61To90 = b3;
            this.bucketOver90 = b4;
            this.oldestDays = oldestDays;
        }

        int riskRank() {
            return switch (riskOf(oldestDays)) {
                case HIGH -> 3;
                case MEDIUM -> 2;
                default -> 1;
            };
        }

        public long getCustomerId() {
            return customerId;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getRepName() {
            return repName;
        }

        public BigDecimal getOverdue() {
            return overdue;
        }

        public BigDecimal getBucket1To30() {
            return bucket1To30;
        }

        public BigDecimal getBucket31To60() {
            return bucket31To60;
        }

        public BigDecimal getBucket61To90() {
            return bucket61To90;
        }

        public BigDecimal getBucketOver90() {
            return bucketOver90;
        }

        public int getOldestDays() {
            return oldestDays;
        }

        public String getRisk() {
            return riskOf(oldestDays);
        }

        public String getRiskLabel() {
            return switch (riskOf(oldestDays)) {
                case HIGH -> "Cao";
                case MEDIUM -> "Trung bình";
                default -> "Thấp";
            };
        }

        public String getRiskTone() {
            return switch (riskOf(oldestDays)) {
                case HIGH -> "danger";
                case MEDIUM -> "warning";
                default -> "info";
            };
        }
    }

    // Tổng quá hạn của một nhân viên phụ trách
    public static class RepTotal {
        private final String name;
        private final BigDecimal amount;

        public RepTotal(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
