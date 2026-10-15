package com.oms.model;

import java.math.BigDecimal;

// Một dòng sổ công nợ theo đại lý (S7-03): phải thu, đã thu, còn nợ và còn nợ chia theo tuổi nợ tính từ ngày lập hoá đơn
public class DebtAgingRow {

    private final long customerId;
    private final String code;
    private final String name;
    private final BigDecimal receivable;
    private final BigDecimal paid;
    private final BigDecimal outstanding;
    private final BigDecimal bucket0To30;
    private final BigDecimal bucket31To60;
    private final BigDecimal bucket61To90;
    private final BigDecimal bucketOver90;
    private final BigDecimal overdue;

    public DebtAgingRow(long customerId, String code, String name, BigDecimal receivable, BigDecimal paid,
                        BigDecimal outstanding, BigDecimal bucket0To30, BigDecimal bucket31To60,
                        BigDecimal bucket61To90, BigDecimal bucketOver90, BigDecimal overdue) {
        this.customerId = customerId;
        this.code = code;
        this.name = name;
        this.receivable = receivable;
        this.paid = paid;
        this.outstanding = outstanding;
        this.bucket0To30 = bucket0To30;
        this.bucket31To60 = bucket31To60;
        this.bucket61To90 = bucket61To90;
        this.bucketOver90 = bucketOver90;
        this.overdue = overdue;
    }

    // Dòng tổng cộng ở chân bảng
    public static DebtAgingRow total(Iterable<DebtAgingRow> rows) {
        BigDecimal[] sum = new BigDecimal[8];
        java.util.Arrays.fill(sum, BigDecimal.ZERO);
        for (DebtAgingRow row : rows) {
            sum[0] = sum[0].add(row.receivable);
            sum[1] = sum[1].add(row.paid);
            sum[2] = sum[2].add(row.outstanding);
            sum[3] = sum[3].add(row.bucket0To30);
            sum[4] = sum[4].add(row.bucket31To60);
            sum[5] = sum[5].add(row.bucket61To90);
            sum[6] = sum[6].add(row.bucketOver90);
            sum[7] = sum[7].add(row.overdue);
        }
        return new DebtAgingRow(0, "", "Tổng cộng", sum[0], sum[1], sum[2], sum[3], sum[4], sum[5], sum[6], sum[7]);
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

    public BigDecimal getReceivable() {
        return receivable;
    }

    public BigDecimal getPaid() {
        return paid;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    public BigDecimal getBucket0To30() {
        return bucket0To30;
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

    public BigDecimal getOverdue() {
        return overdue;
    }
}
