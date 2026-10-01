package com.oms.controller;

import com.oms.model.CategoryForm;
import com.oms.model.ProductCategory;
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

// S2-06: sửa nhóm hàng; đổi nhóm cha = chuyển cả nhánh sang nhóm cha mới
@WebServlet("/categories/edit")
public class CategoryEditServlet extends HttpServlet {

    private final ProductCategoryService categoryService = new ProductCategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ProductCategory category = findCategory(request);
            if (category == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            CategoryForm form = new CategoryForm(category.getCode(), category.getName(), category.getDescription(),
                    category.getParentId(), String.valueOf(category.getSortOrder()));
            showForm(request, response, category, form, Map.of());
        } catch (SQLException e) {
            log("Không tải được nhóm hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ProductCategory category = findCategory(request);
            if (category == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            CategoryForm form = CategoryFormParser.read(request);
            Map<String, String> errors = categoryService.validate(form, category);
            if (!errors.isEmpty()) {
                showForm(request, response, category, form, errors);
                return;
            }
            try {
                categoryService.update(category, form, CurrentUser.get(request).getId(), request.getRemoteAddr());
            } catch (SQLIntegrityConstraintViolationException e) {
                showForm(request, response, category, form, categoryService.validate(form, category));
                return;
            }
            request.getSession().setAttribute(CategoryListServlet.FLASH_MESSAGE,
                    "Đã cập nhật nhóm hàng \"" + form.getName() + "\".");
            response.sendRedirect(request.getContextPath() + "/categories");
        } catch (SQLException e) {
            log("Không cập nhật được nhóm hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private ProductCategory findCategory(HttpServletRequest request) throws SQLException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        return id == null ? null : categoryService.find(id);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, ProductCategory category,
                          CategoryForm form, Map<String, String> errors)
            throws ServletException, IOException, SQLException {
        request.setAttribute("editing", true);
        request.setAttribute("category", category);
        request.setAttribute("parentOptions", categoryService.getParentOptions(category));
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("maxLevel", ProductCategoryService.MAX_LEVEL);
        request.getRequestDispatcher(CategoryCreateServlet.VIEW).forward(request, response);
    }
}
