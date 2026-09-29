package com.oms.controller;

import com.oms.model.LoginResult;
import com.oms.model.SessionUser;
import com.oms.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/auth/login.jsp";
    private static final int IDENTIFIER_MAX_LENGTH = 150;

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser current = CurrentUser.get(request);
        if (current != null) {
            response.sendRedirect(request.getContextPath() + current.getHomePath());
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String identifier = request.getParameter("username");
        identifier = identifier == null ? "" : identifier.trim();
        String password = request.getParameter("password");

        if (identifier.isEmpty() || identifier.length() > IDENTIFIER_MAX_LENGTH
                || password == null || password.isEmpty()) {
            showError(request, response, identifier, "Vui lòng nhập tên đăng nhập và mật khẩu.", 0);
            return;
        }

        try {
            LoginResult result = authService.login(identifier, password);
            switch (result.getOutcome()) {
                case SUCCESS -> signIn(request, response, result.getUserId());
                case INVALID -> showError(request, response, identifier, "Tên đăng nhập hoặc mật khẩu không đúng."
                        + " Bạn đã nhập sai " + result.getFailedCount() + "/" + result.getMaxFailedLogins() + " lần,"
                        + " còn " + (result.getMaxFailedLogins() - result.getFailedCount())
                        + " lần trước khi bị khóa tạm " + result.getLockMinutes() + " phút.", 0);
                case TEMP_LOCKED -> {
                    long minutes = (result.getLockSeconds() + 59) / 60;
                    showError(request, response, identifier, "Bạn đã nhập sai " + result.getMaxFailedLogins()
                            + " lần liên tiếp nên bị khóa tạm. Vui lòng thử lại sau " + minutes + " phút.",
                            result.getLockSeconds());
                }
                case PENDING -> showError(request, response, identifier,
                        "Tài khoản đang chờ quản trị viên duyệt, chưa đăng nhập được.", 0);
                case LOCKED -> showError(request, response, identifier,
                        "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.", 0);
            }
        } catch (SQLException e) {
            log("Không đăng nhập được", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void signIn(HttpServletRequest request, HttpServletResponse response, long userId)
            throws IOException, SQLException {
        SessionUser user = authService.loadSessionUser(userId);
        HttpSession session = request.getSession();
        // Đổi mã phiên sau khi đăng nhập để chống chiếm phiên (session fixation)
        request.changeSessionId();
        session.setAttribute(SessionUser.SESSION_KEY, user);

        // Vào trang chủ theo vai trò (S1-01)
        String target = user.getHomePath();
        response.sendRedirect(request.getContextPath() + target);
    }

    private static void showError(HttpServletRequest request, HttpServletResponse response, String identifier,
                                  String message, long lockSeconds) throws ServletException, IOException {
        request.setAttribute("username", identifier);
        request.setAttribute("error", message);
        request.setAttribute("lockSeconds", lockSeconds);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
