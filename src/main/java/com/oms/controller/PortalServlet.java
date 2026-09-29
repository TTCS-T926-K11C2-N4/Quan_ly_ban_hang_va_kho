package com.oms.controller;

import com.oms.model.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Trang chủ của tài khoản đại lý (S1-01); nhân viên nội bộ dùng /dashboard
@WebServlet("/portal")
public class PortalServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/portal/portal.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
