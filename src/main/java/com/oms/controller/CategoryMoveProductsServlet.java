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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// S2-06: chuyển các sản phẩm đã chọn từ nhóm categoryId sang nhóm targetCategoryId
@WebServlet("/categories/products/move")
public class CategoryMoveProductsServlet extends HttpServlet {

    private final ProductCategoryService categoryService = new ProductCategoryService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long categoryId = AccountFormParser.parseId(request.getParameter("categoryId"));
        String[] productValues = request.getParameterValues("productIds");
        List<Long> productIds = productValues == null ? List.of() : Arrays.stream(productValues)
                .map(AccountFormParser::parseId).filter(Objects::nonNull).distinct().toList();
        Long targetId = AccountFormParser.parseId(request.getParameter("targetCategoryId"));
        try {
            ProductCategory from = categoryId == null ? null : categoryService.find(categoryId);
            if (from == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            HttpSession session = request.getSession();
            Map<String, String> errors = categoryService.validateMove(from, productIds, targetId);
            if (!errors.isEmpty()) {
                session.setAttribute(CategoryListServlet.FLASH_ERROR, String.join(" ", errors.values()));
            } else {
                int moved = categoryService.moveProducts(from, productIds, targetId, CurrentUser.get(request).getId(),
                        request.getRemoteAddr());
                ProductCategory target = categoryService.find(targetId);
                session.setAttribute(CategoryListServlet.FLASH_MESSAGE,
                        "Đã chuyển " + moved + " sản phẩm sang nhóm \"" + target.getName() + "\".");
            }
            response.sendRedirect(request.getContextPath() + "/categories/products?id=" + from.getId());
        } catch (SQLException e) {
            log("Không chuyển được sản phẩm sang nhóm khác", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
