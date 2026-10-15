package com.oms.controller;

import com.oms.model.DebtAgingRow;
import com.oms.service.DebtLedgerService;
import com.oms.service.InvoiceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// S7-03: sổ công nợ theo đại lý, phân tích tuổi nợ, lọc riêng nhóm quá hạn
@WebServlet("/debts")
public class DebtLedgerServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/debts/debt-ledger.jsp";

    private final DebtLedgerService debtLedgerService = new DebtLedgerService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean overdueOnly = "1".equals(request.getParameter("overdue"));
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            List<DebtAgingRow> rows = debtLedgerService.rows(overdueOnly);
            request.setAttribute("rows", rows);
            request.setAttribute("total", debtLedgerService.total(rows));
        } catch (SQLException e) {
            log("Không tải được sổ công nợ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("overdue", overdueOnly);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
