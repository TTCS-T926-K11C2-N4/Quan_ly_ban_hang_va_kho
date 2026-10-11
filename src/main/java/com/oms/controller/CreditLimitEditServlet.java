package com.oms.controller;

import com.oms.model.CreditLimitForm;
import com.oms.model.Customer;
import com.oms.model.Permission;
import com.oms.service.CreditLimitService;
import com.oms.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

// S3-05: lưu form "Sửa hạn mức công nợ" (AccessRules: chỉ người có CREDIT_MANAGE)
@WebServlet("/customers/credit-limit/edit")
public class CreditLimitEditServlet extends HttpServlet {

    private final CustomerService customerService = new CustomerService();
    private final CreditLimitService creditLimitService = new CreditLimitService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        long userId = CurrentUser.get(request).getId();
        try {
            Customer customer = id == null ? null : customerService.findVisible(userId, Permission.CREDIT_MANAGE, id);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            CreditLimitForm form = new CreditLimitForm(normalize(request.getParameter("creditLimit")),
                    normalize(request.getParameter("maxDebtDays")), normalize(request.getParameter("reason")),
                    request.getParameter("version"));
            Map<String, String> errors = creditLimitService.validate(form, customer);
            if (errors.isEmpty() && !creditLimitService.update(customer, form, userId, request.getRemoteAddr())) {
                // Hiện số mới nhất để người dùng xem lại; giữ số đã gõ và lý do, version mới để lưu lại được
                customer = customerService.findVisible(userId, Permission.CREDIT_MANAGE, id);
                if (customer == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                form = new CreditLimitForm(form.getCreditLimit(), form.getMaxDebtDays(), form.getReason(),
                        String.valueOf(customer.getVersion()));
                errors = Map.of("form", "Hạn mức vừa được người khác cập nhật (số hiện tại ở trên)."
                        + " Kiểm tra lại rồi bấm Lưu thay đổi nếu vẫn muốn sửa.");
            }
            if (!errors.isEmpty()) {
                CreditLimitServlet.show(request, response, customer, form, errors);
                return;
            }
            request.getSession().setAttribute(CreditLimitServlet.FLASH_MESSAGE,
                    "Đã lưu hạn mức công nợ của \"" + customer.getName() + "\".");
            response.sendRedirect(request.getContextPath() + "/customers/credit-limit?id=" + customer.getId());
        } catch (SQLException e) {
            log("Không lưu được hạn mức công nợ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
