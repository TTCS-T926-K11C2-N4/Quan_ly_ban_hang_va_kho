package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerBlockDao;
import com.oms.model.Customer;
import com.oms.model.CustomerBlockEntry;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

// Khoá / mở giao dịch với đại lý (S3-07). Đại lý bị khoá không tạo được đơn mới (kiểm ở SalesOrderService),
// đơn đang dở vẫn xử lý tiếp nhưng có cảnh báo. Mỗi lần khoá/mở bắt buộc có lý do, ghi customer_block_history
// và nhật ký thao tác trong cùng transaction.
public class CustomerBlockService {

    public static final int REASON_MAX_LENGTH = 500;
    private static final String ENTITY = "CUSTOMER";

    private final CustomerBlockDao customerBlockDao = new CustomerBlockDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public CustomerBlockEntry findLatest(long customerId) throws SQLException {
        return customerBlockDao.findLatestBlock(customerId);
    }

    public int countOpenOrders(long customerId) throws SQLException {
        return customerBlockDao.countOpenOrders(customerId);
    }

    // null nếu hợp lệ
    public String validateReason(String reason, boolean blocking) {
        if (reason == null) {
            return blocking ? "Vui lòng nhập lý do khoá giao dịch." : "Vui lòng nhập lý do mở lại giao dịch.";
        }
        return reason.length() > REASON_MAX_LENGTH ? "Lý do tối đa " + REASON_MAX_LENGTH + " ký tự." : null;
    }

    // Gọi validateReason trước. false nếu đại lý đã ở trạng thái đó (người khác vừa khoá/mở).
    public boolean changeBlocked(Customer customer, boolean blocking, String reason, long actorUserId,
                                 String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!customerBlockDao.updateBlocked(connection, customer.getId(), blocking, reason, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                customerBlockDao.insertBlockHistory(connection, customer.getId(),
                        blocking ? CustomerBlockEntry.BLOCK : CustomerBlockEntry.UNBLOCK, reason, actorUserId);
                auditLogDao.insert(connection, actorUserId, blocking ? "CUSTOMER_BLOCK" : "CUSTOMER_UNBLOCK", ENTITY,
                        customer.getId(), JsonUtil.object(toValues(customer.getCode(), customer.isBlocked())),
                        JsonUtil.object(toValues(customer.getCode(), blocking)), reason, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static Map<String, Object> toValues(String code, boolean blocked) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("blocked", blocked);
        return values;
    }
}
