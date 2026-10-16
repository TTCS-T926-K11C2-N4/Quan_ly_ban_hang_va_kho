package com.oms.controller;

import com.oms.service.LotService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-01: /lots/track bật / tắt quản lý lô theo SKU (INVENTORY_MANAGE); /lots/override cho phép hoặc thu hồi xuất lô
// quá hạn kèm lý do và /lots/threshold đặt ngưỡng cảnh báo (WAREHOUSE_MANAGE: Quản lý kho). Quyền khai báo ở AccessRules.
@WebServlet({"/lots/track", "/lots/override", "/lots/threshold"})
public class LotActionServlet extends HttpServlet {

    private final LotService lotService = new LotService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        String ip = request.getRemoteAddr();
        try {
            LotService.Outcome outcome;
            switch (request.getServletPath()) {
                case "/lots/track" -> {
                    Long product = AccountFormParser.parseId(request.getParameter("product"));
                    if (product == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    outcome = lotService.setTracked(userId, product, "on".equals(request.getParameter("mode")), ip);
                }
                case "/lots/override" -> {
                    Long lot = AccountFormParser.parseId(request.getParameter("lot"));
                    if (lot == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    outcome = lotService.setExpiredIssue(userId, lot, "allow".equals(request.getParameter("mode")),
                            request.getParameter("reason"), ip);
                }
                default -> outcome = lotService.saveNearExpiryDays(userId, request.getParameter("days"), ip);
            }
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.getSession().setAttribute(
                    outcome.getError() != null ? LotListServlet.FLASH_ERROR : LotListServlet.FLASH_MESSAGE,
                    outcome.getError() != null ? outcome.getError() : outcome.getMessage());
            response.sendRedirect(request.getContextPath() + "/lots" + returnQuery(request));
        } catch (SQLException e) {
            log("Không xử lý được thao tác lô", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Quay lại đúng bộ lọc đang xem (các ô ẩn r_*)
    private static String returnQuery(HttpServletRequest request) {
        StringBuilder query = new StringBuilder();
        for (String name : new String[] {"keyword", "warehouse", "status", "page", "sku"}) {
            String value = request.getParameter("r_" + name);
            if (value != null && !value.isBlank()) {
                query.append(query.length() == 0 ? '?' : '&').append(name).append('=')
                        .append(java.net.URLEncoder.encode(value.trim(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return query.toString();
    }
}
