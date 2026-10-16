package com.oms.service;

import com.oms.dao.ApprovalDao;
import com.oms.dao.AuditLogDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.SalesOrderDao;
import com.oms.model.ApprovalRequest;
import com.oms.model.DraftOrder;
import com.oms.model.OrderDetail;
import com.oms.model.OrderStatus;
import com.oms.model.Permission;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Vòng đời và huỷ đơn hàng (S4-06). Xem đơn theo phạm vi ORDER_VIEW (Nhân viên kinh doanh: đơn của đại lý mình
// phụ trách); huỷ theo ORDER_MANAGE khi đơn chưa xuất kho. Huỷ = đổi trạng thái + nhả tồn đang giữ chỗ
// + ghi lịch sử trạng thái + nhật ký thao tác, cùng một transaction.
public class OrderLifecycleService {

    public static final int REASON_MAX_LENGTH = 500;
    private static final int RECENT_LIMIT = 20;
    private static final String AUDIT_ENTITY = "ORDER";
    private static final String RELEASE_REASON = "ORDER_CANCELLED";

    private final SalesOrderDao orderDao = new SalesOrderDao();
    private final CustomerService customerService = new CustomerService();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();
    private final ApprovalDao approvalDao = new ApprovalDao();

    // null nếu không có đơn này hoặc người dùng không được xem đại lý của đơn (trả 404 như nhau)
    public OrderDetail findVisible(long userId, long orderId) throws SQLException {
        OrderDetail order = orderDao.findDetail(orderId);
        if (order == null
                || customerService.findVisible(userId, Permission.ORDER_VIEW, order.getCustomerId()) == null) {
            return null;
        }
        return order;
    }

    // Người dùng được huỷ đơn này: có ORDER_MANAGE với đại lý của đơn và đơn còn ở trạng thái huỷ được
    public boolean canCancel(long userId, OrderDetail order) throws SQLException {
        return order.isCancellable()
                && customerService.findVisible(userId, Permission.ORDER_MANAGE, order.getCustomerId()) != null;
    }

    // Đơn do đại lý tự đặt đang chờ nhân viên phụ trách xác nhận (S4-10), và người dùng quản lý được đại lý của đơn
    public boolean canRepConfirm(long userId, OrderDetail order) throws SQLException {
        return OrderStatus.PENDING_APPROVAL.equals(order.getStatus())
                && ApprovalDao.REP_CONFIRMATION.equals(approvalDao.findPendingType(order.getId()))
                && customerService.findVisible(userId, Permission.ORDER_MANAGE, order.getCustomerId()) != null;
    }

    // Đơn đã gửi gần đây cho hộp "Mở lại" của màn tạo đơn, cùng phạm vi như đơn nháp
    public List<DraftOrder> findVisibleRecent(long userId) throws SQLException {
        String scope = permissionDao.findScope(userId, Permission.ORDER_VIEW);
        if ("ALL".equals(scope)) {
            return orderDao.findRecentSubmitted(null, RECENT_LIMIT);
        }
        return "ASSIGNED".equals(scope) ? orderDao.findRecentSubmitted(userId, RECENT_LIMIT) : List.of();
    }

    // Lỗi của lý do huỷ; null là hợp lệ
    public String validateReason(String reason) {
        if (reason == null) {
            return "Vui lòng nhập lý do huỷ đơn.";
        }
        if (reason.length() > REASON_MAX_LENGTH) {
            return "Lý do huỷ tối đa " + REASON_MAX_LENGTH + " ký tự.";
        }
        return null;
    }

    // Gọi canCancel và validateReason trước. false nếu đơn vừa đổi trạng thái hoặc được sửa ở nơi khác
    public boolean cancel(OrderDetail order, long version, String reason, long actorUserId, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!orderDao.cancel(connection, order.getId(), version, order.getStatus(), reason, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                int released = orderDao.releaseReservations(connection, order.getId(), RELEASE_REASON);
                // Đơn đang chờ duyệt (S4-05): đóng yêu cầu duyệt cùng lúc để lịch sử duyệt không treo ở "Chờ"
                if (OrderStatus.PENDING_APPROVAL.equals(order.getStatus())) {
                    approvalDao.decide(connection, order.getId(), ApprovalRequest.REJECTED,
                            "Huỷ đơn trước khi duyệt: " + reason, actorUserId);
                }
                orderDao.insertStatusHistory(connection, order.getId(), order.getStatus(), OrderStatus.CANCELLED,
                        reason, actorUserId);
                Map<String, Object> newValues = values(order.getOrderNo(), OrderStatus.CANCELLED);
                newValues.put("releasedReservations", released);
                auditLogDao.insert(connection, actorUserId, "ORDER_CANCEL", AUDIT_ENTITY, order.getId(),
                        JsonUtil.object(values(order.getOrderNo(), order.getStatus())), JsonUtil.object(newValues),
                        reason, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static Map<String, Object> values(String orderNo, String status) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("orderNo", orderNo);
        values.put("status", status);
        return values;
    }
}
