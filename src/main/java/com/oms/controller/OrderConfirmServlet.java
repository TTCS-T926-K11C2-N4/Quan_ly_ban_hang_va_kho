package com.oms.controller;

import com.oms.model.OrderDetail;
import com.oms.service.OrderApprovalService;
import com.oms.service.OrderLifecycleService;
import com.oms.service.StockReservationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S4-10: nhân viên phụ trách xác nhận hoặc từ chối đơn do đại lý tự đặt trên cổng (approval_type REP_CONFIRMATION).
// Xác nhận = duyệt đơn (giữ chỗ tồn tính hạn); từ chối bắt buộc ý kiến. Quyền: ORDER_MANAGE với đại lý của đơn.
@WebServlet("/orders/confirm")
public class OrderConfirmServlet extends HttpServlet {

    private final OrderLifecycleService lifecycle = new OrderLifecycleService();
    private final OrderApprovalService approvalService = new OrderApprovalService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        OrderApprovalService.Decision decision = "reject".equals(request.getParameter("decision"))
                ? OrderApprovalService.Decision.REJECT : "approve".equals(request.getParameter("decision"))
                ? OrderApprovalService.Decision.APPROVE : null;
        String comment = request.getParameter("comment") == null ? null : request.getParameter("comment").trim();
        comment = comment == null || comment.isEmpty() ? null : comment;
        try {
            OrderDetail order = id == null ? null : lifecycle.findVisible(userId, id);
            if (order == null || decision == null || !lifecycle.canRepConfirm(userId, order)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String detailUrl = request.getContextPath() + "/orders/view?id=" + order.getId();
            String error = OrderApprovalService.validateComment(decision, comment);
            if (error != null) {
                request.getSession().setAttribute(OrderViewServlet.FLASH_ERROR, error);
                response.sendRedirect(detailUrl);
                return;
            }
            try {
                boolean done = approvalService.decide(order, order.getVersion(), decision, comment, userId,
                        request.getRemoteAddr());
                if (done) {
                    request.getSession().setAttribute(OrderViewServlet.FLASH_MESSAGE, decision
                            == OrderApprovalService.Decision.APPROVE
                            ? "Đã xác nhận đơn " + order.getOrderNo() + " của đại lý."
                            : "Đã từ chối đơn " + order.getOrderNo() + "; tồn đang giữ chỗ đã được nhả.");
                } else {
                    request.getSession().setAttribute(OrderViewServlet.FLASH_ERROR, "Đơn " + order.getOrderNo()
                            + " vừa được xử lý ở nơi khác. Vui lòng xem lại trạng thái mới.");
                }
            } catch (StockReservationService.InsufficientStockException e) {
                request.getSession().setAttribute(OrderViewServlet.FLASH_ERROR, e.getMessage());
            }
            response.sendRedirect(detailUrl);
        } catch (SQLException e) {
            log("Không xác nhận được đơn của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
