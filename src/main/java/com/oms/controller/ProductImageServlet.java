package com.oms.controller;

import com.oms.service.ProductService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

// S2-05: ảnh sản phẩm, ?id=&size=thumb|full; chưa có ảnh thì chuyển sang ảnh mặc định
@WebServlet("/products/image")
public class ProductImageServlet extends HttpServlet {

    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        if (id == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            Path path = productService.findImagePath(id, !"full".equals(request.getParameter("size")));
            if (path == null) {
                response.sendRedirect(request.getContextPath() + "/assets/img/product-placeholder.svg");
                return;
            }
            // EncodingFilter đặt UTF-8 cho mọi response; ảnh không có charset nên bỏ đi
            response.setCharacterEncoding((String) null);
            response.setContentType("image/png");
            response.setContentLengthLong(Files.size(path));
            // Đường dẫn ảnh kèm v=<id ảnh> nên ảnh mới có đường dẫn mới, được lưu đệm lâu
            response.setHeader("Cache-Control", "private, max-age=86400");
            Files.copy(path, response.getOutputStream());
        } catch (SQLException e) {
            log("Không đọc được ảnh sản phẩm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
