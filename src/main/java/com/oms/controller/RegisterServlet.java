package com.oms.controller;

import com.oms.model.RegistrationForm;
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

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/auth/register.jsp";

    // LoginServlet đọc rồi xóa attribute này để hiện thông báo một lần
    public static final String FLASH_REGISTERED = "flashRegistered";

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RegistrationForm form = new RegistrationForm(
                normalize(request.getParameter("fullName")),
                normalize(request.getParameter("email")),
                normalize(request.getParameter("username")),
                request.getParameter("password"),
                request.getParameter("confirmPassword"),
                "true".equals(request.getParameter("acceptTerms")));

        try {
            Map<String, String> errors = accountService.validateRegistration(form);
            if (errors.isEmpty()) {
                try {
                    accountService.register(form, request.getRemoteAddr());
                    request.getSession().setAttribute(FLASH_REGISTERED, Boolean.TRUE);
                    response.sendRedirect(request.getContextPath() + "/login");
                    return;
                } catch (SQLIntegrityConstraintViolationException e) {
                    // Người khác vừa đăng ký trùng tên đăng nhập/email giữa lúc kiểm tra và lúc lưu
                    errors = accountService.validateRegistration(form);
                }
            }
            // Không gửi lại mật khẩu ra HTML: người dùng nhập lại hai ô mật khẩu
            request.setAttribute("form", form);
            request.setAttribute("errors", errors);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không đăng ký được tài khoản", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
