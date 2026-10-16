package com.oms.controller;

import com.oms.model.DeliveryTrip;
import com.oms.model.Permission;
import com.oms.service.DeliveryTripService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-06: chi tiết chuyến giao: điểm giao kèm địa chỉ, người nhận, số điện thoại; thêm / bỏ điểm, xuất phát, hoàn thành
@WebServlet("/trips/view")
public class TripViewServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/trips/trip-view.jsp";
    static final String FLASH_MESSAGE = "tripViewFlashMessage";
    static final String FLASH_ERROR = "tripViewFlashError";

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
            boolean manage = CurrentUser.get(request).can(Permission.DELIVERY_MANAGE)
                    && tripService.canUseWarehouse(CurrentUser.get(request).getId(), trip.getWarehouseId());
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, FLASH_ERROR, "flashError");
            request.setAttribute("trip", trip);
            request.setAttribute("canManage", manage);
            if (manage && trip.isPlanned()) {
                request.setAttribute("candidates", tripService.candidates(trip.getWarehouseId(), null));
            }
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được chuyến giao", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
