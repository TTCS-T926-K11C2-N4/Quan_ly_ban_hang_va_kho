package com.oms.controller;

import com.oms.dao.PermissionDao;
import com.oms.model.Permission;
import com.oms.model.SalesReport;
import com.oms.service.SalesReportService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

// S8-06: báo cáo doanh số nhiều chiều (/reports/sales), xuất Excel (/reports/sales/export). Nhân viên kinh doanh chỉ
// thấy đại lý mình phụ trách; biên lợi nhuận chỉ hiện cho người xem được giá vốn.
@WebServlet(urlPatterns = {"/reports/sales", "/reports/sales/export"})
public class SalesReportServlet extends HttpServlet {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SalesReportService reportService = new SalesReportService();
    private final PermissionDao permissionDao = new PermissionDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate today = DateTimeUtil.today();
        LocalDate to = parseDate(request.getParameter("to"), today);
        LocalDate from = parseDate(request.getParameter("from"), to.withDayOfMonth(1));
        if (from.isAfter(to)) {
            from = to;
        }
        String dimension = request.getParameter("dim");
        long userId = CurrentUser.get(request).getId();
        boolean showMargin = CurrentUser.get(request).can(Permission.COST_PRICE_VIEW);
        try {
            String scope = permissionDao.findScope(userId, Permission.REPORT_VIEW);
            Long salesRepId = "ALL".equals(scope) ? null : userId;
            SalesReport report = reportService.report(dimension, from, to, showMargin, salesRepId);
            if (request.getServletPath().endsWith("/export")) {
                ExcelDownload.send(response, "bao-cao-doanh-so-" + report.getDimension().toLowerCase() + "-"
                        + from.format(FILE_DATE) + "-" + to.format(FILE_DATE) + ".xlsx", reportService.export(report));
                return;
            }
            request.setAttribute("report", report);
            request.setAttribute("dimensions", SalesReport.DIMENSIONS);
            request.setAttribute("from", from.toString());
            request.setAttribute("to", to.toString());
            request.getRequestDispatcher("/WEB-INF/views/reports/sales-report.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được báo cáo doanh số", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
