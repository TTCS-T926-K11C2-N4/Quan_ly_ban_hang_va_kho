package com.oms.controller;

import com.oms.dao.WarehouseDao;
import com.oms.model.Permission;
import com.oms.service.StockTransferService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-07: danh sách phiếu chuyển kho nội bộ và hộp tạo phiếu (kho đi, kho đến)
@WebServlet("/transfers")
public class TransferListServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/transfers/transfer-list.jsp";
    static final String FLASH_MESSAGE = "transferFlashMessage";
    static final String FLASH_ERROR = "transferFlashError";

    private final StockTransferService transferService = new StockTransferService();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("transfers", transferService.findAll());
            request.setAttribute("warehouses", warehouseDao.findActive());
        } catch (SQLException e) {
            log("Không tải được danh sách phiếu chuyển kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("canManage", CurrentUser.get(request).can(Permission.INVENTORY_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
