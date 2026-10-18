package com.oms.controller;

import com.oms.model.DeliveryTrip;
import com.oms.service.DeliveryTripService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

// S6-07: theo dõi hàng đã xuất nhưng chưa giao xong: các chuyến đang giao kèm số điểm đã giao và còn lại, tiền hàng đang
// nằm trên đường, điểm giao không thành công (hàng vẫn thuộc trách nhiệm của chuyến), cảnh báo chuyến quá giờ dự kiến về
@WebServlet("/trips/monitor")
public class TripMonitorServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/trips/trip-monitor.jsp";

    private final DeliveryTripService tripService = new DeliveryTripService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        try {
            if (!tripService.canView(userId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            List<DeliveryTrip> trips = tripService.findRunning(userId);
            int remaining = 0;
            BigDecimal onRoad = BigDecimal.ZERO;
            BigDecimal failed = BigDecimal.ZERO;
            long overdue = 0;
            for (DeliveryTrip trip : trips) {
                remaining += trip.getRemainingCount();
                onRoad = onRoad.add(trip.getOnRoadValue());
                failed = failed.add(trip.getFailedValue());
                if (trip.isOverdue()) {
                    overdue++;
                }
            }
            request.setAttribute("trips", trips);
            request.setAttribute("remainingStops", remaining);
            request.setAttribute("onRoadValue", onRoad);
            request.setAttribute("failedValue", failed);
            request.setAttribute("overdueCount", overdue);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được theo dõi chuyến giao", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
