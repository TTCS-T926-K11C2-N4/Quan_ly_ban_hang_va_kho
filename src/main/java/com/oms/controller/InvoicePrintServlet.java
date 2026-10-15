package com.oms.controller;

import com.oms.model.Invoice;
import com.oms.service.InvoiceService;
import com.oms.util.Code128;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-01: trang in hoá đơn A4 (trình duyệt: In → Lưu dạng PDF), kèm mã vạch số hoá đơn
@WebServlet("/invoices/print")
public class InvoicePrintServlet extends HttpServlet {

    private static final int BARCODE_HEIGHT = 48;

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
            Invoice invoice = id == null ? null : invoiceService.find(id);
            if (invoice == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("invoice", invoice);
            request.setAttribute("barcode", Code128.svg(invoice.getInvoiceNo(), BARCODE_HEIGHT));
            request.getRequestDispatcher("/WEB-INF/views/invoices/invoice-print.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được hoá đơn để in", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
