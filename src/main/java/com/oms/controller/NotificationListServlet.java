package com.oms.controller;

import com.oms.dao.NotificationDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-08: thông báo trong ứng dụng của người đăng nhập; POST đánh dấu đã đọc tất cả
@WebServlet("/notifications")
public class NotificationListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/notifications/notification-list.jsp";
    private static final int LIMIT = 100;

    private final NotificationDao notificationDao = new NotificationDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("notifications", notificationDao.findByUser(CurrentUser.get(request).getId(), LIMIT));
        } catch (SQLException e) {
            log("Không tải được thông báo", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            notificationDao.markAllRead(CurrentUser.get(request).getId());
        } catch (SQLException e) {
            log("Không đánh dấu đã đọc được thông báo", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/notifications");
    }
}
