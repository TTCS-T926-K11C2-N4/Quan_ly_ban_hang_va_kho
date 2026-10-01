package com.oms.controller;

import com.oms.model.SessionUser;
import com.oms.security.SessionRegistry;
import com.oms.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/change-password")
public class ChangePasswordServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/auth/change-password.jsp";

    // Trang Đăng nhập lấy ra để hiện thông báo một lần sau khi đổi mật khẩu
    public static final String FLASH_CHANGED = "flashPasswordChanged";

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        try {
            String error = authService.changePassword(user.getId(), request.getParameter("currentPassword"),
                    request.getParameter("newPassword"), request.getParameter("confirmPassword"),
                    request.getRemoteAddr());
            if (error != null) {
                request.setAttribute("error", error);
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }

            // Đổi xong đăng xuất mọi phiên, kể cả phiên hiện tại, để người dùng đăng nhập lại bằng mật khẩu mới
            // (S1-04 thu hồi phiên ở thiết bị khác)
            SessionRegistry.invalidateAll(user.getId(), null);
            HttpSession current = request.getSession(false);
            if (current != null) {
                try {
                    current.invalidate();
                } catch (IllegalStateException e) {
                    // invalidateAll đã hủy phiên này
                }
            }
            request.getSession().setAttribute(FLASH_CHANGED,
                    "Đổi mật khẩu thành công. Vui lòng đăng nhập lại bằng mật khẩu mới.");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (SQLException e) {
            log("Không đổi được mật khẩu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
