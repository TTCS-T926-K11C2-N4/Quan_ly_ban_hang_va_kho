package com.oms.filter;

import com.oms.dao.NotificationDao;
import com.oms.model.SessionUser;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

// Số thông báo chưa đọc cho chấm đỏ trên chuông ở thanh trên (S7-08). Chỉ đếm khi mở trang (GET của Servlet ứng dụng)
// cho người đã đăng nhập; khai báo trong web.xml sau AuthFilter.
public class NotificationBadgeFilter implements Filter {

    public static final String COUNT_ATTRIBUTE = "unreadNotificationCount";
    private static final Set<String> CONTAINER_SERVLETS = Set.of("default", "jsp");

    private final NotificationDao notificationDao = new NotificationDao();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (user != null && "GET".equals(request.getMethod())
                && !CONTAINER_SERVLETS.contains(request.getHttpServletMapping().getServletName())) {
            try {
                request.setAttribute(COUNT_ATTRIBUTE, notificationDao.countUnread(user.getId()));
            } catch (SQLException e) {
                // Không đếm được thì chuông chỉ thiếu chấm đỏ, trang vẫn mở bình thường
                request.getServletContext().log("Không đếm được thông báo chưa đọc", e);
            }
        }
        chain.doFilter(request, response);
    }
}
