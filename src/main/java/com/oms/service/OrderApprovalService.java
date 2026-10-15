package com.oms.service;

import com.oms.dao.ApprovalDao;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerCreditDao;
import com.oms.dao.SalesOrderDao;
import com.oms.model.ApprovalQueueRow;
import com.oms.model.ApprovalRequest;
import com.oms.model.CustomerCredit;
import com.oms.model.OrderDetail;
import com.oms.model.OrderQuote;
import com.oms.model.OrderStatus;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Duyệt đơn vượt hạn mức hoặc bán dưới giá sàn (S4-05). Đơn không vi phạm được hệ thống tự duyệt ngay lúc gửi;
// đơn vi phạm chờ Quản lý kinh doanh: Duyệt (giữ chỗ tồn bắt đầu tính hạn, chờ xuất kho), Từ chối (kết thúc) hoặc
// Trả lại sửa (về Nháp). Từ chối / Trả lại nhả tồn đang giữ chỗ và bắt buộc ý kiến. Mỗi quyết định: đổi trạng thái
// đơn + ghi quyết định vào approval_requests (chỉ một lần) + lịch sử trạng thái + nhật ký thao tác, cùng transaction.
public class OrderApprovalService {

    public static final int COMMENT_MAX_LENGTH = 500;
    private static final String AUDIT_ENTITY = "ORDER";
    static final String AUTO_APPROVE_NOTE = "Hệ thống tự duyệt: không vượt hạn mức, không dưới giá sàn";

    public enum Decision {
        APPROVE(ApprovalRequest.APPROVED, OrderStatus.APPROVED, "ORDER_APPROVE", "Duyệt đơn", null),
        REJECT(ApprovalRequest.REJECTED, OrderStatus.REJECTED, "ORDER_REJECT", "Từ chối đơn", "ORDER_REJECTED"),
        RETURN(ApprovalRequest.RETURNED, OrderStatus.DRAFT, "ORDER_RETURN", "Trả lại sửa", "ORDER_RETURNED");

        private final String requestStatus;
        private final String orderStatus;
        private final String auditAction;
        private final String label;
        private final String releaseReason;

        Decision(String requestStatus, String orderStatus, String auditAction, String label, String releaseReason) {
            this.requestStatus = requestStatus;
            this.orderStatus = orderStatus;
            this.auditAction = auditAction;
            this.label = label;
            this.releaseReason = releaseReason;
        }

        // null nếu không phải approve | reject | return
        public static Decision of(String code) {
            if (code == null) {
                return null;
            }
            return switch (code) {
                case "approve" -> APPROVE;
                case "reject" -> REJECT;
                case "return" -> RETURN;
                default -> null;
            };
        }

        public boolean isCommentRequired() {
            return this != APPROVE;
        }

        public String getLabel() {
            return label;
        }
    }

    private final ApprovalDao approvalDao = new ApprovalDao();
    private final SalesOrderDao orderDao = new SalesOrderDao();
    private final StockReservationService reservationService = new StockReservationService();
    private final AuditLogDao auditLogDao = new AuditLogDao();
    private final CustomerCreditDao creditDao = new CustomerCreditDao();

    public int countPending() throws SQLException {
        return approvalDao.countPending();
    }

    public List<ApprovalQueueRow> findQueue(String keyword) throws SQLException {
        return approvalDao.findQueue(keyword);
    }

    // null nếu không có đơn; Quản lý kinh doanh / Kế toán xem được đơn của mọi đại lý
    public OrderDetail findOrder(long orderId) throws SQLException {
        return orderDao.findDetail(orderId);
    }

    // Công nợ hiện tại của đại lý (đã gồm đơn đang chờ duyệt này) để quản lý cân nhắc khi duyệt
    public CustomerCredit findCredit(long customerId) throws SQLException {
        return creditDao.find(customerId, DateTimeUtil.today());
    }

    public List<ApprovalRequest> findHistory(long orderId) throws SQLException {
        return approvalDao.findHistory(orderId);
    }

    // Đơn đang chờ nhân viên phụ trách xác nhận (đơn từ cổng đại lý)
    public boolean isAwaitingRepConfirmation(long orderId) throws SQLException {
        return ApprovalDao.REP_CONFIRMATION.equals(approvalDao.findPendingType(orderId));
    }

    public String findLatestReturnComment(long orderId) throws SQLException {
        return approvalDao.findLatestReturnComment(orderId);
    }

    // Lỗi của ý kiến; null là hợp lệ. Từ chối / Trả lại sửa bắt buộc ý kiến (AC2), Duyệt thì không bắt buộc
    public static String validateComment(Decision decision, String comment) {
        if (comment == null) {
            return decision.isCommentRequired() ? "Vui lòng nhập ý kiến khi " + decision.getLabel().toLowerCase()
                    + "." : null;
        }
        if (comment.length() > COMMENT_MAX_LENGTH) {
            return "Ý kiến tối đa " + COMMENT_MAX_LENGTH + " ký tự.";
        }
        return null;
    }

