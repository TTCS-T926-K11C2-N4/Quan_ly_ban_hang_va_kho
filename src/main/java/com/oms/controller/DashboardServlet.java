package com.oms.controller;

import com.oms.model.SessionUser;
import com.oms.service.DashboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/dashboard/dashboard.jsp";

    private final DashboardService dashboardService = new DashboardService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Đại lý không được xem số liệu toàn công ty: dùng trang chủ riêng (S1-01)
        SessionUser user = CurrentUser.get(request);
        if (user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        // Chỉ hiện số thật; phần chưa có dữ liệu (doanh thu, đơn hàng) JSP ghi rõ thay vì số mẫu.
        // Người đăng nhập (currentUser) JSP đọc thẳng từ session.
        try {
            request.setAttribute("summary", dashboardService.getSummary());
        } catch (SQLException e) {
            log("Không tải được số liệu tổng quan", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("recentOrders", dashboardService.getRecentOrders());

        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
