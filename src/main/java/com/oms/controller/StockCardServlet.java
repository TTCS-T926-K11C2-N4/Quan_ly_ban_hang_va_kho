package com.oms.controller;

import com.oms.dao.WarehouseDao;
import com.oms.model.StockCardFilter;
import com.oms.service.StockCardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// S6-02: thẻ kho của một sản phẩm: mọi phát sinh theo thứ tự thời gian, tồn sau phát sinh, liên kết chứng từ gốc;
// lọc theo kho và khoảng ngày
@WebServlet("/stock/card")
public class StockCardServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/stock/stock-card.jsp";
    // Loại chứng từ gốc -> lớp Servlet màn xem; chỉ làm liên kết khi ứng dụng đã có màn đó
    private static final Map<String, String> VIEWER_SERVLETS = Map.of(
            "GOODS_RECEIPT", "com.oms.controller.GoodsReceiptViewServlet",
            "DELIVERY_NOTE", "com.oms.controller.DeliveryNoteViewServlet",
            "STOCK_TRANSFER", "com.oms.controller.TransferViewServlet",
            "STOCKTAKE", "com.oms.controller.StocktakeViewServlet");

    private final StockCardService cardService = new StockCardService();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long productId = AccountFormParser.parseId(request.getParameter("product"));
        StockCardFilter filter = new StockCardFilter(productId,
                AccountFormParser.parseId(request.getParameter("warehouse")),
                parseDate(request.getParameter("from")), parseDate(request.getParameter("to")));
        try {
            request.setAttribute("products", cardService.getProducts());
            request.setAttribute("warehouses", warehouseDao.findActive());
            if (productId != null) {
                String unit = cardService.baseUnitName(productId);
                if (unit == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("entryPage", cardService.search(filter, parsePage(request)));
                request.setAttribute("currentOnHand", cardService.currentOnHand(filter));
                request.setAttribute("baseUnit", unit);
            }
        } catch (SQLException e) {
            log("Không tải được thẻ kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Set<String> linkable = new HashSet<>();
        VIEWER_SERVLETS.forEach((type, servlet) -> {
            if (getServletContext().getServletRegistrations().containsKey(servlet)) {
                linkable.add(type);
            }
        });
        request.setAttribute("linkableTypes", linkable);
        request.setAttribute("filter", filter);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // null nếu để trống hoặc sai định dạng yyyy-MM-dd
    static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
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
