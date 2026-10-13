package com.oms.controller;

import com.oms.service.StockLedgerService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

// S5-05: nút "Xuất danh sách" tải file Excel sổ tồn theo bộ lọc đang chọn
@WebServlet("/stock/export")
public class StockLedgerExportServlet extends HttpServlet {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StockLedgerService ledgerService = new StockLedgerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            ExcelDownload.send(response, "so-ton-kho-" + DateTimeUtil.today().format(FILE_DATE) + ".xlsx",
                    ledgerService.export(StockLedgerServlet.parseFilter(request)));
        } catch (SQLException e) {
            log("Không xuất được sổ tồn kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
