package com.oms.model;

public class RecentOrder {

    private final long id;
    private final String code;
    private final String customerName;
    private final String statusLabel;
    private final long totalAmount;

    public RecentOrder(long id, String code, String customerName, String statusLabel, long totalAmount) {
        this.id = id;
        this.code = code;
        this.customerName = customerName;
        this.statusLabel = statusLabel;
        this.totalAmount = totalAmount;
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public long getTotalAmount() {
        return totalAmount;
    }
}
