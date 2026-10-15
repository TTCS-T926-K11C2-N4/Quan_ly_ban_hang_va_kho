package com.oms.controller;

import com.oms.model.SalesReturn;
import com.oms.service.ReturnService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S8-01: chi tiết phiếu trả (GET) và các thao tác nhận hàng / huỷ (POST /returns/receive, /returns/cancel)
@WebServlet(urlPatterns = {"/returns/view", "/returns/receive", "/returns/cancel"})
public class ReturnViewServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/returns/return-view.jsp";

    private final ReturnService returnService = new ReturnService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            ReturnService.Access access = returnService.access(CurrentUser.get(request).getId());
            if (!access.isAllowed()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            SalesReturn salesReturn = id == null ? null : returnService.find(id, access.getSalesRepId());
            if (salesReturn == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, ReturnListServlet.FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, ReturnListServlet.FLASH_ERROR, "flashError");
            request.setAttribute("salesReturn", salesReturn);
            request.setAttribute("access", access);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được phiếu trả hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        long userId = CurrentUser.get(request).getId();
        try {
            ReturnService.Access access = returnService.access(userId);
            boolean receiving = request.getServletPath().endsWith("/receive");
            if (!access.isAllowed() || (receiving && !access.isCanReceive())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (id == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            ReturnService.Outcome outcome = receiving
                    ? returnService.receive(userId, id, request.getRemoteAddr())
                    : returnService.cancel(userId, access.getSalesRepId(), id, request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(ReturnListServlet.FLASH_ERROR, outcome.getError());
            } else {
                request.getSession().setAttribute(ReturnListServlet.FLASH_MESSAGE, receiving
                        ? "Đã xác nhận nhận hàng trả " + outcome.getReturnNo() + " và cộng tồn kho."
                        : "Đã huỷ phiếu trả " + outcome.getReturnNo() + ".");
            }
            response.sendRedirect(request.getContextPath() + "/returns/view?id=" + id);
        } catch (SQLException e) {
            log("Không xử lý được phiếu trả hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
