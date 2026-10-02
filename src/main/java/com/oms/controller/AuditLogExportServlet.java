package com.oms.controller;

import com.oms.service.AuditLogService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

// S2-04: xuất các dòng nhật ký đang lọc ra Excel (tối đa AuditLogService.EXPORT_MAX_ROWS dòng)
@WebServlet("/audit-logs/export")
public class AuditLogExportServlet extends HttpServlet {

    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuditLogParams params = AuditLogParams.read(request);
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        try {
            auditLogService.writeExcel(params.toFilter(), content);
        } catch (SQLException e) {
            log("Không xuất được nhật ký thao tác", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        ExcelDownload.send(response, "nhat-ky-thao-tac-" + LocalDate.now() + ".xlsx", content);
    }
}
