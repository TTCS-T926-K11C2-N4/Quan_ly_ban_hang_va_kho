package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.OrderClosureDao;
import com.oms.dao.PickListDao;
import com.oms.dao.SalesOrderDao;
import com.oms.model.OrderStatus;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

// Tự đóng đơn: đơn Đã giao chuyển "Đóng" khi đã giao đủ, hoá đơn đã lập và mọi hoá đơn của đơn đã thanh toán đủ.
// Gọi trong transaction thu tiền / giảm trừ công nợ, sau khi hoá đơn đã cập nhật trạng thái, để đơn đóng cùng lúc
// hoá đơn cuối cùng được thanh toán đủ.
public class OrderClosureService {

    private static final String AUDIT_ENTITY = "ORDER";

    private final OrderClosureDao closureDao = new OrderClosureDao();
    private final PickListDao pickListDao = new PickListDao();
    private final SalesOrderDao salesOrderDao = new SalesOrderDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Trả về số đơn vừa đóng
    public int closeSettled(Connection connection, long customerId, long actorUserId, String ipAddress)
            throws SQLException {
        int closed = 0;
        for (OrderClosureDao.SettledOrder order : closureDao.lockSettled(connection, customerId)) {
            if (!pickListDao.changeOrderStatus(connection, order.id(), OrderStatus.DELIVERED, OrderStatus.CLOSED,
                    actorUserId)) {
                continue;
            }
            salesOrderDao.insertStatusHistory(connection, order.id(), OrderStatus.DELIVERED, OrderStatus.CLOSED,
                    "Đã giao đủ, hoá đơn đã thanh toán đủ", actorUserId);
            auditLogDao.insert(connection, actorUserId, "ORDER_CLOSE", AUDIT_ENTITY, order.id(),
                    JsonUtil.object(Map.of("orderNo", order.orderNo(), "status", OrderStatus.DELIVERED)),
                    JsonUtil.object(Map.of("orderNo", order.orderNo(), "status", OrderStatus.CLOSED)),
                    "Đã giao đủ, hoá đơn đã thanh toán đủ", ipAddress);
            closed++;
        }
        return closed;
    }
}
