package com.oms.controller;

import com.oms.model.AuditCatalog;
import com.oms.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S2-04: nhật ký thao tác, lọc theo thời gian, người dùng, hành động, đối tượng; mới nhất trước
@WebServlet("/audit-logs")
public class AuditLogListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/audit/audit-log-list.jsp";

    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuditLogParams params = AuditLogParams.read(request);
        try {
            request.setAttribute("logPage", auditLogService.search(params.toFilter(), parsePage(request)));
            request.setAttribute("actors", auditLogService.getActors());
            request.setAttribute("nameLookupsJson", auditLogService.getNameLookupsJson());
        } catch (SQLException e) {
            log("Không tải được nhật ký thao tác", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        // Gán từng giá trị: EL của Tomcat 10.1 không đọc được thành phần của record
        request.setAttribute("fromDate", params.from());
        request.setAttribute("toDate", params.to());
        request.setAttribute("actorFilter", params.actorUserId());
        request.setAttribute("entityFilter", params.entityType());
        request.setAttribute("groupFilter", params.actionGroup());
        request.setAttribute("entities", AuditCatalog.entities());
        request.setAttribute("groups", AuditCatalog.groups());
        request.setAttribute("exportMaxRows", AuditLogService.EXPORT_MAX_ROWS);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static int parsePage(HttpServletRequest request) {
        try {
            String page = request.getParameter("page");
            return page == null ? 1 : Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
