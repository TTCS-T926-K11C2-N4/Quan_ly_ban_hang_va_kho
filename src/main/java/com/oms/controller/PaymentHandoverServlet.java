package com.oms.controller;

import com.oms.service.InvoiceService;
import com.oms.service.PaymentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-05: kế toán xác nhận đã nhận tiền của khoản thu theo tuyến (/payments/confirm) hoặc huỷ khoản thu chờ nộp
// (/payments/void, kèm lý do)
@WebServlet(urlPatterns = {"/payments/confirm", "/payments/void"})
public class PaymentHandoverServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        long userId = CurrentUser.get(request).getId();
        try {
            if (!invoiceService.canView(userId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (id == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            boolean voiding = request.getServletPath().endsWith("/void");
            PaymentService.Outcome outcome = voiding
                    ? paymentService.voidPending(userId, id, request.getParameter("reason"), request.getRemoteAddr())
                    : paymentService.confirmHandover(userId, id, request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(PaymentListServlet.FLASH_ERROR, outcome.getError());
            } else {
                request.getSession().setAttribute(PaymentListServlet.FLASH_MESSAGE, voiding
                        ? "Đã huỷ khoản thu " + outcome.getReceiptNo() + "."
                        : "Đã xác nhận nhận tiền khoản thu " + outcome.getReceiptNo() + " và đối trừ công nợ.");
            }
            response.sendRedirect(request.getContextPath() + "/payments");
        } catch (SQLException e) {
            log("Không xử lý được khoản thu chờ nộp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
