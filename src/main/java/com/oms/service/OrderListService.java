package com.oms.service;

import com.oms.dao.OrderListDao;
import com.oms.dao.PermissionDao;
import com.oms.model.OrderListFilter;
import com.oms.model.OrderListRow;
import com.oms.model.PageResult;
import com.oms.model.Permission;

import java.sql.SQLException;
import java.util.List;

// Danh sách đơn hàng (S4-07). Phạm vi xem theo ORDER_VIEW: ALL xem mọi đơn; ASSIGNED (Nhân viên kinh doanh) chỉ
// đơn của đại lý mình phụ trách; còn lại không thấy đơn nào. Phạm vi do server quyết định, không đọc từ tham số.
public class OrderListService {

    private final OrderListDao listDao = new OrderListDao();
    private final PermissionDao permissionDao = new PermissionDao();

    // Kết quả một lần tìm: trang dữ liệu, tổng tiền cả tập đang lọc, và người xem có thấy mọi đơn không
    public static final class Result {
        private final PageResult<OrderListRow> page;
        private final OrderListDao.Totals totals;
        private final boolean canViewAll;

        Result(PageResult<OrderListRow> page, OrderListDao.Totals totals, boolean canViewAll) {
            this.page = page;
            this.totals = totals;
            this.canViewAll = canViewAll;
        }

        public PageResult<OrderListRow> getPage() {
            return page;
        }

        public OrderListDao.Totals getTotals() {
            return totals;
        }

        public boolean isCanViewAll() {
            return canViewAll;
        }
    }

    public boolean canViewAll(long userId) throws SQLException {
        return "ALL".equals(permissionDao.findScope(userId, Permission.ORDER_VIEW));
    }

    // Nhân viên kinh doanh bỏ qua ô lọc Nhân viên vì chỉ thấy đại lý của mình (AC3)
    public Result search(long userId, OrderListFilter filter, int page) throws SQLException {
        String scope = permissionDao.findScope(userId, Permission.ORDER_VIEW);
        if ("ALL".equals(scope)) {
            return new Result(listDao.search(filter, null, page), listDao.totals(filter, null), true);
        }
        if ("ASSIGNED".equals(scope)) {
            return new Result(listDao.search(filter, userId, page), listDao.totals(filter, userId), false);
        }
        PageResult<OrderListRow> empty = new PageResult<>(List.of(), 1, filter.getPageSize(), 0);
        return new Result(empty, new OrderListDao.Totals(java.math.BigDecimal.ZERO, 0), false);
    }
}
