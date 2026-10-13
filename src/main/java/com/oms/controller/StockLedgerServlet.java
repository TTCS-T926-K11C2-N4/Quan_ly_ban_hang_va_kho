package com.oms.controller;

import com.oms.dao.WarehouseDao;
import com.oms.model.StockLedgerFilter;
import com.oms.model.StockLevel;
import com.oms.service.StockLedgerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-05: sổ tồn kho theo SKU và kho, ba thẻ số liệu, tìm theo SKU / tên, lọc kho, nhóm hàng, trạng thái tồn
// (chú giải Hết hàng / Sắp hết / Còn hàng bấm được để lọc)
@WebServlet("/stock")
public class StockLedgerServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/stock/stock-ledger.jsp";

    private final StockLedgerService ledgerService = new StockLedgerService();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StockLedgerFilter filter = parseFilter(request);
        try {
            request.setAttribute("summary", ledgerService.summary(filter));
            request.setAttribute("stockPage", ledgerService.search(filter, parsePage(request)));
            request.setAttribute("warehouses", warehouseDao.findActive());
            request.setAttribute("categories", ledgerService.getCategories());
        } catch (SQLException e) {
            log("Không tải được sổ tồn kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("filter", filter);
        request.setAttribute("levels", StockLevel.values());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    static StockLedgerFilter parseFilter(HttpServletRequest request) {
        String keyword = request.getParameter("keyword");
        return new StockLedgerFilter(keyword == null ? null : keyword.trim(),
                AccountFormParser.parseId(request.getParameter("warehouse")),
                AccountFormParser.parseId(request.getParameter("category")),
                StockLevel.fromCode(request.getParameter("level")));
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
