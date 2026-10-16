package com.oms.controller;

import com.oms.service.InvoiceService;
import com.oms.service.ReconciliationStatementService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-06: danh sách biên bản đối chiếu công nợ và lập biên bản mới theo kỳ (GET danh sách, POST lập)
@WebServlet("/statements")
public class StatementServlet extends HttpServlet {

    static final String FLASH_MESSAGE = "statementFlashMessage";
    static final String FLASH_ERROR = "statementFlashError";
    private static final String VIEW = "/WEB-INF/views/statements/statement-list.jsp";

    private final ReconciliationStatementService statementService = new ReconciliationStatementService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("statements", statementService.findAll());
            request.setAttribute("customers", statementService.customers());
        } catch (SQLException e) {
            log("Không tải được biên bản đối chiếu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        java.time.LocalDate today = DateTimeUtil.today();
        request.setAttribute("defaultFrom", today.withDayOfMonth(1).toString());
        request.setAttribute("defaultTo", today.toString());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long customerId = AccountFormParser.parseId(request.getParameter("customer"));
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())
                    || !CurrentUser.get(request).can(com.oms.model.Permission.INVOICE_MANAGE)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (customerId == null) {
                request.getSession().setAttribute(FLASH_ERROR, "Chọn đại lý cần đối chiếu.");
                response.sendRedirect(request.getContextPath() + "/statements");
                return;
            }
            ReconciliationStatementService.Outcome outcome = statementService.create(
                    CurrentUser.get(request).getId(), customerId, request.getParameter("from"),
                    request.getParameter("to"), request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(FLASH_ERROR, outcome.getError());
                response.sendRedirect(request.getContextPath() + "/statements");
                return;
            }
            request.getSession().setAttribute(StatementViewServlet.FLASH_MESSAGE,
                    "Đã lập biên bản " + outcome.getStatementNo() + ". Kiểm tra số liệu rồi gửi cho đại lý.");
            response.sendRedirect(request.getContextPath() + "/statements/view?id=" + outcome.getId());
        } catch (SQLException e) {
            log("Không lập được biên bản đối chiếu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
