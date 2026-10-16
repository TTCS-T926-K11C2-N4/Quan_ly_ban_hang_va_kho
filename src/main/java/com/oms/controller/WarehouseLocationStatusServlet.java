package com.oms.controller;

import com.oms.model.Warehouse;
import com.oms.model.WarehouseLocation;
import com.oms.service.WarehouseLocationService;
import com.oms.service.WarehouseService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-03: ngừng dùng (thay cho xoá, vì phiếu kho và sổ tồn có thể đã trỏ tới) hoặc dùng lại một vị trí
@WebServlet("/warehouses/locations/status")
public class WarehouseLocationStatusServlet extends HttpServlet {

    private final WarehouseService warehouseService = new WarehouseService();
    private final WarehouseLocationService locationService = new WarehouseLocationService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouseId"));
        Long locationId = AccountFormParser.parseId(request.getParameter("locationId"));
        String active = request.getParameter("active");
        if (!"true".equals(active) && !"false".equals(active)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            Warehouse warehouse = warehouseId == null ? null : warehouseService.find(warehouseId);
            WarehouseLocation location = warehouse == null || locationId == null ? null
                    : locationService.find(warehouse.getId(), locationId);
            if (location == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            boolean activate = Boolean.parseBoolean(active);
            String error = activate ? null
                    : locationService.validateDeactivate(location, locationService.findTree(warehouse.getId()));
            if (error != null) {
                request.getSession().setAttribute(WarehouseLocationServlet.FLASH_ERROR, error);
            } else if (locationService.changeActive(warehouse, location, activate, CurrentUser.get(request).getId(),
                    request.getRemoteAddr())) {
                request.getSession().setAttribute(WarehouseLocationServlet.FLASH_MESSAGE,
                        (activate ? "Đã dùng lại " : "Đã ngừng dùng ") + location.getTypeLabel().toLowerCase() + " "
                                + location.getCode() + ".");
            }
            response.sendRedirect(request.getContextPath() + "/warehouses/locations?id=" + warehouse.getId());
        } catch (SQLException e) {
            log("Không đổi được trạng thái vị trí kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
