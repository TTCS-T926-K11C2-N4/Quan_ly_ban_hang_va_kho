package com.oms.controller;

import com.oms.model.Invoice;
import com.oms.model.SessionUser;
import com.oms.service.PortalDebtService;
import com.oms.util.Code128;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-07: đại lý xem lại và in/tải PDF hoá đơn của chính mình; hoá đơn của đại lý khác trả 404
@WebServlet("/portal/invoices/print")
public class PortalInvoicePrintServlet extends HttpServlet {

    private static final int BARCODE_HEIGHT = 48;

    private final PortalDebtService portalDebtService = new PortalDebtService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            Invoice invoice = id == null ? null : portalDebtService.findInvoice(user.getId(), id);
            if (invoice == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("invoice", invoice);
            request.setAttribute("barcode", Code128.svg(invoice.getInvoiceNo(), BARCODE_HEIGHT));
            request.setAttribute("backUrl", "/portal/debts");
            request.getRequestDispatcher("/WEB-INF/views/invoices/invoice-print.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được hoá đơn của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
