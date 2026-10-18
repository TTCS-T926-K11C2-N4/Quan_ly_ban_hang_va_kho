package com.oms.controller;

import com.oms.service.DeliveryTripService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// S6-06: lập chuyến giao từ các phiếu xuất đã chọn, gán tài xế và phương tiện
@WebServlet("/trips/new")
public class TripCreateServlet extends HttpServlet {

    private final DeliveryTripService tripService = new DeliveryTripService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouse"));
        Long regionId = AccountFormParser.parseId(request.getParameter("region"));
        if (warehouseId == null) {
            request.getSession().setAttribute(TripListServlet.FLASH_ERROR, "Vui lòng chọn kho xuất hàng.");
            response.sendRedirect(request.getContextPath() + "/trips");
            return;
        }
        String back = request.getContextPath() + "/trips?warehouse=" + warehouseId
                + (regionId == null ? "" : "&region=" + regionId);
        try {
            DeliveryTripService.Outcome outcome = tripService.create(CurrentUser.get(request).getId(), warehouseId,
                    regionId, request.getParameter("driverName"), request.getParameter("driverPhone"),
                    request.getParameter("vehiclePlate"), request.getParameter("plannedDeparture"),
                    request.getParameter("expectedReturn"), request.getParameter("note"),
                    parseIds(request.getParameterValues("note_id")), request.getRemoteAddr());
            if (outcome.getError() != null) {
                request.getSession().setAttribute(TripListServlet.FLASH_ERROR, outcome.getError());
                response.sendRedirect(back);
                return;
            }
            request.getSession().setAttribute(TripViewServlet.FLASH_MESSAGE, outcome.getMessage());
            response.sendRedirect(request.getContextPath() + "/trips/view?id=" + outcome.getId());
        } catch (SQLException e) {
            log("Không lập được chuyến giao", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    static List<Long> parseIds(String[] values) {
        List<Long> ids = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                Long id = AccountFormParser.parseId(value);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }
}
