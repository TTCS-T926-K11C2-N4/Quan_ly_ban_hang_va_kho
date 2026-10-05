package com.oms.model;

// Số liệu thẻ đầu trang Tổng quan. hasSalesData = false khi chưa có chức năng đơn hàng (SCRUM-59): doanh thu và số đơn
// hiện 0 kèm ghi chú thay vì số giả; lowStockProductCount là số thật (sắp hết + hết hàng).
public class DashboardSummary {

    private final long todayRevenue;
    private final double revenueChangePercent;
    private final int todayOrderCount;
    private final double orderChangePercent;
    private final long lowStockProductCount;
    private final boolean hasSalesData;

    public DashboardSummary(long todayRevenue, double revenueChangePercent,
                            int todayOrderCount, double orderChangePercent,
                            long lowStockProductCount, boolean hasSalesData) {
        this.todayRevenue = todayRevenue;
        this.revenueChangePercent = revenueChangePercent;
        this.todayOrderCount = todayOrderCount;
        this.orderChangePercent = orderChangePercent;
        this.lowStockProductCount = lowStockProductCount;
        this.hasSalesData = hasSalesData;
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

    public long getLowStockProductCount() {
        return lowStockProductCount;
    }

    public boolean isHasSalesData() {
        return hasSalesData;
    }
}
