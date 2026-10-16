package com.oms.controller;

import com.oms.service.WarehouseService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

// S5-03: nút "Xuất danh sách" tải file Excel các kho theo bộ lọc đang chọn; có tích chọn kho thì chỉ các kho đó
@WebServlet("/warehouses/export")
public class WarehouseExportServlet extends HttpServlet {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WarehouseService warehouseService = new WarehouseService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Set<Long> selectedIds = new HashSet<>();
        String[] ids = request.getParameterValues("id");
        if (ids != null) {
            for (String id : ids) {
                Long parsed = AccountFormParser.parseId(id);
                if (parsed != null) {
                    selectedIds.add(parsed);
                }
            }
        }
        try {
            ExcelDownload.send(response, "danh-sach-kho-" + DateTimeUtil.today().format(FILE_DATE) + ".xlsx",
                    warehouseService.export(WarehouseFormParser.parseFilter(request), selectedIds));
        } catch (SQLException e) {
            log("Không xuất được danh sách kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
