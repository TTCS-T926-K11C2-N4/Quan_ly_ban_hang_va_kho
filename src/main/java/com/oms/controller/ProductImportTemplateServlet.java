package com.oms.controller;

import com.oms.model.Permission;
import com.oms.service.ProductImportService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

// S2-08: tải file mẫu nhập sản phẩm (kèm sheet hướng dẫn liệt kê mã nhóm hàng, đơn vị tính hiện có)
@WebServlet("/products/import/template")
public class ProductImportTemplateServlet extends HttpServlet {

    private final ProductImportService importService = new ProductImportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Ghi ra bộ nhớ trước để nếu lỗi CSDL vẫn còn trả được trang lỗi 500
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            importService.writeTemplate(buffer, CurrentUser.get(request).can(Permission.COST_PRICE_VIEW));
        } catch (SQLException e) {
            log("Không tạo được file mẫu nhập sản phẩm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        ExcelDownload.send(response, "mau-nhap-san-pham.xlsx", buffer);
    }
}
