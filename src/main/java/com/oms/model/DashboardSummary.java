package com.oms.model;

public class DashboardSummary {

    private final long todayRevenue;
    private final double revenueChangePercent;
    private final int todayOrderCount;
    private final double orderChangePercent;
    private final int lowStockProductCount;

    public DashboardSummary(long todayRevenue, double revenueChangePercent,
                            int todayOrderCount, double orderChangePercent,
                            int lowStockProductCount) {
        this.todayRevenue = todayRevenue;
        this.revenueChangePercent = revenueChangePercent;
        this.todayOrderCount = todayOrderCount;
        this.orderChangePercent = orderChangePercent;
        this.lowStockProductCount = lowStockProductCount;
    }

    public long getTodayRevenue() {
        return todayRevenue;
    }

    public double getRevenueChangePercent() {
        return revenueChangePercent;
    }

    public int getTodayOrderCount() {
        return todayOrderCount;
    }

    public double getOrderChangePercent() {
        return orderChangePercent;
    }

    public int getLowStockProductCount() {
        return lowStockProductCount;
    }
}
