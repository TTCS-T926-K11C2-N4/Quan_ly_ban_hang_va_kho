package com.oms.security;

import com.oms.model.SessionUser;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Các phiên đang đăng nhập của từng người dùng, để thu hồi phiên khi đổi mật khẩu (S1-04),
// khi bị khóa (S1-10) và cập nhật quyền ngay khi quản trị viên đổi vai trò.
// Lưu trong bộ nhớ nên chỉ đúng khi chạy một Tomcat; khởi động lại Tomcat thì mọi người đăng nhập lại.
@WebListener
public class SessionRegistry implements HttpSessionListener {

    private static final Map<Long, Set<HttpSession>> SESSIONS = new ConcurrentHashMap<>();

    public static void register(long userId, HttpSession session) {
        SESSIONS.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    // keep: phiên được giữ lại (vd phiên vừa đổi mật khẩu); null để thu hồi hết
    public static void invalidateAll(long userId, HttpSession keep) {
        Set<HttpSession> sessions = SESSIONS.get(userId);
        if (sessions == null) {
            return;
        }
        for (HttpSession session : sessions) {
            if (session != keep) {
                sessions.remove(session);
                try {
                    session.invalidate();
                } catch (IllegalStateException e) {
                    // Phiên đã hết hạn hoặc đã đăng xuất trước đó
                }
            }
        }
    }

    public static void replaceUser(SessionUser user) {
        Set<HttpSession> sessions = SESSIONS.get(user.getId());
        if (sessions == null) {
            return;
        }
        for (HttpSession session : sessions) {
            try {
                session.setAttribute(SessionUser.SESSION_KEY, user);
            } catch (IllegalStateException e) {
                sessions.remove(session);
            }
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        SESSIONS.values().forEach(sessions -> sessions.remove(session));
    }
}
