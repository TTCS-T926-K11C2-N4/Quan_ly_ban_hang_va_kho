package com.oms.model;

import java.math.BigDecimal;

// Đại lý còn hoá đơn chưa thanh toán hết (S7-02)
public class Debtor {

    private final long customerId;
    private final String code;
    private final String name;
    private final int openInvoices;
    private final BigDecimal outstanding;

    public Debtor(long customerId, String code, String name, int openInvoices, BigDecimal outstanding) {
        this.customerId = customerId;
        this.code = code;
        this.name = name;
        this.openInvoices = openInvoices;
        this.outstanding = outstanding;
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

    public int getOpenInvoices() {
        return openInvoices;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }
}
