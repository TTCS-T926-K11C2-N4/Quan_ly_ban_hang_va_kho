package com.oms.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Dựng lại "keyword=...&page=..." từ các tham số đang có của request (đã mã hoá URL), để sau khi lưu ở form POST
// quay về đúng danh sách đang lọc. Tham số trống bị bỏ qua.
final class QueryString {

    private QueryString() {
    }

    static String of(HttpServletRequest request, List<String> names) {
        StringBuilder query = new StringBuilder();
        for (String name : names) {
            String value = request.getParameter(name);
            if (value != null && !value.isBlank()) {
                query.append(query.length() == 0 ? "" : "&").append(name).append('=')
                        .append(URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
            }
        }
        return query.toString();
    }
}
