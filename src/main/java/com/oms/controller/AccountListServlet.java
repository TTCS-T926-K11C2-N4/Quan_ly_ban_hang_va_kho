package com.oms.controller;

import com.oms.model.AccountFilter;
import com.oms.model.AccountStatus;
import com.oms.service.AccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/accounts")
public class AccountListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/accounts/account-list.jsp";
    private static final int KEYWORD_MAX_LENGTH = 100;

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = normalize(request.getParameter("keyword"));
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            keyword = keyword.substring(0, KEYWORD_MAX_LENGTH);
        }
        String roleFilter = normalize(request.getParameter("roleFilter"));
        AccountStatus statusFilter = AccountStatus.fromCode(normalize(request.getParameter("statusFilter")));
        int page = parsePage(request.getParameter("page"));

        try {
            AccountFilter filter = new AccountFilter(keyword, roleFilter, statusFilter);
            request.setAttribute("accountPage", accountService.search(filter, page));
            request.setAttribute("roles", accountService.getAllRoles());
        } catch (SQLException e) {
            log("Không tải được danh sách tài khoản", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Flash.moveToRequest(request, AccountCreateServlet.FLASH_CREATED_USERNAME, "createdUsername");
        Flash.moveToRequest(request, AccountCreateServlet.FLASH_CREATED_EMAIL, "createdEmail");
        Flash.moveToRequest(request, AccountEditServlet.FLASH_MESSAGE, "flashMessage");

        request.setAttribute("keyword", keyword);
        request.setAttribute("roleFilter", roleFilter);
        request.setAttribute("statusFilter", statusFilter == null ? null : statusFilter.name());
        request.setAttribute("statuses", AccountStatus.values());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int parsePage(String value) {
        try {
            return value == null ? 1 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
