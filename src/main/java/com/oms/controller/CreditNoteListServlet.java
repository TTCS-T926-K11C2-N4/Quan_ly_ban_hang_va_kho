package com.oms.controller;

import com.oms.service.CreditNoteService;
import com.oms.service.InvoiceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S8-02: danh sách chứng từ điều chỉnh giảm trừ công nợ
@WebServlet("/credit-notes")
public class CreditNoteListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/credit-notes/credit-note-list.jsp";

    private final CreditNoteService creditNoteService = new CreditNoteService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("creditNotes", creditNoteService.findAll());
        } catch (SQLException e) {
            log("Không tải được chứng từ điều chỉnh", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
