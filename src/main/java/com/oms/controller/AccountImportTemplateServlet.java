package com.oms.controller;

import com.oms.service.AccountImportService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

// S2-01: tải file mẫu nhập người dùng (kèm sheet hướng dẫn liệt kê mã vai trò, kho, địa bàn hiện có)
@WebServlet("/accounts/import/template")
public class AccountImportTemplateServlet extends HttpServlet {

    private final AccountImportService importService = new AccountImportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Ghi ra bộ nhớ trước để nếu lỗi CSDL vẫn còn trả được trang lỗi 500
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            importService.writeTemplate(buffer);
        } catch (SQLException e) {
            log("Không tạo được file mẫu nhập người dùng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        ExcelDownload.send(response, "mau-nhap-nguoi-dung.xlsx", buffer);
    }
}
