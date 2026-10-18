package com.oms.controller;

import com.oms.service.ReturnService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// S8-01: chọn hoá đơn gốc, chọn dòng hàng, số lượng, tình trạng và lý do để lập phiếu trả hàng
@WebServlet("/returns/new")
public class ReturnNewServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/returns/return-form.jsp";

    private final ReturnService returnService = new ReturnService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long invoiceId = AccountFormParser.parseId(request.getParameter("invoice"));
        try {
            ReturnService.Access access = returnService.access(CurrentUser.get(request).getId());
            if (!access.isAllowed() || !access.isCanCreate()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            show(request, response, access, invoiceId);
        } catch (SQLException e) {
            log("Không tải được form phiếu trả hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long invoiceId = AccountFormParser.parseId(request.getParameter("invoice"));
        try {
            ReturnService.Access access = returnService.access(CurrentUser.get(request).getId());
            if (!access.isAllowed() || !access.isCanCreate()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (invoiceId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            List<ReturnService.Input> inputs = new ArrayList<>();
            String[] ids = request.getParameterValues("item");
            if (ids != null) {
                for (String raw : ids) {
                    Long itemId = AccountFormParser.parseId(raw);
                    if (itemId != null) {
                        inputs.add(new ReturnService.Input(itemId, request.getParameter("qty_" + itemId),
                                request.getParameter("condition_" + itemId)));
                    }
                }
            }
            ReturnService.Outcome outcome = returnService.create(CurrentUser.get(request).getId(),
                    access.getSalesRepId(), invoiceId, request.getParameter("reason"), request.getParameter("note"),
                    inputs, request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.setAttribute("formError", outcome.getError());
                request.setAttribute("reason", request.getParameter("reason"));
                request.setAttribute("note", request.getParameter("note"));
                show(request, response, access, invoiceId);
                return;
            }
            request.getSession().setAttribute(ReturnListServlet.FLASH_MESSAGE, "Đã lập phiếu trả hàng "
                    + outcome.getReturnNo() + ". Tồn kho chỉ tăng khi Quản lý kho xác nhận nhận hàng.");
            response.sendRedirect(request.getContextPath() + "/returns/view?id=" + outcome.getId());
        } catch (SQLException e) {
            log("Không lập được phiếu trả hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, ReturnService.Access access,
                      Long invoiceId) throws SQLException, ServletException, IOException {
        if (invoiceId == null) {
            request.setAttribute("invoices", returnService.invoiceOptions(access.getSalesRepId()));
        } else {
            ReturnService.InvoiceLines lines = returnService.lines(invoiceId, access.getSalesRepId());
            if (lines == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("invoiceId", invoiceId);
            request.setAttribute("invoiceNo", lines.getInvoiceNo());
            request.setAttribute("lines", lines.getLines());
        }
        request.setAttribute("reasons", com.oms.model.SalesReturn.REASONS);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