    // Gọi validateComment trước. false nếu đơn vừa được người khác quyết định hoặc đổi trạng thái (version đã đổi).
    // Duyệt mà kho không còn đủ hàng cho dòng chưa giữ chỗ thì InsufficientStockException, không đổi gì.
    public boolean decide(OrderDetail order, long version, Decision decision, String comment, long actorUserId,
                          String ipAddress) throws SQLException, StockReservationService.InsufficientStockException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!orderDao.changeStatus(connection, order.getId(), version, OrderStatus.PENDING_APPROVAL,
                        decision.orderStatus, actorUserId)
                        || !approvalDao.decide(connection, order.getId(), decision.requestStatus, comment,
                        actorUserId)) {
                    connection.rollback();
                    return false;
                }
                Map<String, Object> newValues = values(order.getOrderNo(), decision.orderStatus);
                if (decision == Decision.APPROVE) {
                    newValues.put("reservedLines", reservationService.reserve(connection, order.getId(), actorUserId));
                } else {
                    newValues.put("releasedReservations",
                            orderDao.releaseReservations(connection, order.getId(), decision.releaseReason));
                }
                orderDao.insertStatusHistory(connection, order.getId(), OrderStatus.PENDING_APPROVAL,
                        decision.orderStatus, note(decision.label, comment), actorUserId);
                auditLogDao.insert(connection, actorUserId, decision.auditAction, AUDIT_ENTITY, order.getId(),
                        JsonUtil.object(values(order.getOrderNo(), OrderStatus.PENDING_APPROVAL)),
                        JsonUtil.object(newValues), comment, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException | StockReservationService.InsufficientStockException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Trong transaction gửi đơn (SalesOrderService): đơn vi phạm thì tạo yêu cầu duyệt, không vi phạm thì tự duyệt.
    // Trả về true nếu đơn đã được duyệt luôn.
    boolean afterSubmit(Connection connection, long orderId, String orderNo, OrderQuote quote, CustomerCredit credit,
                        long actorUserId, String ipAddress)
            throws SQLException, StockReservationService.InsufficientStockException {
        return afterSubmit(connection, orderId, orderNo, quote, credit, actorUserId, ipAddress, false);
    }

    // portal: đơn do đại lý tự đặt (S4-10) luôn ở Chờ duyệt dù không vi phạm, chờ nhân viên phụ trách xác nhận
    boolean afterSubmit(Connection connection, long orderId, String orderNo, OrderQuote quote, CustomerCredit credit,
                        long actorUserId, String ipAddress, boolean portal)
            throws SQLException, StockReservationService.InsufficientStockException {
        List<Map<String, Object>> violations = violations(quote, credit);
        if (portal) {
            approvalDao.insertRequest(connection, orderId, ApprovalDao.REP_CONFIRMATION,
                    JsonUtil.array(violations), actorUserId);
            return false;
        }
        if (!violations.isEmpty()) {
            approvalDao.insertRequest(connection, orderId, JsonUtil.array(violations), actorUserId);
            return false;
        }
        orderDao.changeStatus(connection, orderId, null, OrderStatus.PENDING_APPROVAL, OrderStatus.APPROVED,
                actorUserId);
        Map<String, Object> newValues = values(orderNo, OrderStatus.APPROVED);
        newValues.put("reservedLines", reservationService.reserve(connection, orderId, actorUserId));
        newValues.put("autoApproved", true);
        orderDao.insertStatusHistory(connection, orderId, OrderStatus.PENDING_APPROVAL, OrderStatus.APPROVED,
                AUTO_APPROVE_NOTE, actorUserId);
        auditLogDao.insert(connection, actorUserId, "ORDER_APPROVE", AUDIT_ENTITY, orderId,
                JsonUtil.object(values(orderNo, OrderStatus.PENDING_APPROVAL)), JsonUtil.object(newValues),
                AUTO_APPROVE_NOTE, ipAddress);
        return true;
    }

    // Vi phạm chụp lại lúc gửi (AC1: lý do và mức vi phạm). credit: công nợ đã khoá trong transaction gửi đơn,
    // chưa tính đơn đang gửi. Giá theo đơn vị đặt; giá thực bán là giá sau chiết khấu.
    static List<Map<String, Object>> violations(OrderQuote quote, CustomerCredit credit) {
        List<Map<String, Object>> violations = new ArrayList<>();
        if (credit != null && credit.exceeds(quote.getTotal())) {
            Map<String, Object> over = new LinkedHashMap<>();
            over.put("type", ApprovalRequest.Violation.OVER_CREDIT_LIMIT);
            over.put("exceeded_by", plain(credit.getExceededBy(quote.getTotal())));
            over.put("credit_limit", plain(credit.getCreditLimit()));
            over.put("used", plain(credit.getUsed()));
            violations.add(over);
        }
        for (int i = 0; i < quote.getLines().size(); i++) {
            OrderQuote.Line line = quote.getLines().get(i);
            if (line.isBelowFloor()) {
                Map<String, Object> below = new LinkedHashMap<>();
                below.put("type", ApprovalRequest.Violation.BELOW_FLOOR_PRICE);
                below.put("line_no", i + 1);
                below.put("sku", line.getProduct().getSku());
                below.put("unit_price", plain(line.getUnitPrice()));
                below.put("net_price", plain(line.getNetUnitPrice()));
                below.put("floor_price", plain(line.getFloorPrice()));
                violations.add(below);
            }
        }
        return violations;
    }

    // Bỏ số 0 thừa nhưng không ra dạng 5E+6 trong JSON
    private static BigDecimal plain(BigDecimal amount) {
        return new BigDecimal(amount.stripTrailingZeros().toPlainString());
    }

    private static String note(String label, String comment) {
        return comment == null ? label : label + ": " + comment;
    }

    private static Map<String, Object> values(String orderNo, String status) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("orderNo", orderNo);
        values.put("status", status);
        return values;
    }
}
