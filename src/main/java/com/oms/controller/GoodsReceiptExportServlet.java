package com.oms.controller;

import com.oms.service.GoodsReceiptService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

// S5-04: nút "Xuất danh sách" tải file Excel các phiếu nhập theo bộ lọc đang chọn
@WebServlet("/goods-receipts/export")
public class GoodsReceiptExportServlet extends HttpServlet {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final GoodsReceiptService receiptService = new GoodsReceiptService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            ExcelDownload.send(response, "phieu-nhap-kho-" + DateTimeUtil.today().format(FILE_DATE) + ".xlsx",
                    receiptService.export(GoodsReceiptFormParser.parseFilter(request)));
        } catch (SQLException e) {
            log("Không xuất được danh sách phiếu nhập", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
