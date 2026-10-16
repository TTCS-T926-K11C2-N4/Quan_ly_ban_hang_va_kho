package com.oms.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Trang tổng hợp các báo cáo (REPORT_VIEW); mỗi báo cáo kiểm quyền riêng của nó
@WebServlet("/reports")
public class ReportHubServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/reports/report-hub.jsp").forward(request, response);
    }
}
