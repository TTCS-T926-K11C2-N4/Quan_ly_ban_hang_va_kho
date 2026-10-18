package com.oms.controller;

import com.oms.dao.WarehouseDao;
import com.oms.model.LotStatus;
import com.oms.model.Permission;
import com.oms.service.LotService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-01: danh sách lô và hạn sử dụng (số lô, ngày sản xuất, hạn dùng, số còn lại, cảnh báo sắp hết hạn), bật / tắt
// quản lý lô theo SKU, đặt ngưỡng cảnh báo, cho phép xuất lô quá hạn (Quản lý kho)
@WebServlet("/lots")
public class LotListServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/lots/lot-list.jsp";
    static final String FLASH_MESSAGE = "lotFlashMessage";
    static final String FLASH_ERROR = "lotFlashError";

    private final LotService lotService = new LotService();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("keyword") == null ? "" : request.getParameter("keyword").trim();
        String productKeyword = request.getParameter("sku") == null ? "" : request.getParameter("sku").trim();
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouse"));
        LotStatus status = LotStatus.fromCode(request.getParameter("status"));
        try {
            long[] counts = lotService.countByStatus(keyword, warehouseId);
            request.setAttribute("expiredCount", counts[0]);
            request.setAttribute("nearCount", counts[1]);
            request.setAttribute("okCount", counts[2]);
            request.setAttribute("noneCount", counts[3]);
            request.setAttribute("nearDays", lotService.nearExpiryDays());
            request.setAttribute("lotPage", lotService.search(keyword, warehouseId, status, parsePage(request)));
            request.setAttribute("products", lotService.findProducts(productKeyword));
            request.setAttribute("warehouses", warehouseDao.findActive());
        } catch (SQLException e) {
            log("Không tải được danh sách lô", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("keyword", keyword);
        request.setAttribute("productKeyword", productKeyword);
        request.setAttribute("warehouseId", warehouseId);
        request.setAttribute("status", status);
        request.setAttribute("statuses", LotStatus.values());
        request.setAttribute("canManage", CurrentUser.get(request).can(Permission.INVENTORY_MANAGE));
        request.setAttribute("canApprove", CurrentUser.get(request).can(Permission.WAREHOUSE_MANAGE));
        request.setAttribute("reasonMaxLength", LotService.REASON_MAX_LENGTH);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static int parsePage(HttpServletRequest request) {
        try {
            String page = request.getParameter("page");
            return page == null ? 1 : Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
