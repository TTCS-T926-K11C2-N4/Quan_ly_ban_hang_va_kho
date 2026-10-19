package com.oms.service;

import com.oms.dao.DashboardDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.ProductDao;
import com.oms.dao.StockLedgerDao;
import com.oms.model.DashboardSummary;
import com.oms.model.Permission;
import com.oms.model.ProductFilter;
import com.oms.model.RecentOrder;
import com.oms.model.StockLedgerFilter;
import com.oms.model.StockLedgerRow;
import com.oms.model.StockLevel;
import com.oms.model.StockStatus;
import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

// Số liệu trang Tổng quan, giới hạn theo phạm vi quyền của người xem: đơn hàng theo ORDER_VIEW, doanh thu theo
// INVOICE_VIEW (ALL: mọi đại lý; ASSIGNED: đại lý mình phụ trách; không có quyền: không hiện).
// Doanh thu tính theo hoá đơn phát hành trong ngày, khớp với dashboard điều hành và báo cáo doanh số.
public class DashboardService {

    private static final int LOW_STOCK_LIMIT = 5;
    private static final int RECENT_ORDER_LIMIT = 5;

    private final ProductDao productDao = new ProductDao();
    private final StockLedgerDao ledgerDao = new StockLedgerDao();
    private final DashboardDao dashboardDao = new DashboardDao();
    private final PermissionDao permissionDao = new PermissionDao();

    // Phạm vi xem: allowed = có quyền; salesRepId = người cần lọc theo (null khi xem mọi đại lý)
    private record Scope(boolean allowed, Long salesRepId) {
    }

    private Scope scopeOf(long userId, String permission) throws SQLException {
        String scope = permissionDao.findScope(userId, permission);
        return new Scope("ALL".equals(scope) || "ASSIGNED".equals(scope), "ASSIGNED".equals(scope) ? userId : null);
    }

    // S5-09: các SKU-kho đã chạm mức tồn tối thiểu (còn hàng nhưng khả dụng <= mức tối thiểu), cùng cách tính với sổ tồn
    public List<StockLedgerRow> getLowStockRows() throws SQLException {
        return ledgerDao.findPage(new StockLedgerFilter(null, null, null, StockLevel.LOW), 0, LOW_STOCK_LIMIT);
    }

    // Tổng số SKU-kho sắp hết, để hiện "Xem tất cả N" khi danh sách bị cắt
    public long countLowStockRows() throws SQLException {
        return ledgerDao.count(new StockLedgerFilter(null, null, null, StockLevel.LOW));
    }

    public DashboardSummary getSummary(long userId) throws SQLException {
        // Cùng cách tính trạng thái tồn với bộ lọc ở danh sách sản phẩm (tổng mọi kho)
        long needRestock = productDao.count(new ProductFilter(null, null, null, StockStatus.LOW_STOCK, null))
                + productDao.count(new ProductFilter(null, null, null, StockStatus.OUT_OF_STOCK, null));
        LocalDate today = DateTimeUtil.today();
        LocalDate yesterday = today.minusDays(1);

        Scope orders = scopeOf(userId, Permission.ORDER_VIEW);
        int todayOrders = 0;
        Double orderChange = null;
        if (orders.allowed()) {
            todayOrders = dashboardDao.countSubmittedOrders(DateTimeUtil.startOfDayUtc(today),
                    DateTimeUtil.startOfDayUtc(today.plusDays(1)), orders.salesRepId());
            int yesterdayOrders = dashboardDao.countSubmittedOrders(DateTimeUtil.startOfDayUtc(yesterday),
                    DateTimeUtil.startOfDayUtc(today), orders.salesRepId());
            orderChange = changePercent(BigDecimal.valueOf(todayOrders), BigDecimal.valueOf(yesterdayOrders));
        }

        Scope invoices = scopeOf(userId, Permission.INVOICE_VIEW);
        BigDecimal todayRevenue = BigDecimal.ZERO;
        Double revenueChange = null;
        if (invoices.allowed()) {
            todayRevenue = dashboardDao.sumInvoiceRevenue(today, invoices.salesRepId());
            revenueChange = changePercent(todayRevenue, dashboardDao.sumInvoiceRevenue(yesterday, invoices.salesRepId()));
        }
        return new DashboardSummary(orders.allowed(), todayOrders, orderChange, invoices.allowed(), todayRevenue,
                revenueChange, needRestock);
    }

    public List<RecentOrder> getRecentOrders(long userId) throws SQLException {
        Scope orders = scopeOf(userId, Permission.ORDER_VIEW);
        return orders.allowed() ? dashboardDao.findRecentOrders(orders.salesRepId(), RECENT_ORDER_LIMIT) : List.of();
    }

    // Phần trăm thay đổi so với hôm qua; null khi hôm qua bằng 0 vì không có mốc để so
    static Double changePercent(BigDecimal today, BigDecimal yesterday) {
        if (yesterday == null || yesterday.signum() == 0) {
            return null;
        }
        return today.subtract(yesterday).multiply(BigDecimal.valueOf(100))
                .divide(yesterday, 1, RoundingMode.HALF_UP).doubleValue();
    }
}
