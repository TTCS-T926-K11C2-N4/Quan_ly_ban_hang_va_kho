package com.oms.controller;

import com.oms.model.OrderDetail;
import com.oms.model.OrderStatus;
import com.oms.service.OrderApprovalService;
import com.oms.service.StockReservationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S4-05 AC2: Duyệt / Từ chối / Trả lại sửa một đơn đang chờ duyệt; Từ chối và Trả lại sửa bắt buộc ý kiến.
// Xong thì về danh sách chờ duyệt kèm thông báo; lỗi ý kiến thì mở lại hộp thoại trên màn duyệt.
@WebServlet("/approvals/decide")
public class ApprovalDecideServlet extends HttpServlet {

    private final OrderApprovalService approvalService = new OrderApprovalService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        OrderApprovalService.Decision decision = OrderApprovalService.Decision.of(request.getParameter("decision"));
        if (id == null || decision == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String comment = request.getParameter("comment") == null ? null : request.getParameter("comment").trim();
        comment = comment == null || comment.isEmpty() ? null : comment;
        try {
            OrderDetail order = approvalService.findOrder(id);
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String viewUrl = request.getContextPath() + "/approvals/view?id=" + order.getId();
            if (!OrderStatus.PENDING_APPROVAL.equals(order.getStatus())) {
                request.getSession().setAttribute(ApprovalListServlet.FLASH_ERROR, "Đơn " + order.getOrderNo()
                        + " đang ở trạng thái \"" + order.getStatusLabel() + "\", không còn chờ duyệt.");
                response.sendRedirect(viewUrl);
                return;
            }
            String error = OrderApprovalService.validateComment(decision, comment);
            if (error != null) {
                ApprovalViewServlet.show(request, response, order, request.getParameter("decision"), comment, error);
                return;
            }
            boolean done;
            try {
                done = approvalService.decide(order, parseVersion(request.getParameter("version")), decision, comment,
                        CurrentUser.get(request).getId(), request.getRemoteAddr());
            } catch (StockReservationService.InsufficientStockException e) {
                request.getSession().setAttribute(ApprovalListServlet.FLASH_ERROR, e.getMessage()
                        + " Đơn vẫn chờ duyệt; có thể trả lại để nhân viên sửa số lượng.");
                response.sendRedirect(viewUrl);
                return;
            }
            if (!done) {
                request.getSession().setAttribute(ApprovalListServlet.FLASH_ERROR, "Đơn " + order.getOrderNo()
                        + " vừa được người khác xử lý hoặc cập nhật. Vui lòng xem lại trạng thái mới.");
                response.sendRedirect(viewUrl);
                return;
            }
            request.getSession().setAttribute(ApprovalListServlet.FLASH_MESSAGE, message(decision, order));
            response.sendRedirect(request.getContextPath() + "/approvals");
        } catch (SQLException e) {
            log("Không ghi được quyết định duyệt đơn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static String message(OrderApprovalService.Decision decision, OrderDetail order) {
        return switch (decision) {
            case APPROVE -> "Đã duyệt đơn " + order.getOrderNo() + ". Tồn được giữ chỗ, đơn chờ kho soạn hàng.";
            case REJECT -> "Đã từ chối đơn " + order.getOrderNo() + ". Tồn đang giữ chỗ cho đơn đã được nhả.";
            case RETURN -> "Đã trả lại đơn " + order.getOrderNo() + " cho nhân viên sửa. Đơn về trạng thái Nháp.";
        };
    }

    // Version không hợp lệ thì coi như đã cũ (-1), để quyết định bị từ chối thay vì ghi đè
    private static long parseVersion(String value) {
        try {
            return value == null ? -1 : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
