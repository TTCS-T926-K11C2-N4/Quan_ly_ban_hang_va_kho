package com.oms.controller;

import com.oms.model.AccountForm;
import com.oms.model.EditableAccount;
import com.oms.service.AccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

@WebServlet("/accounts/edit")
public class AccountEditServlet extends HttpServlet {

    // AccountListServlet đọc rồi xóa attribute này để hiện thông báo một lần
    public static final String FLASH_MESSAGE = "flashMessage";

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            EditableAccount account = findAccount(request);
            if (account == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showForm(request, response, account, account.getForm(), Map.of());
        } catch (SQLException e) {
            log("Không tải được tài khoản để sửa", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            EditableAccount account = findAccount(request);
            if (account == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            AccountForm submitted = AccountFormParser.read(request);
            // Tên đăng nhập không sửa được: luôn dùng giá trị trong DB, bỏ qua giá trị gửi lên
            AccountForm form = new AccountForm(submitted.getFullName(), account.getUsername(), submitted.getEmail(),
                    submitted.getPhone(), submitted.getRoleCodes(), submitted.getWarehouseId(), submitted.getRegionId());
            boolean activate = "activate".equals(request.getParameter("action")) && account.isPending();

            Map<String, String> errors = accountService.validateAccountUpdate(account, form);
            if (errors.isEmpty()) {
                try {
                    accountService.update(account, form, activate, request.getRemoteAddr());
                    String message = activate
                            ? "Đã cập nhật và kích hoạt tài khoản " + account.getUsername() + "."
                            : "Đã cập nhật tài khoản " + account.getUsername() + ".";
                    request.getSession().setAttribute(FLASH_MESSAGE, message);
                    response.sendRedirect(request.getContextPath() + "/accounts");
                    return;
                } catch (SQLIntegrityConstraintViolationException e) {
                    // Người khác vừa đổi sang cùng email giữa lúc kiểm tra và lúc lưu
                    errors = accountService.validateAccountUpdate(account, form);
                }
            }
            showForm(request, response, account, form, errors);
        } catch (SQLException e) {
            log("Không cập nhật được tài khoản", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private EditableAccount findAccount(HttpServletRequest request) throws SQLException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        return id == null ? null : accountService.findForEdit(id);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, EditableAccount account,
                          AccountForm form, Map<String, String> errors)
            throws ServletException, IOException, SQLException {
        request.setAttribute("editing", true);
        request.setAttribute("account", account);
        request.setAttribute("roles", accountService.getAssignableRoles());
        request.setAttribute("warehouses", accountService.getWarehouses());
        request.setAttribute("regions", accountService.getRegions());
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(AccountCreateServlet.VIEW).forward(request, response);
    }
}
