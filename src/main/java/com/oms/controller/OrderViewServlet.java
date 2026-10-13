package com.oms.controller;

import com.oms.model.OrderDetail;
import com.oms.model.OrderStatus;
import com.oms.model.SessionUser;
import com.oms.service.OrderLifecycleService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S4-06: Chi tiết đơn hàng & vòng đời: thông tin đơn, đại lý, điểm giao, thanh tiến trình, dòng hàng,
// lịch sử đổi trạng thái; nút Huỷ đơn khi được phép (POST /orders/cancel).
@WebServlet("/orders/view")
public class OrderViewServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/orders/order-detail.jsp";
    static final String FLASH_MESSAGE = "orderDetailFlashMessage";
    static final String FLASH_ERROR = "orderDetailFlashError";

    private static final OrderLifecycleService LIFECYCLE = new OrderLifecycleService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Tài khoản đại lý xem đơn của mình ở cổng đại lý (S5-01), không dùng màn nội bộ này
        SessionUser user = CurrentUser.get(request);
        if (user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            OrderDetail order = id == null ? null : LIFECYCLE.findVisible(user.getId(), id);
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, FLASH_ERROR, "flashError");
            show(request, response, order, null, null);
        } catch (SQLException e) {
            log("Không tải được chi tiết đơn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // cancelError / cancelReason: lỗi và lý do vừa gõ khi huỷ không hợp lệ, để mở lại hộp huỷ đơn
    static void show(HttpServletRequest request, HttpServletResponse response, OrderDetail order, String cancelError,
                     String cancelReason) throws ServletException, IOException, SQLException {
        request.setAttribute("order", order);
        request.setAttribute("canCancel", LIFECYCLE.canCancel(CurrentUser.get(request).getId(), order));
        request.setAttribute("cancelError", cancelError);
        request.setAttribute("cancelReason", cancelReason);
        request.setAttribute("statusLegend", OrderStatus.legend());
        request.setAttribute("reasonMaxLength", OrderLifecycleService.REASON_MAX_LENGTH);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
