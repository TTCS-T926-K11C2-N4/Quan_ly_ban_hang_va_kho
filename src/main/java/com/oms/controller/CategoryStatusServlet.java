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

// S2-06: Ngừng hoạt động (cả nhánh) / Hoạt động lại (chỉ nhóm này). Tham số active=false|true.
@WebServlet("/categories/status")
public class CategoryStatusServlet extends HttpServlet {

    private final ProductCategoryService categoryService = new ProductCategoryService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        String activeParam = request.getParameter("active");
        try {
            ProductCategory category = id == null ? null : categoryService.find(id);
            if (category == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!"true".equals(activeParam) && !"false".equals(activeParam)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            HttpSession session = request.getSession();
            long actorId = CurrentUser.get(request).getId();
            if ("false".equals(activeParam)) {
                categoryService.deactivate(category, actorId, request.getRemoteAddr());
                session.setAttribute(CategoryListServlet.FLASH_MESSAGE, "Đã ngừng hoạt động nhóm \""
                        + category.getName() + "\"" + (category.getChildCount() > 0 ? " và các nhóm con." : "."));
            } else {
                String error = categoryService.validateActivate(category);
                if (error != null) {
                    session.setAttribute(CategoryListServlet.FLASH_ERROR, error);
                } else {
                    categoryService.activate(category, actorId, request.getRemoteAddr());
                    session.setAttribute(CategoryListServlet.FLASH_MESSAGE,
                            "Nhóm \"" + category.getName() + "\" đã hoạt động lại.");
                }
            }
            response.sendRedirect(request.getContextPath() + "/categories");
        } catch (SQLException e) {
            log("Không đổi được trạng thái nhóm hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
