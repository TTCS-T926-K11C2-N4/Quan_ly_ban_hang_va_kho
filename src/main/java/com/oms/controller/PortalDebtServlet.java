package com.oms.controller;

import com.oms.model.CustomerCredit;
import com.oms.model.Invoice;
import com.oms.model.SessionUser;
import com.oms.service.PortalDebtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

// S7-07: công nợ của chính đại lý: hoá đơn kèm số tiền, hạn thanh toán, trạng thái; tổng còn nợ và hạn mức còn lại
@WebServlet("/portal/debts")
public class PortalDebtServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/portal/portal-debts.jsp";

    private final PortalDebtService portalDebtService = new PortalDebtService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        try {
            List<Invoice> invoices = portalDebtService.findInvoices(user.getId());
            BigDecimal outstanding = BigDecimal.ZERO;
            for (Invoice invoice : invoices) {
                outstanding = outstanding.add(invoice.getOutstanding());
            }
            CustomerCredit credit = portalDebtService.findCredit(user.getId());
            request.setAttribute("invoices", invoices);
            request.setAttribute("outstanding", outstanding);
            request.setAttribute("credit", credit);
        } catch (SQLException e) {
            log("Không tải được công nợ của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
