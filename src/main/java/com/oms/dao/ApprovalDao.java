package com.oms.dao;

import com.oms.model.ApprovalQueueRow;
import com.oms.model.ApprovalRequest;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Yêu cầu duyệt đơn (approval_requests, S4-05). Mỗi lần gửi đơn vi phạm là một dòng PENDING; quyết định chỉ ghi được
// một lần (trigger trg_approval_requests_decide_once), không xoá được. Vi phạm lưu dạng JSON, đọc lại bằng JSON_TABLE.
public class ApprovalDao {

    private static final String VIOLATIONS_TABLE = "JSON_TABLE(ar.violations, '$[*]' COLUMNS ("
            + "type VARCHAR(30) PATH '$.type', exceeded_by DECIMAL(18,2) PATH '$.exceeded_by',"
            + " credit_limit DECIMAL(18,2) PATH '$.credit_limit', used DECIMAL(18,2) PATH '$.used',"
            + " line_no INT PATH '$.line_no', sku VARCHAR(50) PATH '$.sku', unit_price DECIMAL(18,2) PATH '$.unit_price',"
            + " net_price DECIMAL(18,2) PATH '$.net_price', floor_price DECIMAL(18,2) PATH '$.floor_price')) jt";

    public static final String MANAGER_EXCEPTION = "MANAGER_EXCEPTION";
    // Đơn do đại lý tự đặt trên cổng (S4-10): nhân viên phụ trách xác nhận
    public static final String REP_CONFIRMATION = "REP_CONFIRMATION";

    public void insertRequest(Connection connection, long orderId, String violationsJson, long requestedBy)
            throws SQLException {
        insertRequest(connection, orderId, MANAGER_EXCEPTION, violationsJson, requestedBy);
    }

