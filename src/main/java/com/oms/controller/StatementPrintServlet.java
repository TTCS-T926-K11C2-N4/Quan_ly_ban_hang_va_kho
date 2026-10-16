package com.oms.controller;

import com.oms.model.ReconciliationStatement;
import com.oms.model.SessionUser;
import com.oms.service.InvoiceService;
import com.oms.service.ReconciliationStatementService;
import com.oms.util.Code128;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-06: trang in biên bản đối chiếu (trình duyệt: In → Lưu dạng PDF) cho hai bên ký xác nhận. Nhân viên xem mọi
// biên bản; đại lý chỉ xem biên bản đã gửi của chính mình.
@WebServlet(urlPatterns = {"/statements/print", "/portal/statements/print"})
public class StatementPrintServlet extends HttpServlet {

    private static final int BARCODE_HEIGHT = 48;

    private final ReconciliationStatementService statementService = new ReconciliationStatementService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            ReconciliationStatement statement;
            boolean portal = request.getServletPath().startsWith("/portal");
            if (portal) {
                if (!user.isCustomer()) {
                    response.sendRedirect(request.getContextPath() + user.getHomePath());
                    return;
                }
                statement = id == null ? null : statementService.findOwn(user.getId(), id);
            } else {
                if (!invoiceService.canView(user.getId())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                statement = id == null ? null : statementService.find(id);
            }
            if (statement == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("statement", statement);
            request.setAttribute("barcode", Code128.svg(statement.getStatementNo(), BARCODE_HEIGHT));
            request.setAttribute("backUrl", portal ? "/portal/statements" : "/statements/view?id=" + statement.getId());
            request.getRequestDispatcher("/WEB-INF/views/statements/statement-print.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được biên bản đối chiếu để in", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
