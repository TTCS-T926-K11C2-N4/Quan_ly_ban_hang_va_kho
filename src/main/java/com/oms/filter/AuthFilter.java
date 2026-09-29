package com.oms.filter;

import com.oms.model.SessionUser;
import com.oms.security.AccessRules;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

// Kiểm đăng nhập và quyền cho mọi request (S1-02, S1-05). Khai báo trong web.xml để chạy sau EncodingFilter.
public class AuthFilter implements Filter {

    // LoginServlet đưa người dùng về trang này sau khi đăng nhập
    public static final String RETURN_TO = "returnTo";

    // Servlet sẵn có của Tomcat: tài nguyên tĩnh và JSP. Đường dẫn không khớp Servlet nào của ứng dụng
    // cũng rơi vào đây, nên để Tomcat trả 404 thay vì báo thiếu quyền.
    private static final Set<String> CONTAINER_SERVLETS = Set.of("default", "jsp");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        String contextPath = request.getContextPath();

        // Địa chỉ gốc: LoginServlet đưa người đã đăng nhập về trang chủ theo vai trò
        if (path.isEmpty() || "/".equals(path)) {
            response.sendRedirect(contextPath + "/login");
            return;
        }
        if (AccessRules.isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (user == null) {
            // Trình duyệt gửi mã phiên mà server không còn giữ: phiên đã hết hạn hoặc đã bị thu hồi
            if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
                response.sendRedirect(contextPath + "/session-expired");
                return;
            }
            if ("GET".equals(request.getMethod())) {
                String query = request.getQueryString();
                request.getSession().setAttribute(RETURN_TO, path + (query == null ? "" : "?" + query));
            }
            response.sendRedirect(contextPath + "/login");
            return;
        }

        String servletName = request.getHttpServletMapping().getServletName();
        if (!CONTAINER_SERVLETS.contains(servletName) && !AccessRules.isAllowed(path, user.getPermissions())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
