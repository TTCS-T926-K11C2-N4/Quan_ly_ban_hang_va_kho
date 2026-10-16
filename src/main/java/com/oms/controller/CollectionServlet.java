package com.oms.controller;

import com.oms.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-05: nhân viên kinh doanh xem các khoản thu theo tuyến của mình (Chờ nộp / Đã ghi nhận) và ghi nhận khoản mới
@WebServlet("/collections")
public class CollectionServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/payments/collection-list.jsp";
    static final String FLASH_MESSAGE = "collectionFlashMessage";
    private static final PaymentService PAYMENT_SERVICE = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            show(request, response);
        } catch (SQLException e) {
            log("Không tải được khoản thu theo tuyến", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Dùng chung với CollectionNewServlet khi form báo lỗi
    static void show(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        request.setAttribute("debtors", PAYMENT_SERVICE.debtorsOf(userId));
        request.setAttribute("collections", PAYMENT_SERVICE.findByCollector(userId));
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        request.setAttribute("maxReceiptMb", PaymentService.RECEIPT_MAX_BYTES / (1024 * 1024));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
