package com.oms.controller;

import com.oms.service.InvoiceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// S7-01: phát hành một hoá đơn từ các phiếu xuất đã giao được chọn (cùng một đại lý)
@WebServlet("/invoices/issue")
public class InvoiceIssueServlet extends HttpServlet {

    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Long> ids = new ArrayList<>();
        String[] values = request.getParameterValues("note_id");
        if (values != null) {
            for (String value : values) {
                Long id = AccountFormParser.parseId(value);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        try {
            InvoiceService.Outcome outcome = invoiceService.issue(CurrentUser.get(request).getId(), ids,
                    request.getParameter("note"), request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(InvoiceListServlet.FLASH_ERROR, outcome.getError());
                response.sendRedirect(request.getContextPath() + "/invoices");
                return;
            }
            request.getSession().setAttribute(InvoiceViewServlet.FLASH_MESSAGE, "Đã phát hành hoá đơn "
                    + outcome.getInvoiceNo() + ". Hoá đơn đã phát hành không sửa, không xoá được.");
            response.sendRedirect(request.getContextPath() + "/invoices/view?id=" + outcome.getId());
        } catch (SQLException e) {
            log("Không phát hành được hoá đơn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
