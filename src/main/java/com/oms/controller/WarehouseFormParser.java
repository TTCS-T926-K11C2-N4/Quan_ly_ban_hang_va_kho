package com.oms.controller;

import com.oms.model.WarehouseFilter;
import com.oms.model.WarehouseForm;
import com.oms.model.WarehouseLocationForm;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

// Đọc form và bộ lọc của màn kho, vị trí (S5-03): bỏ khoảng trắng thừa, ô trống thành null, mã viết hoa
final class WarehouseFormParser {

    private WarehouseFormParser() {
    }

    static WarehouseForm parse(HttpServletRequest request) {
        return new WarehouseForm(upper(request.getParameter("code")), normalize(request.getParameter("name")),
                normalize(request.getParameter("description")), normalize(request.getParameter("address")),
                normalize(request.getParameter("managerUserId")), normalize(request.getParameter("type")),
                normalize(request.getParameter("status")));
    }

    static WarehouseLocationForm parseLocation(HttpServletRequest request) {
        return new WarehouseLocationForm(normalize(request.getParameter("locationId")),
                normalize(request.getParameter("type")), normalize(request.getParameter("parentId")),
                upper(request.getParameter("code")), normalize(request.getParameter("name")),
                normalize(request.getParameter("pickSequence")));
    }

    static WarehouseFilter parseFilter(HttpServletRequest request) {
        String keyword = normalize(request.getParameter("keyword"));
        return new WarehouseFilter(keyword, normalize(request.getParameter("type")),
                normalize(request.getParameter("status")));
    }

    static int parsePage(HttpServletRequest request) {
        try {
            String page = request.getParameter("page");
            return page == null ? 1 : Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String upper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
