package com.oms.controller;

import com.oms.model.CreditNote;
import com.oms.model.Permission;
import com.oms.service.CreditNoteService;
import com.oms.service.InvoiceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S8-02: chi tiết chứng từ điều chỉnh (GET /credit-notes/view) và lập chứng từ từ phiếu trả đã nhận hàng
// (POST /credit-notes/issue)
@WebServlet(urlPatterns = {"/credit-notes/view", "/credit-notes/issue"})
public class CreditNoteViewServlet extends HttpServlet {

    static final String FLASH_MESSAGE = "creditNoteFlashMessage";
    private static final String VIEW = "/WEB-INF/views/credit-notes/credit-note-view.jsp";

    private final CreditNoteService creditNoteService = new CreditNoteService();
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
            CreditNote note = id == null ? null : creditNoteService.find(id);
            if (note == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            request.setAttribute("note", note);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được chứng từ điều chỉnh", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long returnId = AccountFormParser.parseId(request.getParameter("return"));
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())
                    || !CurrentUser.get(request).can(Permission.INVOICE_MANAGE)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (returnId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            CreditNoteService.Outcome outcome = creditNoteService.issue(CurrentUser.get(request).getId(), returnId,
                    request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(ReturnListServlet.FLASH_ERROR, outcome.getError());
                response.sendRedirect(request.getContextPath() + "/returns/view?id=" + returnId);
                return;
            }
            request.getSession().setAttribute(FLASH_MESSAGE, "Đã lập chứng từ điều chỉnh "
                    + outcome.getCreditNoteNo() + ". Công nợ của đại lý đã được giảm trừ.");
            response.sendRedirect(request.getContextPath() + "/credit-notes/view?id=" + outcome.getId());
        } catch (SQLException e) {
            log("Không lập được chứng từ điều chỉnh", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
