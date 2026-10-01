package com.oms.controller;

import com.oms.model.ImportBatch;
import com.oms.service.AccountImportService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

// S2-01: tải các dòng lỗi của lần nhập gần nhất để sửa rồi nhập lại
@WebServlet("/accounts/import/errors")
public class AccountImportErrorsServlet extends HttpServlet {

    private final AccountImportService importService = new AccountImportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        ImportBatch result = session == null ? null : (ImportBatch) session.getAttribute(AccountImportServlet.SESSION_RESULT);
        if (result == null) {
            response.sendRedirect(request.getContextPath() + "/accounts/import");
            return;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        importService.writeErrorReport(result.getErrorRows(), buffer);
        ExcelDownload.send(response, "bao-cao-loi-nhap-nguoi-dung.xlsx", buffer);
    }
}
