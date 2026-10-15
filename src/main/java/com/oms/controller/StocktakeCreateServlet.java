package com.oms.controller;

import com.oms.service.StocktakeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-08: tạo phiếu kiểm kê (chụp số tồn sổ theo lô tại thời điểm tạo) rồi mở phiếu để nhập số đếm
@WebServlet("/stocktakes/new")
public class StocktakeCreateServlet extends HttpServlet {

    private final StocktakeService stocktakeService = new StocktakeService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouse"));
        String scope = request.getParameter("scope");
        Long categoryId = "CATEGORY".equals(scope) ? AccountFormParser.parseId(request.getParameter("category")) : null;
        String listUrl = request.getContextPath() + "/stocktakes";
        if (warehouseId == null) {
            request.getSession().setAttribute(StocktakeListServlet.FLASH_ERROR, "Vui lòng chọn kho cần kiểm kê.");
            response.sendRedirect(listUrl);
            return;
        }
        if ("CATEGORY".equals(scope) && categoryId == null) {
            request.getSession().setAttribute(StocktakeListServlet.FLASH_ERROR,
                    "Vui lòng chọn nhóm hàng cần kiểm kê.");
            response.sendRedirect(listUrl);
            return;
        }
        try {
            StocktakeService.Outcome outcome = stocktakeService.create(CurrentUser.get(request).getId(), warehouseId,
                    categoryId, request.getRemoteAddr());
            if (outcome.getError() != null) {
                request.getSession().setAttribute(StocktakeListServlet.FLASH_ERROR, outcome.getError());
                response.sendRedirect(listUrl);
                return;
            }
            request.getSession().setAttribute(StocktakeViewServlet.FLASH_MESSAGE, "Đã tạo phiếu kiểm kê "
                    + outcome.getStocktakeNo() + ". Số tồn sổ đã được chốt tại thời điểm tạo phiếu; nhập số đếm"
                    + " thực tế cho từng dòng.");
            response.sendRedirect(request.getContextPath() + "/stocktakes/view?id=" + outcome.getId());
        } catch (SQLException e) {
            log("Không tạo được phiếu kiểm kê", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
