package com.oms.controller;

import com.oms.model.Payment;
import com.oms.service.InvoiceService;
import com.oms.service.PaymentService;
import com.oms.util.Code128;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-09: trang in phiếu thu (trình duyệt: In → Lưu dạng PDF), kèm mã vạch số phiếu
@WebServlet("/payments/print")
public class PaymentPrintServlet extends HttpServlet {

    private static final int BARCODE_HEIGHT = 48;

    private final PaymentService paymentService = new PaymentService();
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
            Payment payment = id == null ? null : paymentService.find(id);
            if (payment == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("payment", payment);
            request.setAttribute("barcode", Code128.svg(payment.getReceiptNo(), BARCODE_HEIGHT));
            request.getRequestDispatcher("/WEB-INF/views/payments/payment-print.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được phiếu thu để in", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
