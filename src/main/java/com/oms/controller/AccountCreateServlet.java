package com.oms.controller;

import com.oms.model.AccountForm;
import com.oms.service.AccountService;
import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

@WebServlet("/accounts/new")
public class AccountCreateServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/accounts/account-form.jsp";

    // Tên attribute session dùng để hiện thông báo một lần ở trang danh sách (Post/Redirect/Get)
    public static final String FLASH_CREATED_USERNAME = "flashCreatedUsername";
    public static final String FLASH_CREATED_EMAIL = "flashCreatedEmail";

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showForm(request, response, null, Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AccountForm form = AccountFormParser.read(request);
        try {
            Map<String, String> errors = accountService.validateNewAccount(form);
            if (!errors.isEmpty()) {
                showForm(request, response, form, errors);
                return;
            }

            try {
                accountService.create(form, CurrentUser.get(request).getId(), request.getRemoteAddr(),
                        AppUrl.of(request, "/login"));
            } catch (SQLIntegrityConstraintViolationException e) {
                // Người khác vừa tạo trùng tên đăng nhập/email giữa lúc kiểm tra và lúc lưu
                showForm(request, response, form, accountService.validateNewAccount(form));
                return;
            } catch (MessagingException e) {
                log("Không gửi được mật khẩu tạm tới " + form.getEmail(), e);
                showForm(request, response, form, Map.of("email",
                        "Không gửi được email tới địa chỉ này nên tài khoản chưa được tạo. Kiểm tra lại email rồi thử lại."));
                return;
            }

            HttpSession session = request.getSession();
            session.setAttribute(FLASH_CREATED_USERNAME, form.getUsername());
            session.setAttribute(FLASH_CREATED_EMAIL, form.getEmail());
            response.sendRedirect(request.getContextPath() + "/accounts");
        } catch (SQLException e) {
            log("Không tạo được tài khoản", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          AccountForm form, Map<String, String> errors) throws ServletException, IOException {
        try {
            request.setAttribute("roles", accountService.getAssignableRoles());
            request.setAttribute("warehouses", accountService.getWarehouses());
            request.setAttribute("regions", accountService.getRegions());
        } catch (SQLException e) {
            log("Không tải được dữ liệu cho form tạo tài khoản", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
