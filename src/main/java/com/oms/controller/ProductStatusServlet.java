package com.oms.controller;

import com.oms.model.Product;
import com.oms.service.ProductService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S2-05: Ngừng kinh doanh / Kinh doanh lại. Tham số status=DISCONTINUED|ACTIVE.
@WebServlet("/products/status")
public class ProductStatusServlet extends HttpServlet {

    private final ProductService productService = new ProductService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        String status = request.getParameter("status");
        if (!Product.ACTIVE.equals(status) && !Product.DISCONTINUED.equals(status)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            Product product = id == null ? null : productService.find(id, false);
            if (product == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            productService.changeStatus(product, status, CurrentUser.get(request).getId(), request.getRemoteAddr());
            request.getSession().setAttribute(ProductListServlet.FLASH_MESSAGE, Product.DISCONTINUED.equals(status)
                    ? "Đã ngừng kinh doanh sản phẩm \"" + product.getName() + "\"."
                    : "Sản phẩm \"" + product.getName() + "\" đã kinh doanh lại.");
            response.sendRedirect(request.getContextPath() + "/products");
        } catch (SQLException e) {
            log("Không đổi được trạng thái sản phẩm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
