package com.oms.controller;

import com.oms.dao.StockLedgerDao;
import com.oms.dao.WarehouseDao;
import com.oms.model.Permission;
import com.oms.service.StocktakeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-08: danh sách phiếu kiểm kê kho và hộp tạo phiếu theo kho hoặc nhóm hàng
@WebServlet("/stocktakes")
public class StocktakeListServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/stocktakes/stocktake-list.jsp";
    static final String FLASH_MESSAGE = "stocktakeFlashMessage";
    static final String FLASH_ERROR = "stocktakeFlashError";

    private final StocktakeService stocktakeService = new StocktakeService();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final StockLedgerDao ledgerDao = new StockLedgerDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("stocktakes", stocktakeService.findAll());
            request.setAttribute("warehouses", warehouseDao.findActive());
            request.setAttribute("categories", ledgerDao.findCategories());
        } catch (SQLException e) {
            log("Không tải được danh sách phiếu kiểm kê", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("canManage", CurrentUser.get(request).can(Permission.INVENTORY_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
