package com.oms.controller;

import com.oms.model.CustomerProfile;
import com.oms.model.SessionUser;
import com.oms.service.CustomerProfileService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S3-03: Ngừng giao dịch, Giao dịch lại, Xoá đại lý (đã phát sinh giao dịch thì chỉ ngừng giao dịch)
@WebServlet("/customers/profile/action")
public class CustomerProfileActionServlet extends HttpServlet {

    private final CustomerProfileService profileService = new CustomerProfileService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (!"deactivate".equals(action) && !"activate".equals(action) && !"remove".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        SessionUser user = CurrentUser.get(request);
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        String ip = request.getRemoteAddr();
        try {
            CustomerProfile profile = id == null || !CustomerProfileServlet.canManage(user, id) ? null
                    : profileService.find(id);
            if (profile == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String message;
            if ("remove".equals(action)) {
                if (profileService.remove(profile, user.getId(), ip) == CustomerProfileService.RemoveResult.DELETED) {
                    request.getSession().setAttribute(CustomerProfileServlet.FLASH_DELETED,
                            "Đã xoá đại lý " + profile.getCode() + " - " + profile.getName() + ".");
                    response.sendRedirect(request.getContextPath() + "/customers");
                    return;
                }
                message = "Đại lý đã phát sinh giao dịch nên không xoá được, đã chuyển sang ngừng giao dịch.";
            } else {
                boolean active = "activate".equals(action);
                profileService.setActive(profile, active, user.getId(), ip);
                message = active ? "Đại lý đã giao dịch lại." : "Đã ngừng giao dịch với đại lý. Hồ sơ vẫn được giữ lại.";
            }
            request.getSession().setAttribute(CustomerProfileServlet.FLASH_MESSAGE, message);
            response.sendRedirect(request.getContextPath() + "/customers/profile?id=" + profile.getId());
        } catch (SQLException e) {
            log("Không cập nhật được trạng thái đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
