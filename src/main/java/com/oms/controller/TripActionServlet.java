package com.oms.controller;

import com.oms.service.DeliveryTripService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-06: thêm / bỏ điểm giao, xuất phát, hoàn thành, huỷ chuyến (DELIVERY_MANAGE theo AccessRules)
@WebServlet({"/trips/add", "/trips/remove", "/trips/depart", "/trips/complete", "/trips/cancel"})
public class TripActionServlet extends HttpServlet {

    private final DeliveryTripService tripService = new DeliveryTripService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        if (id == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long userId = CurrentUser.get(request).getId();
        String ip = request.getRemoteAddr();
        try {
            DeliveryTripService.Outcome outcome;
            switch (request.getServletPath()) {
                case "/trips/add" -> outcome = tripService.addNotes(userId, id,
                        TripCreateServlet.parseIds(request.getParameterValues("note_id")), ip);
                case "/trips/remove" -> {
                    Long note = AccountFormParser.parseId(request.getParameter("note"));
                    outcome = tripService.removeNote(userId, id, note == null ? -1 : note, ip);
                }
                case "/trips/depart" -> outcome = tripService.depart(userId, id, ip);
                case "/trips/complete" -> outcome = tripService.complete(userId, id, ip);
                default -> outcome = tripService.cancel(userId, id, ip);
            }
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            boolean cancelled = "/trips/cancel".equals(request.getServletPath()) && outcome.getError() == null;
            String key = cancelled ? TripListServlet.FLASH_MESSAGE
                    : outcome.getError() != null ? TripViewServlet.FLASH_ERROR : TripViewServlet.FLASH_MESSAGE;
            request.getSession().setAttribute(key,
                    outcome.getError() != null ? outcome.getError() : outcome.getMessage());
            response.sendRedirect(request.getContextPath() + (cancelled ? "/trips" : "/trips/view?id=" + id));
        } catch (SQLException e) {
            log("Không xử lý được chuyến giao", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
