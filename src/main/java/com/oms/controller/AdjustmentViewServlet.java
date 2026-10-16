package com.oms.controller;

import com.oms.model.StockAdjustment;
import com.oms.service.AdjustmentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S8-03: chi tiết phiếu (GET /adjustments/view) và duyệt / từ chối (POST /adjustments/approve, /adjustments/reject)
@WebServlet(urlPatterns = {"/adjustments/view", "/adjustments/approve", "/adjustments/reject"})
public class AdjustmentViewServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/adjustments/adjustment-view.jsp";

    private final AdjustmentService adjustmentService = new AdjustmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            StockAdjustment adjustment = id == null ? null : adjustmentService.find(id);
            if (adjustment == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, AdjustmentListServlet.FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, AdjustmentListServlet.FLASH_ERROR, "flashError");
            request.setAttribute("adjustment", adjustment);
            request.setAttribute("canDecide", CurrentUser.get(request).can(com.oms.model.Permission.STOCKTAKE_FINALIZE)
                    && adjustment.getCreatedBy() != CurrentUser.get(request).getId());
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được phiếu điều chỉnh tồn kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        if (id == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        boolean approve = request.getServletPath().endsWith("/approve");
        try {
            AdjustmentService.Outcome outcome = adjustmentService.decide(CurrentUser.get(request).getId(), id,
                    approve, request.getParameter("reason"), request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(AdjustmentListServlet.FLASH_ERROR, outcome.getError());
            } else {
                request.getSession().setAttribute(AdjustmentListServlet.FLASH_MESSAGE, approve
                        ? "Đã duyệt phiếu " + outcome.getAdjustmentNo() + ", tồn kho đã được điều chỉnh."
                        : "Đã từ chối phiếu " + outcome.getAdjustmentNo() + ".");
            }
            response.sendRedirect(request.getContextPath() + "/adjustments/view?id=" + id);
        } catch (SQLException e) {
            log("Không xử lý được phiếu điều chỉnh tồn kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
