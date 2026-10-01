package com.oms.controller;

import com.oms.service.ProductCategoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S2-06: cây nhóm hàng có tìm kiếm, lọc trạng thái, phân trang theo nhóm gốc
@WebServlet("/categories")
public class CategoryListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/categories/category-list.jsp";

    // Thông báo một lần sau khi thêm/sửa/xoá/đổi trạng thái (Post/Redirect/Get)
    static final String FLASH_MESSAGE = "categoryFlashMessage";
    static final String FLASH_ERROR = "categoryFlashError";

    private final ProductCategoryService categoryService = new ProductCategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("keyword");
        keyword = keyword == null ? "" : keyword.trim();
        String status = request.getParameter("status");
        Boolean active = "active".equals(status) ? Boolean.TRUE : "inactive".equals(status) ? Boolean.FALSE : null;

        try {
            request.setAttribute("categoryPage", categoryService.search(keyword, active, parsePage(request)));
        } catch (SQLException e) {
            log("Không tải được cây nhóm hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("keyword", keyword);
        request.setAttribute("statusFilter", active == null ? "all" : status);
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
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
