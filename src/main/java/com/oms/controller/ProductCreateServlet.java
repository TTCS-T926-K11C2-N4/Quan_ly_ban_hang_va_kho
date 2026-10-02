package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.Product;
import com.oms.model.ProductForm;
import com.oms.service.ProductService;
import com.oms.util.AvatarImages;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;

// S2-05: thêm sản phẩm. Ô giá vốn chỉ hiện và chỉ được lưu khi người dùng có quyền COST_PRICE_VIEW.
@WebServlet("/products/new")
@MultipartConfig(maxFileSize = ProductService.IMAGE_MAX_BYTES,
        maxRequestSize = ProductService.IMAGE_MAX_BYTES + 64 * 1024)
public class ProductCreateServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/products/product-form.jsp";

    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ProductForm form = new ProductForm(null, null,
                AccountFormParser.parseId(request.getParameter("categoryId")), null, null, null, Product.ACTIVE,
                null, null);
        try {
            showForm(request, response, form, Map.of());
        } catch (SQLException e) {
            log("Không tải được form thêm sản phẩm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean canEditCost = CurrentUser.get(request).can(Permission.COST_PRICE_VIEW);
        Map<String, String> errors = new LinkedHashMap<>();
        ProductService.ProductImage image = null;
        try {
            image = productService.prepareImage(ProductFormParser.readImage(request),
                    ProductFormParser.imageName(request));
        } catch (AvatarImages.InvalidImageException e) {
            errors.put("image", e.getMessage());
        }
        ProductForm form = ProductFormParser.read(request, canEditCost);
        try {
            errors.putAll(productService.validate(form, null, canEditCost));
            if (!errors.isEmpty()) {
                showForm(request, response, form, errors);
                return;
            }
            try {
                productService.create(form, image, canEditCost, CurrentUser.get(request).getId(),
                        request.getRemoteAddr());
            } catch (SQLIntegrityConstraintViolationException e) {
                // Người khác vừa tạo trùng SKU sau bước kiểm tra
                showForm(request, response, form, productService.validate(form, null, canEditCost));
                return;
            }
            request.getSession().setAttribute(ProductListServlet.FLASH_MESSAGE,
                    "Đã thêm sản phẩm \"" + form.getName() + "\" (" + form.getSku() + ").");
            response.sendRedirect(request.getContextPath() + "/products");
        } catch (SQLException e) {
            log("Không thêm được sản phẩm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, ProductForm form,
                          Map<String, String> errors) throws ServletException, IOException, SQLException {
        request.setAttribute("editing", false);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("categoryOptions", productService.getCategoryOptions(null));
        request.setAttribute("units", productService.getUnits());
        request.setAttribute("canEditCost", CurrentUser.get(request).can(Permission.COST_PRICE_VIEW));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
