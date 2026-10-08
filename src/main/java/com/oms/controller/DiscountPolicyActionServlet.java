package com.oms.controller;

import com.oms.model.DiscountPolicyRow;
import com.oms.service.DiscountPolicyService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S3-01: xoá chính sách (đã có đơn dùng thì chỉ ngừng áp dụng), ngừng hoặc áp dụng lại
@WebServlet("/discount-policies/action")
public class DiscountPolicyActionServlet extends HttpServlet {

    private final DiscountPolicyService policyService = new DiscountPolicyService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (!"remove".equals(action) && !"activate".equals(action) && !"deactivate".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        long userId = CurrentUser.get(request).getId();
        String ip = request.getRemoteAddr();
        try {
            DiscountPolicyRow policy = id == null ? null : policyService.find(id);
            if (policy == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String message;
            if ("remove".equals(action)) {
                message = policyService.remove(policy, userId, ip) == DiscountPolicyService.RemoveResult.DELETED
                        ? "Đã xoá chính sách " + policy.getCode() + "."
                        : "Chính sách " + policy.getCode() + " đã có đơn hàng dùng nên không xoá được, đã chuyển sang ngừng áp dụng.";
            } else {
                boolean active = "activate".equals(action);
                policyService.setActive(policy, active, userId, ip);
                message = (active ? "Đã áp dụng lại chính sách " : "Đã ngừng áp dụng chính sách ") + policy.getCode() + ".";
            }
            request.getSession().setAttribute(DiscountPolicyServlet.FLASH_MESSAGE, message);
            String query = QueryString.of(request, DiscountPolicyServlet.LIST_PARAMS);
            response.sendRedirect(request.getContextPath() + "/discount-policies" + (query.isEmpty() ? "" : "?" + query));
        } catch (SQLException e) {
            log("Không cập nhật được chính sách chiết khấu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
