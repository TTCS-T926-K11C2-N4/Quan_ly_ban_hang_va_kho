package com.oms.controller;

import com.oms.model.OrderDetail;
import com.oms.service.OrderLifecycleService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S4-06: huỷ đơn hàng (bắt buộc lý do, tự nhả tồn đang giữ chỗ). Đơn đã xuất kho không huỷ được.
@WebServlet("/orders/cancel")
public class OrderCancelServlet extends HttpServlet {

    private final OrderLifecycleService lifecycle = new OrderLifecycleService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        String reason = request.getParameter("reason") == null ? null : request.getParameter("reason").trim();
        reason = reason == null || reason.isEmpty() ? null : reason;
        try {
            OrderDetail order = id == null ? null : lifecycle.findVisible(userId, id);
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String detailUrl = request.getContextPath() + "/orders/view?id=" + order.getId();
            if (!lifecycle.canCancel(userId, order)) {
                request.getSession().setAttribute(OrderViewServlet.FLASH_ERROR, order.isShipped()
                        ? "Đơn " + order.getOrderNo() + " đã xuất kho nên không huỷ được. Vui lòng lập phiếu trả hàng."
                        : "Đơn " + order.getOrderNo() + " đang ở trạng thái \"" + order.getStatusLabel()
                        + "\" nên không huỷ được.");
                response.sendRedirect(detailUrl);
                return;
            }
            String error = lifecycle.validateReason(reason);
            if (error != null) {
                OrderViewServlet.show(request, response, order, error, reason);
                return;
            }
            long version = parseVersion(request.getParameter("version"));
            if (!lifecycle.cancel(order, version, reason, userId, request.getRemoteAddr())) {
                request.getSession().setAttribute(OrderViewServlet.FLASH_ERROR, "Đơn " + order.getOrderNo()
                        + " vừa được cập nhật ở nơi khác nên chưa huỷ. Vui lòng xem lại trạng thái mới rồi thử lại.");
                response.sendRedirect(detailUrl);
                return;
            }
            request.getSession().setAttribute(OrderViewServlet.FLASH_MESSAGE, "Đã huỷ đơn " + order.getOrderNo()
                    + ". Tồn đang giữ chỗ của đơn (nếu có) đã được nhả.");
            response.sendRedirect(detailUrl);
        } catch (SQLException e) {
            log("Không huỷ được đơn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Version không hợp lệ thì coi như đã cũ (-1), để lần huỷ bị từ chối thay vì ghi đè
    private static long parseVersion(String value) {
        try {
            return value == null ? -1 : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
