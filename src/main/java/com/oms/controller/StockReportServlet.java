package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.StockReport;
import com.oms.service.StockReportService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

// S8-07: báo cáo tồn kho và vòng quay hàng (/reports/stock), xuất Excel (/reports/stock/export). Giá trị tồn dựa trên
// giá vốn nên chỉ hiện cho người xem được giá vốn hoặc Quản lý kho.
@WebServlet(urlPatterns = {"/reports/stock", "/reports/stock/export"})
public class StockReportServlet extends HttpServlet {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StockReportService reportService = new StockReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int window = parseDays(request.getParameter("window"), StockReportService.DEFAULT_WINDOW_DAYS, 365);
        int slow = parseDays(request.getParameter("slow"), StockReportService.DEFAULT_SLOW_DAYS, 3650);
        boolean showValue = CurrentUser.get(request).can(Permission.COST_PRICE_VIEW)
                || CurrentUser.get(request).can(Permission.STOCKTAKE_FINALIZE);
        try {
            StockReport report = reportService.report(window, slow, showValue);
            if (request.getServletPath().endsWith("/export")) {
                ExcelDownload.send(response, "bao-cao-ton-kho-" + DateTimeUtil.today().format(FILE_DATE) + ".xlsx",
                        reportService.export(report));
                return;
            }
            request.setAttribute("report", report);
            request.getRequestDispatcher("/WEB-INF/views/reports/stock-report.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được báo cáo tồn kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static int parseDays(String value, int fallback, int max) {
        try {
            int days = Integer.parseInt(value == null ? "" : value.trim());
            return days >= 1 && days <= max ? days : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
