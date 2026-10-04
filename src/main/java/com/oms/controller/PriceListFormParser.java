package com.oms.controller;

import com.oms.model.PriceListForm;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

// Đọc form bảng giá: các dòng giá gửi lên dạng mảng productId[], price[], floorPrice[] cùng thứ tự.
// Giữ cả dòng trống để báo lỗi: người dùng phải nhập đủ hoặc bấm × xoá dòng.
final class PriceListFormParser {

    private PriceListFormParser() {
    }

    static PriceListForm read(HttpServletRequest request) {
        String[] productIds = values(request, "productId");
        String[] prices = values(request, "price");
        String[] floors = values(request, "floorPrice");
        int count = Math.max(productIds.length, Math.max(prices.length, floors.length));
        List<PriceListForm.Line> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String productId = at(productIds, i);
            String price = at(prices, i);
            String floor = at(floors, i);
            lines.add(new PriceListForm.Line(AccountFormParser.parseId(productId), price, floor));
        }
        return new PriceListForm(AccountFormParser.parseId(request.getParameter("customerGroupId")),
                normalize(request.getParameter("name")), normalize(request.getParameter("validFrom")),
                normalize(request.getParameter("validTo")), lines);
    }

    private static String[] values(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? new String[0] : values;
    }

    private static String at(String[] values, int index) {
        return index < values.length ? normalize(values[index]) : null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
