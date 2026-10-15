package com.oms.service;

import com.oms.dao.ProductDao;
import com.oms.dao.StockLedgerDao;
import com.oms.model.DashboardSummary;
import com.oms.model.ProductFilter;
import com.oms.model.RecentOrder;
import com.oms.model.StockLedgerFilter;
import com.oms.model.StockLedgerRow;
import com.oms.model.StockLevel;
import com.oms.model.StockStatus;

import java.sql.SQLException;
import java.util.List;

// Số liệu trang Tổng quan: chỉ hiện số thật đang có. Doanh thu, số đơn, đơn gần đây chờ chức năng đơn hàng
// (SCRUM-59) và dashboard đầy đủ (S8-05) mới có dữ liệu để tính.
public class DashboardService {

    private static final int LOW_STOCK_LIMIT = 5;

    private final ProductDao productDao = new ProductDao();
    private final StockLedgerDao ledgerDao = new StockLedgerDao();

    // S5-09: các SKU-kho đã chạm mức tồn tối thiểu (còn hàng nhưng khả dụng <= mức tối thiểu), cùng cách tính với sổ tồn
    public List<StockLedgerRow> getLowStockRows() throws SQLException {
        return ledgerDao.findPage(new StockLedgerFilter(null, null, null, StockLevel.LOW), 0, LOW_STOCK_LIMIT);
    }

    // Tổng số SKU-kho sắp hết, để hiện "Xem tất cả N" khi danh sách bị cắt
    public long countLowStockRows() throws SQLException {
        return ledgerDao.count(new StockLedgerFilter(null, null, null, StockLevel.LOW));
    }

    public DashboardSummary getSummary() throws SQLException {
        // Cùng cách tính trạng thái tồn với bộ lọc ở danh sách sản phẩm (tổng mọi kho)
        long needRestock = productDao.count(new ProductFilter(null, null, null, StockStatus.LOW_STOCK, null))
                + productDao.count(new ProductFilter(null, null, null, StockStatus.OUT_OF_STOCK, null));
        return new DashboardSummary(0, 0, 0, 0, needRestock, false);
    }

    public List<RecentOrder> getRecentOrders() {
        return List.of();
    }
}
