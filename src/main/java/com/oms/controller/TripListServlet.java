package com.oms.controller;

import com.oms.dao.WarehouseDao;
import com.oms.model.Permission;
import com.oms.model.SessionUser;
import com.oms.service.DeliveryTripService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-06: danh sách chuyến giao hàng và hộp lập chuyến mới: chọn kho (và khu vực), tài xế, phương tiện, rồi tick các phiếu
// xuất Chờ giao cần gom vào chuyến. Chọn kho / khu vực thì tải lại để hiện đúng danh sách phiếu gom được.
@WebServlet("/trips")
public class TripListServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/trips/trip-list.jsp";
    static final String FLASH_MESSAGE = "tripFlashMessage";
    static final String FLASH_ERROR = "tripFlashError";

    private final DeliveryTripService tripService = new DeliveryTripService();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        try {
            if (!tripService.canView(user.getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } catch (SQLException e) {
            log("Không kiểm tra được quyền xem chuyến giao", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouse"));
        Long regionId = AccountFormParser.parseId(request.getParameter("region"));
        String status = request.getParameter("status");
        if (!"PLANNED".equals(status) && !"RUNNING".equals(status) && !"COMPLETED".equals(status)) {
            status = null;
        }
        try {
            request.setAttribute("trips", tripService.findTrips(user.getId(), status));
            request.setAttribute("warehouses", warehouseDao.findActive());
            request.setAttribute("regions", tripService.regions());
            if (warehouseId != null) {
                request.setAttribute("candidates", tripService.candidates(warehouseId, regionId));
            }
        } catch (SQLException e) {
            log("Không tải được danh sách chuyến giao", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("warehouseId", warehouseId);
        request.setAttribute("regionId", regionId);
        request.setAttribute("status", status);
        request.setAttribute("canManage", user.can(Permission.DELIVERY_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
