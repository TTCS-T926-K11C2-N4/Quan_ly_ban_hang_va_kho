package com.oms.controller;

import com.oms.model.ProductCategory;
import com.oms.service.ProductCategoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S2-06: sản phẩm của một nhóm hàng, kèm danh sách nhóm có thể chuyển tới (form gửi tới /categories/products/move)
@WebServlet("/categories/products")
public class CategoryProductsServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/categories/category-products.jsp";

    private final ProductCategoryService categoryService = new ProductCategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            ProductCategory category = id == null ? null : categoryService.find(id);
            if (category == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("category", category);
            request.setAttribute("products", categoryService.getProducts(category.getId()));
            request.setAttribute("moveTargets", categoryService.getTree().stream()
                    .filter(target -> target.isActive() && target.getId() != category.getId())
                    .toList());
        } catch (SQLException e) {
            log("Không tải được sản phẩm của nhóm hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, CategoryListServlet.FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, CategoryListServlet.FLASH_ERROR, "flashError");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
