package com.oms.controller;

import com.oms.model.Invoice;
import com.oms.service.InvoiceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-01: chi tiết một hoá đơn (xem, in / lưu PDF); hoá đơn đã phát hành chỉ xem
@WebServlet("/invoices/view")
public class InvoiceViewServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/invoices/invoice-view.jsp";
    static final String FLASH_MESSAGE = "invoiceViewFlashMessage";

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
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            request.setAttribute("invoice", invoice);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được hoá đơn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
