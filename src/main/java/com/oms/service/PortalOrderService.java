package com.oms.service;

import com.oms.dao.PortalOrderDao;
import com.oms.model.PortalOrder;

import java.sql.SQLException;
import java.util.List;

// Đại lý theo dõi đơn hàng của chính mình trên cổng đại lý (S5-01). Đại lý được lấy từ tài khoản đăng nhập
// (users.customer_id), không bao giờ từ tham số của trang, nên không thể xem đơn của đại lý khác.
public class PortalOrderService {

    private static final int LIST_LIMIT = 100;

    private final PortalOrderDao orderDao = new PortalOrderDao();

    // Tài khoản chưa gắn đại lý thì không có đơn nào
    public List<PortalOrder> findOrders(long userId) throws SQLException {
        Long customerId = orderDao.findCustomerId(userId);
        return customerId == null ? List.of() : orderDao.findOrders(customerId, LIST_LIMIT);
    }

    // null nếu không có đơn này hoặc đơn của đại lý khác
    public PortalOrder findOrder(long userId, long orderId) throws SQLException {
        Long customerId = orderDao.findCustomerId(userId);
        return customerId == null ? null : orderDao.findOrder(customerId, orderId);
    }
}
