package com.oms.controller;

import com.oms.model.CategoryForm;
import com.oms.service.ProductCategoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

// S2-06: thêm nhóm hàng; ?parentId= để thêm nhóm con của một nhóm có sẵn
@WebServlet("/categories/new")
public class CategoryCreateServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/categories/category-form.jsp";

    private final ProductCategoryService categoryService = new ProductCategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long parentId = AccountFormParser.parseId(request.getParameter("parentId"));
        showForm(request, response, new CategoryForm(null, null, null, parentId, null), Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CategoryForm form = CategoryFormParser.read(request);
        try {
            Map<String, String> errors = categoryService.validate(form, null);
            if (!errors.isEmpty()) {
                showForm(request, response, form, errors);
                return;
            }
            try {
                categoryService.create(form, CurrentUser.get(request).getId(), request.getRemoteAddr());
            } catch (SQLIntegrityConstraintViolationException e) {
                // Người khác vừa tạo trùng mã giữa lúc kiểm tra và lúc lưu
                showForm(request, response, form, categoryService.validate(form, null));
                return;
            }
            request.getSession().setAttribute(CategoryListServlet.FLASH_MESSAGE,
                    "Đã thêm nhóm hàng \"" + form.getName() + "\".");
            response.sendRedirect(request.getContextPath() + "/categories");
        } catch (SQLException e) {
            log("Không thêm được nhóm hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, CategoryForm form,
                          Map<String, String> errors) throws ServletException, IOException {
        try {
            request.setAttribute("parentOptions", categoryService.getParentOptions(null));
        } catch (SQLException e) {
            log("Không tải được danh sách nhóm cha", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("maxLevel", ProductCategoryService.MAX_LEVEL);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
