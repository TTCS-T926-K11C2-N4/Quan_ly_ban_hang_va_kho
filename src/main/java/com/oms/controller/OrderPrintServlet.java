package com.oms.controller;

import com.oms.model.OrderDetail;
import com.oms.service.OrderLifecycleService;
import com.oms.util.Code128;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S4-08: phiếu đơn hàng để in hoặc lưu PDF (trình duyệt: In → Lưu dạng PDF). Cùng phạm vi xem đơn như trang chi tiết.
@WebServlet("/orders/print")
public class OrderPrintServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/orders/order-print.jsp";
    private static final int BARCODE_HEIGHT = 48;

    private final OrderLifecycleService lifecycle = new OrderLifecycleService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            OrderDetail order = id == null ? null : lifecycle.findVisible(CurrentUser.get(request).getId(), id);
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("order", order);
            request.setAttribute("barcode", Code128.svg(order.getOrderNo(), BARCODE_HEIGHT));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được phiếu in đơn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
