package com.oms.controller;

import com.oms.model.ImportBatch;
import com.oms.service.AccountImportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Locale;

// S2-01 Bước 1: tải file Excel lên, đọc và kiểm tra rồi chuyển sang bước xem trước
@WebServlet("/accounts/import")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = AccountImportServlet.MAX_FILE_BYTES,
        maxRequestSize = AccountImportServlet.MAX_FILE_BYTES + 1024 * 1024)
public class AccountImportServlet extends HttpServlet {

    static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    static final String VIEW = "/WEB-INF/views/accounts/import-upload.jsp";

    // Dòng đã đọc được, giữ trong session từ lúc xem trước tới lúc nhập; sau khi nhập chuyển sang SESSION_RESULT
    static final String SESSION_BATCH = "accountImportBatch";
    static final String SESSION_RESULT = "accountImportResult";

    private final AccountImportService importService = new AccountImportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Part part;
        try {
            part = request.getPart("file");
        } catch (IllegalStateException e) {
            // Tomcat báo file vượt maxFileSize bằng IllegalStateException
            showError(request, response, "File vượt quá dung lượng tối đa 10 MB.");
            return;
        }
        if (part == null || part.getSize() == 0) {
            showError(request, response, "Vui lòng chọn file Excel.");
            return;
        }
        String fileName = baseName(part.getSubmittedFileName());
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            showError(request, response, "Chỉ hỗ trợ file .xlsx hoặc .xls.");
            return;
        }

        ImportBatch batch;
        try (InputStream in = part.getInputStream()) {
            batch = importService.read(in, fileName);
        } catch (AccountImportService.InvalidFileException e) {
            showError(request, response, e.getMessage());
            return;
        } catch (SQLException e) {
            log("Không kiểm tra được file nhập người dùng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.getSession().setAttribute(SESSION_BATCH, batch);
        request.getSession().removeAttribute(SESSION_RESULT);
        response.sendRedirect(request.getContextPath() + "/accounts/import/preview");
    }

    // Một số trình duyệt gửi cả đường dẫn trên máy (C:\Users\...\file.xlsx): chỉ giữ tên file
    private static String baseName(String submittedName) {
        if (submittedName == null) {
            return "";
        }
        String name = submittedName.substring(Math.max(submittedName.lastIndexOf('/'), submittedName.lastIndexOf('\\')) + 1);
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private static void showError(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
