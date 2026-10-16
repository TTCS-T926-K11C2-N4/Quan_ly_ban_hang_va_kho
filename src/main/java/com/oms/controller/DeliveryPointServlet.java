package com.oms.controller;

import com.oms.model.DeliveryPoint;
import com.oms.model.Permission;
import com.oms.service.DeliveryConfirmService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

// S6-08: chi tiết điểm giao (?address=&date=): người nhận, số điện thoại (gọi), ghi chú đường đi, các đơn cần giao
// kèm hàng và tiền thu hộ; chọn phạm vi (các đơn) rồi bấm giao thành công / không thành công.
@WebServlet("/deliveries/point")
public class DeliveryPointServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/deliveries/delivery-point.jsp";

    private final DeliveryConfirmService deliveryService = new DeliveryConfirmService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long addressId = AccountFormParser.parseId(request.getParameter("address"));
        LocalDate date = DeliveryListServlet.parseDate(request.getParameter("date"));
        try {
            DeliveryPoint point = addressId == null ? null
                    : deliveryService.point(addressId, date, deliveryService.scope(CurrentUser.get(request).getId()));
            if (point == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, DeliveryListServlet.FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, DeliveryListServlet.FLASH_ERROR, "flashError");
            request.setAttribute("point", point);
            request.setAttribute("date", date);
            request.setAttribute("dateText", DateTimeUtil.formatDay(date));
            request.setAttribute("canManage", CurrentUser.get(request).can(Permission.DELIVERY_MANAGE));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được điểm giao", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
