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
    private static final String FLASH_CHANGED = "flashPasswordChanged";

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Flash.moveToRequest(request, FLASH_CHANGED, "success");
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

            // S1-04: đăng xuất mọi thiết bị khác; phiên hiện tại được giữ nhưng đổi mã phiên
            HttpSession session = request.getSession();
            SessionRegistry.invalidateAll(user.getId(), session);
            request.changeSessionId();
            session.setAttribute(SessionUser.SESSION_KEY, authService.loadSessionUser(user.getId()));
            session.setAttribute(FLASH_CHANGED, "Đổi mật khẩu thành công. Các phiên đăng nhập khác đã bị đăng xuất.");
            response.sendRedirect(request.getContextPath() + "/change-password");
        } catch (SQLException e) {
            log("Không đổi được mật khẩu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
