package com.oms.controller;

import com.oms.service.DebtLedgerService;
import com.oms.service.InvoiceService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

// S7-03: tải file Excel sổ công nợ theo bộ lọc đang chọn
@WebServlet("/debts/export")
public class DebtLedgerExportServlet extends HttpServlet {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DebtLedgerService debtLedgerService = new DebtLedgerService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            ExcelDownload.send(response, "so-cong-no-" + DateTimeUtil.today().format(FILE_DATE) + ".xlsx",
                    debtLedgerService.export("1".equals(request.getParameter("overdue"))));
        } catch (SQLException e) {
            log("Không xuất được sổ công nợ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
