package com.oms.controller;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Trả JSON cho orders.js; không cho trình duyệt/proxy lưu vì dữ liệu đổi theo đại lý và bảng giá
final class OrderJson {

    private OrderJson() {
    }

    static void write(HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(json);
    }
}
