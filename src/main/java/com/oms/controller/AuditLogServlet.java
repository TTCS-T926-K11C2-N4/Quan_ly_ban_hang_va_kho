package com.oms.controller;

import com.oms.model.AuditEntityType;
import com.oms.model.AuditLogFilter;
import com.oms.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

// Trang Nhật ký thao tác (S2-04), chỉ Quản trị hệ thống (quyền AUDIT_LOG_VIEW).
// Tham số lọc: actorUserId, entityType (mã AuditEntityType), fromDate, toDate (yyyy-MM-dd), page.
@WebServlet("/audit-logs")
public class AuditLogServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/audit-logs/audit-log-list.jsp";

    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        LocalDate fromDate = parseDate(request.getParameter("fromDate"), "fromDate", errors);
        LocalDate toDate = parseDate(request.getParameter("toDate"), "toDate", errors);
        AuditLogFilter filter = new AuditLogFilter(AccountFormParser.parseId(request.getParameter("actorUserId")),
                AuditEntityType.fromCode(request.getParameter("entityType")), fromDate, toDate);
        errors.putAll(auditLogService.validate(filter));

        try {
            // Bộ lọc sai thì vẫn hiện trang (kèm lỗi) nhưng không lọc theo ngày
            AuditLogFilter effective = errors.isEmpty() ? filter
                    : new AuditLogFilter(filter.getActorUserId(), filter.getEntityType(), null, null);
            request.setAttribute("logPage", auditLogService.search(effective, parsePage(request.getParameter("page"))));
            request.setAttribute("actors", auditLogService.getActors());
        } catch (SQLException e) {
            log("Không tải được nhật ký thao tác", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        request.setAttribute("filter", filter);
        request.setAttribute("entityTypes", AuditEntityType.values());
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static LocalDate parseDate(String value, String field, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            errors.put(field, "Ngày không hợp lệ.");
            return null;
        }
    }

    private static int parsePage(String value) {
        try {
            return value == null ? 1 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
