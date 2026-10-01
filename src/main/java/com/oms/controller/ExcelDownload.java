package com.oms.controller;

import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

// Trả file .xlsx về trình duyệt dưới dạng tải xuống
final class ExcelDownload {

    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ExcelDownload() {
    }

    // fileName chỉ dùng ký tự ASCII để header Content-Disposition không cần mã hoá
    static void send(HttpServletResponse response, String fileName, ByteArrayOutputStream content) throws IOException {
        response.setContentType(CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setContentLength(content.size());
        content.writeTo(response.getOutputStream());
    }
}
