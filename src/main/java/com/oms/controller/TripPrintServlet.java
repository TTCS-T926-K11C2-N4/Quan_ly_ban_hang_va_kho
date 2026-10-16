package com.oms.controller;

import com.oms.model.DeliveryTrip;
import com.oms.service.DeliveryTripService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-06: danh sách điểm giao của chuyến để in: địa chỉ và số điện thoại người nhận từng điểm
@WebServlet("/trips/print")
public class TripPrintServlet extends HttpServlet {

    private final DeliveryTripService tripService = new DeliveryTripService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            if (!tripService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            DeliveryTrip trip = id == null ? null : tripService.find(id);
            if (trip == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("trip", trip);
            request.getRequestDispatcher("/WEB-INF/views/trips/trip-print.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được danh sách điểm giao để in", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
