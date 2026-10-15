package com.oms.controller;

import com.oms.model.PurchaseHistory;
import com.oms.service.PurchaseHistoryService;
import com.oms.util.DateTimeUtil;
import com.oms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// S4-04: lịch sử mua hàng 3 tháng gần nhất của đại lý (JSON) cho màn tạo đơn; chỉ đại lý trong phạm vi của người dùng
@WebServlet("/orders/history")
public class OrderHistoryServlet extends HttpServlet {

    private final PurchaseHistoryService historyService = new PurchaseHistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("customerId"));
        try {
            PurchaseHistory history = id == null ? null
                    : historyService.find(CurrentUser.get(request).getId(), id);
            if (history == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (PurchaseHistory.Item item : history.getItems()) {
                Map<String, Object> json = new LinkedHashMap<>();
                json.put("productId", item.getProductId());
                json.put("text", item.getSku() + " - " + item.getName());
                json.put("unitId", item.getUnitId());
                json.put("unitName", item.getUnitName());
                json.put("orders", item.getOrderCount());
                json.put("avgQty", item.getAvgQty().stripTrailingZeros().toPlainString());
                json.put("lastDate", DateTimeUtil.formatDay(item.getLastDate()));
                items.add(json);
            }
            List<Map<String, Object>> lastLines = new ArrayList<>();
            for (PurchaseHistory.Line line : history.getLastOrderLines()) {
                Map<String, Object> json = new LinkedHashMap<>();
                json.put("productId", line.getProductId());
                json.put("text", line.getSku() + " - " + line.getName());
                json.put("unitId", line.getUnitId());
                json.put("unitName", line.getUnitName());
                json.put("qty", line.getQty().stripTrailingZeros().toPlainString());
                lastLines.add(json);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("months", PurchaseHistoryService.MONTHS);
            data.put("items", items);
            data.put("lastOrderNo", history.getLastOrderNo());
            data.put("lastOrderDate", DateTimeUtil.formatDay(history.getLastOrderDate()));
            data.put("lastLines", lastLines);
            OrderJson.write(response, JsonUtil.object(data));
        } catch (SQLException e) {
            log("Không tải được lịch sử mua hàng của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
