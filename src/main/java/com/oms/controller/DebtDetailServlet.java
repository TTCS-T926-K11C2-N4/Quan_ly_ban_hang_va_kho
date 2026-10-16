package com.oms.controller;

import com.oms.model.DebtDetail;
import com.oms.service.DebtDetailService;
import com.oms.service.InvoiceService;
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

// S8-10: sổ chi tiết công nợ một đại lý theo khoảng thời gian: xem (/debts/detail), xuất Excel (/debts/detail/export)
// và trang in để lưu PDF (/debts/detail/print)
@WebServlet(urlPatterns = {"/debts/detail", "/debts/detail/export", "/debts/detail/print"})
public class DebtDetailServlet extends HttpServlet {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DebtDetailService detailService = new DebtDetailService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long customerId = AccountFormParser.parseId(request.getParameter("customer"));
        LocalDate today = DateTimeUtil.today();
        LocalDate to = parseDate(request.getParameter("to"), today);
        LocalDate from = parseDate(request.getParameter("from"), to.withDayOfMonth(1));
        if (from.isAfter(to)) {
            from = to;
        }
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String path = request.getServletPath();
            if (customerId == null) {
                if (path.equals("/debts/detail")) {
                    request.setAttribute("customers", detailService.customers());
                    request.setAttribute("from", from.toString());
                    request.setAttribute("to", to.toString());
                    request.getRequestDispatcher("/WEB-INF/views/debts/debt-detail.jsp").forward(request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            DebtDetail detail = detailService.find(customerId, from, to);
            if (detail == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (path.endsWith("/export")) {
                ExcelDownload.send(response, "so-chi-tiet-cong-no-" + detail.getCustomerCode() + "-"
                        + from.format(FILE_DATE) + "-" + to.format(FILE_DATE) + ".xlsx", detailService.export(detail));
                return;
            }
            request.setAttribute("detail", detail);
            request.setAttribute("from", from.toString());
            request.setAttribute("to", to.toString());
            if (path.endsWith("/print")) {
                request.getRequestDispatcher("/WEB-INF/views/debts/debt-detail-print.jsp").forward(request, response);
                return;
            }
            request.setAttribute("customers", detailService.customers());
            request.setAttribute("customerId", customerId);
            request.getRequestDispatcher("/WEB-INF/views/debts/debt-detail.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được sổ chi tiết công nợ", e);
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
