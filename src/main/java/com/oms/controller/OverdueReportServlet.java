package com.oms.controller;

import com.oms.model.OverdueReport;
import com.oms.service.InvoiceService;
import com.oms.service.OverdueReportService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

// S8-09: báo cáo công nợ quá hạn theo tuổi nợ và nhân viên phụ trách (/debts/overdue), xuất Excel (/debts/overdue/export)
@WebServlet(urlPatterns = {"/debts/overdue", "/debts/overdue/export"})
public class OverdueReportServlet extends HttpServlet {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OverdueReportService reportService = new OverdueReportService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            OverdueReport report = reportService.report();
            if (request.getServletPath().endsWith("/export")) {
                ExcelDownload.send(response, "cong-no-qua-han-" + DateTimeUtil.today().format(FILE_DATE) + ".xlsx",
                        reportService.export(report));
                return;
            }
            request.setAttribute("report", report);
            request.getRequestDispatcher("/WEB-INF/views/debts/overdue-report.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được báo cáo công nợ quá hạn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
