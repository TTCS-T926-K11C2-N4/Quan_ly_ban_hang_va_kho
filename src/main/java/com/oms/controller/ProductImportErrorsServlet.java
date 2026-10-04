package com.oms.controller;

import com.oms.model.ProductImportBatch;
import com.oms.service.ProductImportService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

// S2-08: tải các dòng lỗi của lần nhập gần nhất để sửa rồi nhập lại
@WebServlet("/products/import/errors")
public class ProductImportErrorsServlet extends HttpServlet {

    private final ProductImportService importService = new ProductImportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        ProductImportBatch result = session == null ? null
                : (ProductImportBatch) session.getAttribute(ProductImportServlet.SESSION_RESULT);
        if (result == null) {
            response.sendRedirect(request.getContextPath() + "/products/import");
            return;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        importService.writeErrorReport(result.getErrorRows(), buffer);
        ExcelDownload.send(response, "bao-cao-loi-nhap-san-pham.xlsx", buffer);
    }
}
