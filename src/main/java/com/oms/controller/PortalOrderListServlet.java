package com.oms.controller;

import com.oms.model.SessionUser;
import com.oms.service.PortalOrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-01: đơn hàng của chính đại lý kèm trạng thái, ngày giao dự kiến, tổng tiền
@WebServlet("/portal/orders")
public class PortalOrderListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/portal/portal-orders.jsp";

    private final PortalOrderService portalOrderService = new PortalOrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        try {
            request.setAttribute("orders", portalOrderService.findOrders(user.getId()));
        } catch (SQLException e) {
            log("Không tải được đơn hàng của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
