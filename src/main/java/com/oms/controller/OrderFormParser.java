package com.oms.controller;

import com.oms.model.OrderForm;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

// Đọc form "Tạo đơn hàng" (S3-09): mỗi dòng hàng gửi 4 ô cùng tên (productId, productText, unitId, qty) theo thứ tự
// dòng. Bỏ khoảng trắng thừa, ô trống thành null; dòng để trống hoàn toàn thì bỏ qua.
final class OrderFormParser {

    private OrderFormParser() {
    }

    static OrderForm parse(HttpServletRequest request) {
        String[] productIds = values(request, "productId");
        String[] productTexts = values(request, "productText");
        String[] unitIds = values(request, "unitId");
        String[] qtys = values(request, "qty");
        int count = Math.max(Math.max(productIds.length, productTexts.length), Math.max(unitIds.length, qtys.length));
        List<OrderForm.Line> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            OrderForm.Line line = new OrderForm.Line(at(productIds, i), at(productTexts, i), at(unitIds, i),
                    at(qtys, i));
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return new OrderForm(normalize(request.getParameter("customerId")),
                normalize(request.getParameter("deliveryAddressId")), normalize(request.getParameter("requestedDate")),
                normalize(request.getParameter("note")), AccountFormParser.parseId(request.getParameter("draftId")),
                normalize(request.getParameter("version")), lines);
    }

    private static String[] values(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? new String[0] : values;
    }

    private static String at(String[] values, int index) {
        return index < values.length ? normalize(values[index]) : null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
