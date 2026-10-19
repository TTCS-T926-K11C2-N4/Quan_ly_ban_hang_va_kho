package com.oms.model;

import java.math.BigDecimal;

// Số liệu thẻ đầu trang Tổng quan, đã giới hạn theo phạm vi xem của người dùng. showOrders / showRevenue = false khi
// người dùng không có quyền xem đơn hàng / hoá đơn (vd nhân viên kho không xem doanh thu). Phần trăm so với hôm qua
// là null khi hôm qua bằng 0 (không chia được).
public class DashboardSummary {

    private final boolean showOrders;
    private final int todayOrderCount;
    private final Double orderChangePercent;
    private final boolean showRevenue;
    private final BigDecimal todayRevenue;
    private final Double revenueChangePercent;
    private final long lowStockProductCount;

    public DashboardSummary(boolean showOrders, int todayOrderCount, Double orderChangePercent, boolean showRevenue,
                            BigDecimal todayRevenue, Double revenueChangePercent, long lowStockProductCount) {
        this.showOrders = showOrders;
        this.todayOrderCount = todayOrderCount;
        this.orderChangePercent = orderChangePercent;
        this.showRevenue = showRevenue;
        this.todayRevenue = todayRevenue;
        this.revenueChangePercent = revenueChangePercent;
        this.lowStockProductCount = lowStockProductCount;
    }

    public boolean isShowOrders() {
        return showOrders;
    }

    public int getTodayOrderCount() {
        return todayOrderCount;
    }

    public Double getOrderChangePercent() {
        return orderChangePercent;
    }

    public boolean isShowRevenue() {
        return showRevenue;
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public Double getRevenueChangePercent() {
        return revenueChangePercent;
    }

    public long getLowStockProductCount() {
        return lowStockProductCount;
    }
}
