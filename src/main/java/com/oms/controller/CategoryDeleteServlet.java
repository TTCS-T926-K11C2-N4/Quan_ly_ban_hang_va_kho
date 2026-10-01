package com.oms.controller;

import com.oms.model.ProductCategory;
import com.oms.service.ProductCategoryService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

// S2-06: xoá nhóm hàng; nhóm còn sản phẩm hoặc nhóm con thì không xoá được
@WebServlet("/categories/delete")
public class CategoryDeleteServlet extends HttpServlet {

    private final ProductCategoryService categoryService = new ProductCategoryService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            ProductCategory category = id == null ? null : categoryService.find(id);
            if (category == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            HttpSession session = request.getSession();
            String error = categoryService.validateDelete(category);
            if (error != null) {
                session.setAttribute(CategoryListServlet.FLASH_ERROR, error);
            } else if (!categoryService.delete(category, CurrentUser.get(request).getId(), request.getRemoteAddr())) {
                session.setAttribute(CategoryListServlet.FLASH_ERROR, "Nhóm \"" + category.getName()
                        + "\" vừa có thêm sản phẩm hoặc nhóm con nên chưa xoá được. Hãy tải lại trang.");
            } else {
                session.setAttribute(CategoryListServlet.FLASH_MESSAGE, "Đã xoá nhóm hàng \"" + category.getName() + "\".");
            }
            response.sendRedirect(request.getContextPath() + "/categories");
        } catch (SQLException e) {
            log("Không xoá được nhóm hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
