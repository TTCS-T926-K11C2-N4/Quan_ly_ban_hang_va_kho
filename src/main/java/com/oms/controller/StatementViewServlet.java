package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.ReconciliationStatement;
import com.oms.service.InvoiceService;
import com.oms.service.ReconciliationStatementService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-06: chi tiết biên bản (GET) và gửi biên bản nháp cho đại lý (POST)
@WebServlet("/statements/view")
public class StatementViewServlet extends HttpServlet {

    static final String FLASH_MESSAGE = "statementViewFlashMessage";
    private static final String VIEW = "/WEB-INF/views/statements/statement-view.jsp";

    private final ReconciliationStatementService statementService = new ReconciliationStatementService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            ReconciliationStatement statement = id == null ? null : statementService.find(id);
            if (statement == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, StatementServlet.FLASH_ERROR, "flashError");
            request.setAttribute("statement", statement);
            request.setAttribute("canManage", CurrentUser.get(request).can(Permission.INVOICE_MANAGE));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được biên bản đối chiếu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())
                    || !CurrentUser.get(request).can(Permission.INVOICE_MANAGE)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (id == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            ReconciliationStatementService.Outcome outcome = statementService.send(
                    CurrentUser.get(request).getId(), id, request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(StatementServlet.FLASH_ERROR, outcome.getError());
            } else {
                request.getSession().setAttribute(FLASH_MESSAGE, "Đã gửi biên bản " + outcome.getStatementNo()
                        + " cho đại lý. Đại lý xác nhận hoặc báo lệch trên cổng.");
            }
            response.sendRedirect(request.getContextPath() + "/statements/view?id=" + id);
        } catch (SQLException e) {
            log("Không gửi được biên bản đối chiếu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
