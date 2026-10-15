package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.Stocktake;
import com.oms.service.StocktakeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-08: chi tiết phiếu kiểm kê: số sổ tại thời điểm chụp, số đếm thực tế, chênh lệch từng dòng;
// nhập số đếm (INVENTORY_MANAGE) và chốt phiếu (STOCKTAKE_FINALIZE, chỉ Quản lý kho)
@WebServlet("/stocktakes/view")
public class StocktakeViewServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/stocktakes/stocktake-view.jsp";
    static final String FLASH_MESSAGE = "stocktakeViewFlashMessage";
    static final String FLASH_ERROR = "stocktakeViewFlashError";

    private final StocktakeService stocktakeService = new StocktakeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            Stocktake stocktake = id == null ? null : stocktakeService.find(id);
            if (stocktake == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, FLASH_ERROR, "flashError");
            request.setAttribute("stocktake", stocktake);
            request.setAttribute("canCount", stocktake.isOpen()
                    && CurrentUser.get(request).can(Permission.INVENTORY_MANAGE));
            request.setAttribute("canFinalize", stocktake.isOpen()
                    && CurrentUser.get(request).can(Permission.STOCKTAKE_FINALIZE));
            request.setAttribute("reasonMaxLength", StocktakeService.REASON_MAX_LENGTH);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được phiếu kiểm kê", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