    public void insertRequest(Connection connection, long orderId, String approvalType, String violationsJson,
                              long requestedBy) throws SQLException {
        String sql = "INSERT INTO approval_requests (order_id, approval_type, violations, status, requested_by,"
                + " requested_at) VALUES (?, ?, ?, 'PENDING', ?, UTC_TIMESTAMP())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setString(2, approvalType);
            statement.setString(3, violationsJson);
            statement.setLong(4, requestedBy);
            statement.executeUpdate();
        }
    }

    // Loại yêu cầu duyệt đang chờ của đơn; null nếu đơn không có yêu cầu chờ
    public String findPendingType(long orderId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT approval_type FROM approval_requests WHERE order_id = ? AND status = 'PENDING'")) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    // Ghi quyết định cho yêu cầu đang chờ của đơn; false nếu đơn không còn yêu cầu chờ (đã được người khác quyết định)
    public boolean decide(Connection connection, long orderId, String status, String comment, long actorUserId)
            throws SQLException {
        String sql = "UPDATE approval_requests SET status = ?, decided_by = ?, decided_at = UTC_TIMESTAMP(),"
                + " decision_comment = ? WHERE order_id = ? AND status = 'PENDING'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, actorUserId);
            statement.setString(3, comment);
            statement.setLong(4, orderId);
            return statement.executeUpdate() == 1;
        }
    }

    // Số đơn đang chờ duyệt, cho số đếm trên menu "Duyệt đơn"
    public int countPending() throws SQLException {
        String sql = "SELECT COUNT(*) FROM approval_requests ar JOIN sales_orders o ON o.id = ar.order_id"
                + " WHERE ar.status = 'PENDING' AND o.status = 'PENDING_APPROVAL'";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    // Đơn chờ duyệt, cũ nhất trước; keyword: mã đơn, tên hoặc mã đại lý (null là tất cả)
    public List<ApprovalQueueRow> findQueue(String keyword) throws SQLException {
        String sql = "SELECT o.id, o.order_no, c.name, c.code, rep.full_name, o.total_amount, ar.requested_at,"
                + " (SELECT MAX(jt.exceeded_by) FROM " + VIOLATIONS_TABLE + " WHERE jt.type = 'OVER_CREDIT_LIMIT')"
                + " AS exceeded_by,"
                + " (SELECT COUNT(*) FROM " + VIOLATIONS_TABLE + " WHERE jt.type = 'BELOW_FLOOR_PRICE') AS below_floor"
                + " FROM approval_requests ar JOIN sales_orders o ON o.id = ar.order_id"
                + " JOIN customers c ON c.id = o.customer_id LEFT JOIN users rep ON rep.id = o.sales_rep_id"
                + " WHERE ar.status = 'PENDING' AND o.status = 'PENDING_APPROVAL'"
                + " AND (? IS NULL OR o.order_no LIKE ? OR c.name LIKE ? OR c.code LIKE ?)"
                + " ORDER BY ar.requested_at, o.id";
        List<ApprovalQueueRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = keyword == null ? null : "%" + keyword + "%";
            statement.setString(1, keyword);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new ApprovalQueueRow(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(3), resultSet.getString(4), resultSet.getString(5),
                            resultSet.getBigDecimal(6), resultSet.getObject(7, LocalDateTime.class),
                            resultSet.getBigDecimal(8), resultSet.getInt(9)));
                }
            }
        }
        return rows;
    }

    // Mọi lần gửi duyệt của đơn, mới nhất trước (lịch sử duyệt, AC4: chỉ xem)
    public List<ApprovalRequest> findHistory(long orderId) throws SQLException {
        String headSql = "SELECT ar.id, ar.status, ar.requested_at, rb.full_name, ar.decided_at, db.full_name,"
                + " ar.decision_comment FROM approval_requests ar JOIN users rb ON rb.id = ar.requested_by"
                + " LEFT JOIN users db ON db.id = ar.decided_by WHERE ar.order_id = ? ORDER BY ar.requested_at DESC, ar.id DESC";
        String violationSql = "SELECT ar.id, jt.type, jt.exceeded_by, jt.credit_limit, jt.used, jt.line_no, jt.sku,"
                + " jt.unit_price, jt.net_price, jt.floor_price FROM approval_requests ar, " + VIOLATIONS_TABLE
                + " WHERE ar.order_id = ? ORDER BY ar.id, jt.type DESC, jt.line_no";
        try (Connection connection = DbConnection.getConnection()) {
            Map<Long, List<ApprovalRequest.Violation>> violations = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(violationSql)) {
                statement.setLong(1, orderId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Integer lineNo = resultSet.getObject(6, Integer.class);
                        violations.computeIfAbsent(resultSet.getLong(1), id -> new ArrayList<>())
                                .add(new ApprovalRequest.Violation(resultSet.getString(2), resultSet.getBigDecimal(3),
                                        resultSet.getBigDecimal(4), resultSet.getBigDecimal(5), lineNo,
                                        resultSet.getString(7),
                                        resultSet.getBigDecimal(8), resultSet.getBigDecimal(9),
                                        resultSet.getBigDecimal(10)));
                    }
                }
            }
            List<ApprovalRequest> requests = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(headSql)) {
                statement.setLong(1, orderId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        long id = resultSet.getLong(1);
                        requests.add(new ApprovalRequest(id, resultSet.getString(2),
                                violations.getOrDefault(id, List.of()), resultSet.getObject(3, LocalDateTime.class),
                                resultSet.getString(4), resultSet.getObject(5, LocalDateTime.class),
                                resultSet.getString(6), resultSet.getString(7)));
                    }
                }
            }
            return requests;
        }
    }

    // Ý kiến của lần Trả lại sửa gần nhất, để hiện trên đơn nháp khi nhân viên mở lại (null nếu không có)
    public String findLatestReturnComment(long orderId) throws SQLException {
        String sql = "SELECT decision_comment FROM approval_requests WHERE order_id = ? AND status = 'RETURNED'"
                + " AND NOT EXISTS (SELECT 1 FROM approval_requests p WHERE p.order_id = ? AND p.status = 'PENDING')"
                + " ORDER BY decided_at DESC, id DESC LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }
}
