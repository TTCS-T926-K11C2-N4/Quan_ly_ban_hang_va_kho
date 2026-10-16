package com.oms.controller;

import com.oms.model.Permission;
import com.oms.service.AdjustmentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S8-03: danh sách phiếu điều chỉnh tồn kho
@WebServlet("/adjustments")
public class AdjustmentListServlet extends HttpServlet {

    static final String FLASH_MESSAGE = "adjustmentFlashMessage";
    static final String FLASH_ERROR = "adjustmentFlashError";
    private static final String VIEW = "/WEB-INF/views/adjustments/adjustment-list.jsp";

    private final AdjustmentService adjustmentService = new AdjustmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("adjustments", adjustmentService.findAll());
        } catch (SQLException e) {
            log("Không tải được phiếu điều chỉnh tồn kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("canCreate", CurrentUser.get(request).can(Permission.INVENTORY_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
