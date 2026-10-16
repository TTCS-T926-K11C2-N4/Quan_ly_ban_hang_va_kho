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
import java.util.ArrayList;
import java.util.List;

// S8-03: lập phiếu điều chỉnh tồn kho: chọn kho, rồi chọn lô, số lượng trừ, lý do và ghi chú
@WebServlet("/adjustments/new")
public class AdjustmentNewServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/adjustments/adjustment-form.jsp";

    private final AdjustmentService adjustmentService = new AdjustmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouse"));
        try {
            show(request, response, warehouseId);
        } catch (SQLException e) {
            log("Không tải được form điều chỉnh tồn kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouse"));
        try {
            if (warehouseId == null || !adjustmentService.isActiveWarehouse(warehouseId)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            List<AdjustmentService.Input> inputs = new ArrayList<>();
            String[] lots = request.getParameterValues("lot");
            if (lots != null) {
                for (String raw : lots) {
                    Long lotId = AccountFormParser.parseId(raw);
                    if (lotId != null) {
                        inputs.add(new AdjustmentService.Input(lotId, request.getParameter("qty_" + lotId)));
                    }
                }
            }
            AdjustmentService.Outcome outcome = adjustmentService.create(CurrentUser.get(request).getId(),
                    warehouseId, request.getParameter("reason"), request.getParameter("note"), inputs,
                    request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.setAttribute("formError", outcome.getError());
                request.setAttribute("reason", request.getParameter("reason"));
                request.setAttribute("note", request.getParameter("note"));
                show(request, response, warehouseId);
                return;
            }
            request.getSession().setAttribute(AdjustmentListServlet.FLASH_MESSAGE, "Đã lập phiếu điều chỉnh "
                    + outcome.getAdjustmentNo() + ". Tồn kho chỉ bị trừ khi Quản lý kho khác duyệt phiếu.");
            response.sendRedirect(request.getContextPath() + "/adjustments/view?id=" + outcome.getId());
        } catch (SQLException e) {
            log("Không lập được phiếu điều chỉnh tồn kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, Long warehouseId)
            throws SQLException, ServletException, IOException {
        if (warehouseId == null) {
            request.setAttribute("warehouses", adjustmentService.warehouses());
        } else {
            if (!adjustmentService.isActiveWarehouse(warehouseId)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("warehouseId", warehouseId);
            request.setAttribute("lots", adjustmentService.lotOptions(warehouseId));
        }
        request.setAttribute("reasons", StockAdjustment.REASONS);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
