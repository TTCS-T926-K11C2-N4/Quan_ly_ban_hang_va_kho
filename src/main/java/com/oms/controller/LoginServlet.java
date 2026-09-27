package com.oms.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/auth/login.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(RegisterServlet.FLASH_REGISTERED) != null) {
            session.removeAttribute(RegisterServlet.FLASH_REGISTERED);
            request.setAttribute("success", "Đăng ký thành công. Tài khoản đang chờ quản trị viên duyệt.");
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
