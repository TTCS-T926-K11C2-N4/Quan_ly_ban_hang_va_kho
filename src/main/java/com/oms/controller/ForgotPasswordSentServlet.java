package com.oms.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// ForgotPasswordServlet.doPost redirect sang đây (Post/Redirect/Get) để F5 không gửi lại email
@WebServlet("/forgot-password/sent")
public class ForgotPasswordSentServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/auth/forgot-password-sent.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
