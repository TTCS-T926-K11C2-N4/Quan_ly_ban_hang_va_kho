package com.oms.filter;

import com.oms.model.Permission;
import com.oms.model.SessionUser;
import com.oms.service.OrderApprovalService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

// Số đơn đang chờ duyệt cho mục "Duyệt đơn" trên menu (S4-05). Chỉ đếm khi mở trang (GET của Servlet ứng dụng)
// cho người được xem hàng chờ duyệt; khai báo trong web.xml sau AuthFilter nên người dùng đã đăng nhập.
public class ApprovalBadgeFilter implements Filter {

    public static final String COUNT_ATTRIBUTE = "pendingApprovalCount";
    private static final Set<String> CONTAINER_SERVLETS = Set.of("default", "jsp");

    private final OrderApprovalService approvalService = new OrderApprovalService();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (user != null && user.can(Permission.ORDER_APPROVAL_VIEW) && "GET".equals(request.getMethod())
                && !CONTAINER_SERVLETS.contains(request.getHttpServletMapping().getServletName())) {
            try {
                request.setAttribute(COUNT_ATTRIBUTE, approvalService.countPending());
            } catch (SQLException e) {
                // Không đếm được thì menu chỉ thiếu số đếm, trang vẫn mở bình thường
                ServletContext context = request.getServletContext();
                context.log("Không đếm được đơn chờ duyệt", e);
            }
        }
        chain.doFilter(request, response);
    }
}
