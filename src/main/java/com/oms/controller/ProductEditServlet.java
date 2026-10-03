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

// S2-05: sửa sản phẩm. Người khác sửa cùng lúc thì không ghi đè (khoá lạc quan theo products.version).
@WebServlet("/products/edit")
@MultipartConfig(maxFileSize = ProductService.IMAGE_MAX_BYTES,
        maxRequestSize = ProductService.IMAGE_MAX_BYTES + 64 * 1024)
public class ProductEditServlet extends HttpServlet {

    private static final String FLASH_CONFLICT = "productEditConflict";

    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean canEditCost = CurrentUser.get(request).can(Permission.COST_PRICE_VIEW);
        try {
            Product product = findProduct(request, canEditCost);
            if (product == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            ProductForm form = new ProductForm(product.getSku(), product.getName(), product.getCategoryId(),
                    product.getBaseUnitId(), product.getPackagingSpec(),
                    product.getCostPrice() == null ? null : product.getCostPrice().toBigInteger().toString(),
                    product.getStatus(), product.getDescription(), product.getVersion(),
                    productService.getConversions(product.getId()).stream()
                            .map(c -> new ProductForm.Conversion(c.getUnitId(), c.getFactorText())).toList());
            Flash.moveToRequest(request, FLASH_CONFLICT, "formError");
            showForm(request, response, product, form, Map.of());
        } catch (SQLException e) {
            log("Không tải được sản phẩm", e);
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
        try {
            Product product = findProduct(request, canEditCost);
            if (product == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            ProductForm form = ProductFormParser.read(request, canEditCost);
            if (form.getVersion() == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            errors.putAll(productService.validate(form, product, canEditCost));
            if (!errors.isEmpty()) {
                showForm(request, response, product, form, errors);
                return;
            }
            boolean saved;
            try {
                saved = productService.update(product, form, image, canEditCost, CurrentUser.get(request).getId(),
                        request.getRemoteAddr());
            } catch (SQLIntegrityConstraintViolationException e) {
                showForm(request, response, product, form, productService.validate(form, product, canEditCost));
                return;
            }
            if (!saved) {
                request.getSession().setAttribute(FLASH_CONFLICT, "Sản phẩm vừa được người khác cập nhật nên "
                        + "thay đổi của bạn chưa được lưu. Đây là dữ liệu mới nhất, hãy sửa lại nếu cần.");
                response.sendRedirect(request.getContextPath() + "/products/edit?id=" + product.getId());
                return;
            }
            request.getSession().setAttribute(ProductListServlet.FLASH_MESSAGE,
                    "Đã cập nhật sản phẩm \"" + form.getName() + "\".");
            response.sendRedirect(request.getContextPath() + "/products");
        } catch (SQLException e) {
            log("Không cập nhật được sản phẩm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Product findProduct(HttpServletRequest request, boolean canEditCost) throws SQLException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        return id == null ? null : productService.find(id, canEditCost);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Product product,
                          ProductForm form, Map<String, String> errors)
            throws ServletException, IOException, SQLException {
        request.setAttribute("editing", true);
        request.setAttribute("product", product);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("hasTransactions", productService.hasTransactions(product.getId()));
        request.setAttribute("categoryOptions", productService.getCategoryOptions(product.getCategoryId()));
        request.setAttribute("units", productService.getUnits());
        request.setAttribute("canEditCost", CurrentUser.get(request).can(Permission.COST_PRICE_VIEW));
        request.getRequestDispatcher(ProductCreateServlet.VIEW).forward(request, response);
    }
}
