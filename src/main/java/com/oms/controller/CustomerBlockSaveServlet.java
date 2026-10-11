package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.Permission;
import com.oms.service.CustomerBlockService;
import com.oms.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S3-07: xác nhận khoá (action=BLOCK) hoặc mở giao dịch (action=UNBLOCK); AccessRules chỉ cho CREDIT_MANAGE
@WebServlet("/customers/block/save")
public class CustomerBlockSaveServlet extends HttpServlet {

    private final CustomerService customerService = new CustomerService();
    private final CustomerBlockService blockService = new CustomerBlockService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        String action = request.getParameter("action");
        if (!"BLOCK".equals(action) && !"UNBLOCK".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        boolean blocking = "BLOCK".equals(action);
        String reason = request.getParameter("reason");
        reason = reason == null || reason.isBlank() ? null : reason.trim();
        long userId = CurrentUser.get(request).getId();
        try {
            Customer customer = id == null ? null : customerService.findVisible(userId, Permission.CREDIT_MANAGE, id);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String reasonError = blockService.validateReason(reason, blocking);
            if (reasonError != null) {
                CustomerBlockServlet.show(request, response, customer, reason, reasonError, null);
                return;
            }
            if (!blockService.changeBlocked(customer, blocking, reason, userId, request.getRemoteAddr())) {
                // Người khác vừa khoá/mở: hiện trạng thái mới nhất, giữ lý do đã gõ
                customer = customerService.findVisible(userId, Permission.CREDIT_MANAGE, id);
                if (customer == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                CustomerBlockServlet.show(request, response, customer, reason, null, "Trạng thái giao dịch của đại lý"
                        + " vừa được người khác thay đổi. Vui lòng xem lại trước khi xác nhận.");
                return;
            }
            request.getSession().setAttribute(CustomerBlockServlet.FLASH_MESSAGE, (blocking
                    ? "Đã khoá giao dịch với \"" : "Đã mở lại giao dịch với \"") + customer.getName() + "\".");
            response.sendRedirect(request.getContextPath() + "/customers/block?id=" + customer.getId());
        } catch (SQLException e) {
            log("Không khoá/mở được giao dịch đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
