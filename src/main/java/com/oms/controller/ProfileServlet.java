package com.oms.controller;

import com.oms.model.AccountDetail;
import com.oms.model.ProfileForm;
import com.oms.model.SessionUser;
import com.oms.security.SessionRegistry;
import com.oms.service.AuthService;
import com.oms.service.ProfileService;
import com.oms.util.PhoneUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

// Hồ sơ cá nhân (S2-02): luôn là hồ sơ của người đang đăng nhập, không nhận id từ request
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/profile/profile.jsp";
    private static final String FLASH_UPDATED = "flashProfileUpdated";

    private final ProfileService profileService = new ProfileService();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            AccountDetail profile = profileService.getProfile(CurrentUser.get(request).getId());
            if (profile == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, FLASH_UPDATED, "success");
            showForm(request, response, profile, new ProfileForm(profile.getFullName(), profile.getPhone()), Map.of());
        } catch (SQLException e) {
            log("Không tải được hồ sơ cá nhân", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        // Chỉ đọc hai tham số này; các tham số khác (username, roleCodes, warehouseId...) bị bỏ qua
        ProfileForm form = new ProfileForm(normalize(request.getParameter("fullName")),
                PhoneUtil.normalize(normalize(request.getParameter("phone"))));
        try {
            Map<String, String> errors = profileService.validate(form);
            if (!errors.isEmpty()) {
                AccountDetail profile = profileService.getProfile(userId);
                if (profile == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                showForm(request, response, profile, form, errors);
                return;
            }

            profileService.update(userId, form, request.getRemoteAddr());
            // Họ tên mới hiện ngay ở sidebar trên mọi phiên đang đăng nhập của người này
            SessionUser refreshed = authService.loadSessionUser(userId);
            if (refreshed != null) {
                SessionRegistry.replaceUser(refreshed);
            }
            request.getSession().setAttribute(FLASH_UPDATED, "Đã cập nhật hồ sơ cá nhân.");
            response.sendRedirect(request.getContextPath() + "/profile");
        } catch (SQLException e) {
            log("Không cập nhật được hồ sơ cá nhân", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static void showForm(HttpServletRequest request, HttpServletResponse response, AccountDetail profile,
                                 ProfileForm form, Map<String, String> errors) throws ServletException, IOException {
        request.setAttribute("profile", profile);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
