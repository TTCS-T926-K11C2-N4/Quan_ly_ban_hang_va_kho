package com.oms.controller;

import com.oms.model.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// Người đang đăng nhập. AuthFilter đã chặn request chưa đăng nhập nên Servlet được bảo vệ luôn có giá trị.
final class CurrentUser {

    private CurrentUser() {
    }

    static SessionUser get(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
    }
}
