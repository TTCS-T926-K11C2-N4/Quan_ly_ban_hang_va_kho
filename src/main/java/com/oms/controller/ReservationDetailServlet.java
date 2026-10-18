package com.oms.controller;

import com.oms.model.ReservationOrderDetail;
import com.oms.service.StockReservationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-06: chi tiết đơn đang giữ chỗ: số lượng đặt và số lượng giữ chỗ từng dòng hàng, trạng thái giữ chỗ
@WebServlet("/stock/reservations/view")
public class ReservationDetailServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/reservations/reservation-detail.jsp";

    private final StockReservationService reservationService = new StockReservationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long orderId = AccountFormParser.parseId(request.getParameter("order"));
        try {
            ReservationOrderDetail order = orderId == null ? null
                    : reservationService.findVisible(CurrentUser.get(request).getId(), orderId);
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("order", order);
            request.setAttribute("holdHours", reservationService.getHoldHours());
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được chi tiết giữ chỗ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
