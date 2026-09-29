package com.oms.controller;

import com.oms.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/auth/forgot-password.jsp";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final int EMAIL_MAX_LENGTH = 150;

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        email = email == null ? "" : email.trim();
        String error = email.isEmpty() ? "Vui lòng nhập email."
                : email.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()
                ? "Email không đúng định dạng." : null;
        if (error != null) {
            request.setAttribute("email", email);
            request.setAttribute("error", error);
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        }

        try {
            authService.requestPasswordReset(email, resetPageUrl(request));
        } catch (SQLException e) {
            log("Không tạo được liên kết đặt lại mật khẩu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        // Email có tồn tại hay không đều sang cùng một trang (S1-03)
        response.sendRedirect(request.getContextPath() + "/forgot-password/sent");
    }

    // Ưu tiên APP_BASE_URL (vd http://localhost:8080/du-an-ttcs) vì header Host do trình duyệt gửi lên
    // có thể bị giả mạo để liên kết trong email trỏ sang trang của kẻ tấn công.
    private static String resetPageUrl(HttpServletRequest request) {
        String baseUrl = System.getenv("APP_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            String url = request.getRequestURL().toString();
            baseUrl = url.substring(0, url.length() - request.getServletPath().length());
        }
        return baseUrl.replaceAll("/+$", "") + "/reset-password";
    }
}
