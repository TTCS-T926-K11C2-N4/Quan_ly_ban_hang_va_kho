package com.oms.controller;

import jakarta.servlet.http.HttpServletRequest;

// Địa chỉ tuyệt đối của ứng dụng để đặt vào email
final class AppUrl {

    private AppUrl() {
    }

    // Ưu tiên APP_BASE_URL (vd http://localhost:8080/du-an-ttcs) vì header Host do trình duyệt gửi lên
    // có thể bị giả mạo để liên kết trong email trỏ sang trang của kẻ tấn công.
    static String of(HttpServletRequest request, String path) {
        String baseUrl = System.getenv("APP_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            String url = request.getRequestURL().toString();
            baseUrl = url.substring(0, url.length() - request.getServletPath().length());
        }
        return baseUrl.replaceAll("/+$", "") + path;
    }
}
