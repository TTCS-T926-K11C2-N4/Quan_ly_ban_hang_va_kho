package com.oms.controller;

import com.oms.model.OpenInvoice;
import com.oms.service.InvoiceService;
import com.oms.service.PaymentService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// S7-02: form ghi nhận thanh toán của một đại lý; chọn hoá đơn cụ thể hoặc để trống để đối trừ cũ nhất trước
@WebServlet("/payments/new")
public class PaymentCreateServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/payments/payment-form.jsp";

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
            String name = customerId == null ? null : paymentService.customerName(customerId);
            if (name == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            show(request, response, customerId, name);
        } catch (SQLException e) {
            log("Không tải được form ghi nhận thanh toán", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long customerId = AccountFormParser.parseId(request.getParameter("customer"));
        List<Long> invoiceIds = new ArrayList<>();
        String[] values = request.getParameterValues("invoice_id");
        if (values != null) {
            for (String value : values) {
                Long id = AccountFormParser.parseId(value);
                if (id != null) {
                    invoiceIds.add(id);
                }
            }
        }
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (customerId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            PaymentService.Outcome outcome = paymentService.record(CurrentUser.get(request).getId(), customerId,
                    request.getParameter("amount"), request.getParameter("method"), request.getParameter("date"),
                    request.getParameter("note"), invoiceIds, request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.setAttribute("formError", outcome.getError());
                request.setAttribute("amount", request.getParameter("amount"));
                request.setAttribute("method", request.getParameter("method"));
                request.setAttribute("date", request.getParameter("date"));
                request.setAttribute("note", request.getParameter("note"));
                request.setAttribute("selected", invoiceIds);
                show(request, response, customerId, paymentService.customerName(customerId));
                return;
            }
            request.getSession().setAttribute(PaymentListServlet.FLASH_MESSAGE,
                    "Đã ghi nhận phiếu thu " + outcome.getReceiptNo() + ". Phiếu đã ghi nhận không xoá được.");
            response.sendRedirect(request.getContextPath() + "/payments");
        } catch (SQLException e) {
            log("Không ghi nhận được thanh toán", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, long customerId, String name)
            throws SQLException, ServletException, IOException {
        List<OpenInvoice> open = paymentService.openInvoices(customerId);
        BigDecimal total = BigDecimal.ZERO;
        for (OpenInvoice invoice : open) {
            total = total.add(invoice.getOutstanding());
        }
        request.setAttribute("customerId", customerId);
        request.setAttribute("customerName", name);
        request.setAttribute("openInvoices", open);
        request.setAttribute("totalOutstanding", total);
        if (request.getAttribute("selected") == null) {
            request.setAttribute("selected", List.of());
        }
        if (request.getAttribute("date") == null) {
            request.setAttribute("date", DateTimeUtil.today().toString());
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
