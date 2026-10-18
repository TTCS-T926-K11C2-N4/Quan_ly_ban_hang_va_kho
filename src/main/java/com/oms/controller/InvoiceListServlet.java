package com.oms.controller;

import com.oms.model.Invoice;
import com.oms.model.Permission;
import com.oms.service.InvoiceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

// S7-01: danh sách hoá đơn (lọc theo trạng thái, quá hạn) và các phiếu xuất đã giao chờ lập hoá đơn, gom theo đại lý
@WebServlet("/invoices")
public class InvoiceListServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/invoices/invoice-list.jsp";
    static final String FLASH_MESSAGE = "invoiceFlashMessage";
    static final String FLASH_ERROR = "invoiceFlashError";
    private static final Set<String> STATUSES = Set.of(Invoice.ISSUED, Invoice.PARTIALLY_PAID, Invoice.PAID,
            Invoice.CANCELLED);

    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        String status = request.getParameter("status");
        if (status == null || !STATUSES.contains(status)) {
            status = null;
        }
        boolean overdue = "1".equals(request.getParameter("overdue"));
        Long customerId = AccountFormParser.parseId(request.getParameter("customer"));
        try {
            if (!invoiceService.canView(userId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            boolean canManage = CurrentUser.get(request).can(Permission.INVOICE_MANAGE);
            request.setAttribute("invoices", invoiceService.findInvoices(customerId, status, overdue));
            if (canManage) {
                request.setAttribute("candidates", invoiceService.candidates());
            }
            request.setAttribute("canManage", canManage);
        } catch (SQLException e) {
            log("Không tải được danh sách hoá đơn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("status", status);
        request.setAttribute("overdue", overdue);
        request.setAttribute("customerId", customerId);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
