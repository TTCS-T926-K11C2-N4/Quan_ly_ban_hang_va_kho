package com.oms.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// Thông báo một lần sau redirect (Post/Redirect/Get): Servlet POST đặt vào session, trang đích lấy ra rồi xóa
final class Flash {

    private Flash() {
    }

    static void moveToRequest(HttpServletRequest request, String sessionKey, String requestKey) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(sessionKey) != null) {
            request.setAttribute(requestKey, session.getAttribute(sessionKey));
            session.removeAttribute(sessionKey);
        }
    }
}
