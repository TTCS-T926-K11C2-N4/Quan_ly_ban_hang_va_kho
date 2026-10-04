package com.oms.controller;

import com.oms.model.ProductImportBatch;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

// S2-08 Bước 3: báo cáo tổng kết sau khi nhập
@WebServlet("/products/import/result")
public class ProductImportResultServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/products/import-result.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        ProductImportBatch result = session == null ? null
                : (ProductImportBatch) session.getAttribute(ProductImportServlet.SESSION_RESULT);
        if (result == null) {
            response.sendRedirect(request.getContextPath() + "/products/import");
            return;
        }
        request.setAttribute("result", result);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
