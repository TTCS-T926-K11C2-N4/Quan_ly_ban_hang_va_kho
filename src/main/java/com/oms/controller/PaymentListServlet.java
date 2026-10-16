package com.oms.controller;

import com.oms.model.Permission;
import com.oms.service.InvoiceService;
import com.oms.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-02: đại lý đang còn nợ và các phiếu thu đã ghi nhận
@WebServlet("/payments")
public class PaymentListServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/payments/payment-list.jsp";
    static final String FLASH_MESSAGE = "paymentFlashMessage";
    static final String FLASH_ERROR = "paymentFlashError";

    private final PaymentService paymentService = new PaymentService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long customerId = AccountFormParser.parseId(request.getParameter("customer"));
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("debtors", paymentService.debtors());
            request.setAttribute("payments", paymentService.findPayments(customerId));
            request.setAttribute("pendingPayments", paymentService.findPending());
        } catch (SQLException e) {
            log("Không tải được danh sách phiếu thu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("canManage", CurrentUser.get(request).can(Permission.INVOICE_MANAGE));
        request.setAttribute("customerId", customerId);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
