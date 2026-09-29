package com.oms.controller;

import com.oms.model.DashboardSummary;
import com.oms.model.RecentOrder;
import com.oms.model.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/dashboard/dashboard.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Đại lý không được xem số liệu toàn công ty: dùng trang chủ riêng (S1-01)
        SessionUser user = CurrentUser.get(request);
        if (user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        // Số liệu mẫu theo Figma S1-06 để dựng giao diện; khi có dữ liệu bán hàng thay bằng DashboardService
        // (S8-05). Người đăng nhập (currentUser) JSP đọc thẳng từ session.
        request.setAttribute("unreadNotificationCount", 1);
        request.setAttribute("summary", new DashboardSummary(128_450_000L, 12.5, 248, 8.2, 16));
        request.setAttribute("recentOrders", List.of(
                new RecentOrder("#DH-1028", "Nguyễn Minh Anh", "Đã xác nhận", 4_280_000L),
                new RecentOrder("#DH-1027", "Trần Quốc Huy", "Đang xử lý", 2_650_000L),
                new RecentOrder("#DH-1026", "Lê Thu Hà", "Hoàn tất", 8_120_000L),
                new RecentOrder("#DH-1025", "Phạm Đức Long", "Chờ xác nhận", 1_890_000L)));

        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
