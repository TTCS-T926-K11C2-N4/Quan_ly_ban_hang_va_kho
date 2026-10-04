package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.ProductImportBatch;
import com.oms.service.ProductImportService;
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

// S2-08 Bước 1: tải file Excel sản phẩm lên, đọc và kiểm tra rồi chuyển sang bước xem trước
@WebServlet("/products/import")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = ProductImportServlet.MAX_FILE_BYTES,
        maxRequestSize = ProductImportServlet.MAX_FILE_BYTES + 1024 * 1024)
public class ProductImportServlet extends HttpServlet {

    static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    static final String VIEW = "/WEB-INF/views/products/import-upload.jsp";

    // Dòng đã đọc được, giữ trong session từ lúc xem trước tới lúc nhập; sau khi nhập chuyển sang SESSION_RESULT
    static final String SESSION_BATCH = "productImportBatch";
    static final String SESSION_RESULT = "productImportResult";

    private final ProductImportService importService = new ProductImportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showForm(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Part part;
        try {
            part = request.getPart("file");
        } catch (IllegalStateException e) {
            // Tomcat báo file vượt maxFileSize bằng IllegalStateException
            showForm(request, response, "File vượt quá dung lượng tối đa 10 MB.");
            return;
        }
        if (part == null || part.getSize() == 0) {
            showForm(request, response, "Vui lòng chọn file Excel.");
            return;
        }
        String fileName = baseName(part.getSubmittedFileName());
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            showForm(request, response, "Chỉ hỗ trợ file .xlsx hoặc .xls.");
            return;
        }

        ProductImportBatch batch;
        try (InputStream in = part.getInputStream()) {
            batch = importService.read(in, fileName, CurrentUser.get(request).can(Permission.COST_PRICE_VIEW));
        } catch (ProductImportService.InvalidFileException e) {
            showForm(request, response, e.getMessage());
            return;
        } catch (SQLException e) {
            log("Không kiểm tra được file nhập sản phẩm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.getSession().setAttribute(SESSION_BATCH, batch);
        request.getSession().removeAttribute(SESSION_RESULT);
        response.sendRedirect(request.getContextPath() + "/products/import/preview");
    }

    // Một số trình duyệt gửi cả đường dẫn trên máy (C:\Users\...\file.xlsx): chỉ giữ tên file
    private static String baseName(String submittedName) {
        if (submittedName == null) {
            return "";
        }
        String name = submittedName.substring(Math.max(submittedName.lastIndexOf('/'), submittedName.lastIndexOf('\\')) + 1);
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private static void showForm(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("maxRows", ProductImportService.MAX_ROWS);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
