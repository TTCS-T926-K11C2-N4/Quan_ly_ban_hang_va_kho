package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.StockStatus;
import com.oms.service.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S2-05: danh sách sản phẩm có tìm kiếm (tên, SKU), lọc nhóm hàng/trạng thái/kho, sắp xếp, phân trang
@WebServlet("/products")
public class ProductListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/products/product-list.jsp";
    private static final int KEYWORD_MAX_LENGTH = 100;

    // Thông báo một lần sau khi thêm/sửa/xoá/đổi trạng thái (Post/Redirect/Get)
    static final String FLASH_MESSAGE = "productFlashMessage";
    static final String FLASH_ERROR = "productFlashError";

    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("keyword");
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            keyword = keyword.substring(0, KEYWORD_MAX_LENGTH);
        }
        Long categoryId = AccountFormParser.parseId(request.getParameter("categoryId"));
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouseId"));
        StockStatus status = StockStatus.fromCode(request.getParameter("status"));
        String sort = request.getParameter("sort");
        sort = sort != null && ProductService.SORTS.contains(sort) ? sort : "name";
        boolean canViewCost = CurrentUser.get(request).can(Permission.COST_PRICE_VIEW);

        try {
            request.setAttribute("productPage", productService.search(keyword, categoryId, warehouseId, status, sort,
                    parsePage(request.getParameter("page")), canViewCost));
            request.setAttribute("categories", productService.getCategoryTree());
            request.setAttribute("warehouses", productService.getWarehouses());
        } catch (SQLException e) {
            log("Không tải được danh sách sản phẩm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("keyword", keyword);
        request.setAttribute("categoryFilter", categoryId);
        request.setAttribute("warehouseFilter", warehouseId);
        request.setAttribute("statusFilter", status == null ? null : status.name());
        request.setAttribute("statuses", StockStatus.values());
        request.setAttribute("sort", sort);
        request.setAttribute("canViewCost", canViewCost);
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static int parsePage(String value) {
        try {
            return value == null ? 1 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
