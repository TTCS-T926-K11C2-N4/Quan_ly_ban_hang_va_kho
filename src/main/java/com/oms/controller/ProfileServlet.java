package com.oms.controller;

import com.oms.model.AccountDetail;
import com.oms.model.SessionUser;
import com.oms.security.SessionRegistry;
import com.oms.service.AuthService;
import com.oms.service.ProfileService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

// S2-02 Hồ sơ cá nhân: mọi người đã đăng nhập xem và sửa họ tên, số điện thoại của chính mình.
// Chỉ đọc fullName và phone từ form; tài khoản, vai trò, kho, địa bàn gửi kèm cũng bị bỏ qua.
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/profile/profile.jsp";
    static final String FLASH_MESSAGE = "profileFlashMessage";

    private final ProfileService profileService = new ProfileService();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            AccountDetail profile = profileService.getProfile(CurrentUser.get(request).getId());
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            show(request, response, profile, profile.getFullName(), profile.getPhone(), Map.of());
        } catch (SQLException e) {
            log("Không tải được hồ sơ cá nhân", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        try {
            Map<String, String> errors = profileService.validate(fullName, phone);
            if (!errors.isEmpty()) {
                show(request, response, profileService.getProfile(user.getId()), fullName, phone, errors);
                return;
            }
            profileService.update(user.getId(), fullName, phone, request.getRemoteAddr());
            // Tên mới hiện ngay ở sidebar của mọi phiên đang đăng nhập
            SessionRegistry.replaceUser(authService.loadSessionUser(user.getId()));
            request.getSession().setAttribute(FLASH_MESSAGE, "Đã cập nhật hồ sơ cá nhân.");
            response.sendRedirect(request.getContextPath() + "/profile");
        } catch (SQLException e) {
            log("Không cập nhật được hồ sơ cá nhân", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static void show(HttpServletRequest request, HttpServletResponse response, AccountDetail profile,
                             String fullName, String phone, Map<String, String> errors)
            throws ServletException, IOException {
        request.setAttribute("profile", profile);
        request.setAttribute("fullName", fullName);
        request.setAttribute("phone", phone);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
