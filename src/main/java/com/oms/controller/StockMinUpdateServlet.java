package com.oms.controller;

import com.oms.service.MinStockService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

// S5-09: đặt tồn tối thiểu của một SKU tại một kho từ sổ tồn, rồi quay lại đúng trang / bộ lọc đang xem
@WebServlet("/stock/min-stock")
public class StockMinUpdateServlet extends HttpServlet {

    static final String FLASH_MESSAGE = "stockFlashMessage";
    static final String FLASH_ERROR = "stockFlashError";

    private final MinStockService minStockService = new MinStockService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouse"));
        Long productId = AccountFormParser.parseId(request.getParameter("product"));
        if (warehouseId == null || productId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            MinStockService.Result result = minStockService.update(CurrentUser.get(request).getId(), warehouseId,
                    productId, request.getParameter("minQty"), request.getRemoteAddr());
            if ("NOT_FOUND".equals(result.getError())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (result.getError() != null) {
                request.getSession().setAttribute(FLASH_ERROR, result.getError());
            } else {
                request.getSession().setAttribute(FLASH_MESSAGE, "Đã đặt tồn tối thiểu của " + result.getSku()
                        + " tại kho " + result.getWarehouseCode() + " là "
                        + result.getMinStock().stripTrailingZeros().toPlainString()
                        + (result.getMinStock().signum() == 0 ? " (không cảnh báo)." : " (đơn vị cơ sở)."));
            }
            response.sendRedirect(request.getContextPath() + "/stock" + returnQuery(request));
        } catch (SQLException e) {
            log("Không đặt được tồn tối thiểu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Giữ lại bộ lọc và trang người dùng đang xem (các ô ẩn của form)
    private static String returnQuery(HttpServletRequest request) {
        StringBuilder query = new StringBuilder();
        for (String name : new String[] {"keyword", "warehouseFilter", "category", "level", "page"}) {
            String value = request.getParameter("r_" + name);
            if (value != null && !value.isBlank()) {
                String key = "warehouseFilter".equals(name) ? "warehouse" : name;
                query.append(query.length() == 0 ? '?' : '&').append(key).append('=')
                        .append(URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
            }
        }
        return query.toString();
    }
}
