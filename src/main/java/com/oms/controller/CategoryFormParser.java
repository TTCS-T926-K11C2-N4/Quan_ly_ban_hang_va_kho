package com.oms.controller;

import com.oms.model.CategoryForm;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

// Đọc form thêm/sửa nhóm hàng; ô trống thành null, mã nhóm đổi sang chữ hoa
final class CategoryFormParser {

    private CategoryFormParser() {
    }

    static CategoryForm read(HttpServletRequest request) {
        String code = normalize(request.getParameter("code"));
        return new CategoryForm(
                code == null ? null : code.toUpperCase(Locale.ROOT),
                normalize(request.getParameter("name")),
                normalize(request.getParameter("description")),
                AccountFormParser.parseId(request.getParameter("parentId")),
                normalize(request.getParameter("sortOrder")));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
