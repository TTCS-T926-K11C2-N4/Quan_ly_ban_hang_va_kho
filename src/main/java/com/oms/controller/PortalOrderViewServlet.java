package com.oms.controller;

import com.oms.model.PortalOrder;
import com.oms.model.SessionUser;
import com.oms.service.PortalOrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-01: chi tiết một đơn của đại lý, kèm số lượng thực giao khi giao thiếu. Đơn của đại lý khác trả 404.
@WebServlet("/portal/orders/view")
public class PortalOrderViewServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/portal/portal-order-view.jsp";

    private final PortalOrderService portalOrderService = new PortalOrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            PortalOrder order = id == null ? null : portalOrderService.findOrder(user.getId(), id);
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("order", order);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được chi tiết đơn của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
