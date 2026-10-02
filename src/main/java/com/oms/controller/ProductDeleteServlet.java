package com.oms.controller;

import com.oms.model.Product;
import com.oms.service.ProductService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

// S2-05: xoá sản phẩm chưa phát sinh giao dịch; đã phát sinh thì báo chuyển sang Ngừng kinh doanh
@WebServlet("/products/delete")
public class ProductDeleteServlet extends HttpServlet {

    private final ProductService productService = new ProductService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            Product product = id == null ? null : productService.find(id, false);
            if (product == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            HttpSession session = request.getSession();
            String error = productService.validateDelete(product);
            if (error != null) {
                session.setAttribute(ProductListServlet.FLASH_ERROR, error);
            } else {
                try {
                    productService.delete(product, CurrentUser.get(request).getId(), request.getRemoteAddr());
                    session.setAttribute(ProductListServlet.FLASH_MESSAGE,
                            "Đã xoá sản phẩm \"" + product.getName() + "\".");
                } catch (SQLIntegrityConstraintViolationException e) {
                    session.setAttribute(ProductListServlet.FLASH_ERROR, "Sản phẩm \"" + product.getName()
                            + "\" đang có trong bảng giá hoặc chính sách chiết khấu nên không xoá được. "
                            + "Hãy chuyển sang Ngừng kinh doanh.");
                }
            }
            response.sendRedirect(request.getContextPath() + "/products");
        } catch (SQLException e) {
            log("Không xoá được sản phẩm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
