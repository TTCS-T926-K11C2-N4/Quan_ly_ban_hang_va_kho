package com.oms.controller;

import com.oms.model.ImportBatch;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

// S2-01 Bước 3: báo cáo tổng kết sau khi nhập
@WebServlet("/accounts/import/result")
public class AccountImportResultServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/accounts/import-result.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        ImportBatch result = session == null ? null : (ImportBatch) session.getAttribute(AccountImportServlet.SESSION_RESULT);
        if (result == null) {
            response.sendRedirect(request.getContextPath() + "/accounts/import");
            return;
        }
        request.setAttribute("result", result);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
