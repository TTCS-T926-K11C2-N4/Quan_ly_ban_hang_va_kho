package com.oms.controller;

import com.oms.model.SessionUser;
import com.oms.service.ReconciliationStatementService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-06: biên bản đối chiếu công nợ công ty gửi cho đại lý; đại lý xác nhận hoặc báo lệch ngay trên cổng
@WebServlet("/portal/statements")
public class PortalStatementServlet extends HttpServlet {

    static final String FLASH_MESSAGE = "portalStatementFlashMessage";
    static final String FLASH_ERROR = "portalStatementFlashError";
    private static final String VIEW = "/WEB-INF/views/portal/portal-statements.jsp";

    private final ReconciliationStatementService statementService = new ReconciliationStatementService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        try {
            request.setAttribute("statements", statementService.findOwn(user.getId()));
        } catch (SQLException e) {
            log("Không tải được biên bản đối chiếu của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
