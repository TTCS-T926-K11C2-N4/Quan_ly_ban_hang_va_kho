package com.oms.controller;

import com.oms.security.SessionRegistry;
import com.oms.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// Trang mở từ liên kết trong email quên mật khẩu (S1-03): /reset-password?token=...
@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    public static final String FLASH_PASSWORD_RESET = "flashPasswordReset";

    private static final String VIEW = "/WEB-INF/views/auth/reset-password.jsp";

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        try {
            showPage(request, response, token, authService.isResetTokenValid(token), null);
        } catch (SQLException e) {
            log("Không kiểm tra được liên kết đặt lại mật khẩu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        try {
            String error = authService.validateNewPassword(request.getParameter("newPassword"),
                    request.getParameter("confirmPassword"));
            if (error != null) {
                showPage(request, response, token, authService.isResetTokenValid(token), error);
                return;
            }
            Long userId = authService.resetPassword(token, request.getParameter("newPassword"),
                    request.getRemoteAddr());
            if (userId == null) {
                showPage(request, response, token, false, null);
                return;
            }
            // Ai đó đang đăng nhập bằng mật khẩu cũ cũng bị đăng xuất
            SessionRegistry.invalidateAll(userId, null);
            request.getSession().setAttribute(FLASH_PASSWORD_RESET,
                    "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (SQLException e) {
            log("Không đặt lại được mật khẩu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static void showPage(HttpServletRequest request, HttpServletResponse response, String token,
                                 boolean tokenValid, String error) throws ServletException, IOException {
        request.setAttribute("token", token);
        request.setAttribute("tokenValid", tokenValid);
        request.setAttribute("error", error);
        // Không lưu trang có token trong cache của trình duyệt
        response.setHeader("Cache-Control", "no-store");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
