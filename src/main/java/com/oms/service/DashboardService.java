package com.oms.service;

import com.oms.dao.ProductDao;
import com.oms.model.DashboardSummary;
import com.oms.model.ProductFilter;
import com.oms.model.RecentOrder;
import com.oms.model.StockStatus;

import java.sql.SQLException;
import java.util.List;

// Số liệu trang Tổng quan: chỉ hiện số thật đang có. Doanh thu, số đơn, đơn gần đây chờ chức năng đơn hàng
// (SCRUM-59) và dashboard đầy đủ (S8-05) mới có dữ liệu để tính.
public class DashboardService {

    private final ProductDao productDao = new ProductDao();

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
