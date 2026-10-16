package com.oms.controller;

import com.oms.model.Warehouse;
import com.oms.model.WarehouseLocation;
import com.oms.model.WarehouseLocationForm;
import com.oms.service.WarehouseLocationService;
import com.oms.service.WarehouseService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

// S5-03: lưu form Thêm/Sửa vị trí (khu, kệ) ở trang vị trí của kho; lỗi thì hiện lại trang kèm lỗi từng ô
@WebServlet("/warehouses/locations/save")
public class WarehouseLocationSaveServlet extends HttpServlet {

    private final WarehouseService warehouseService = new WarehouseService();
    private final WarehouseLocationService locationService = new WarehouseLocationService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long warehouseId = AccountFormParser.parseId(request.getParameter("warehouseId"));
        try {
            Warehouse warehouse = warehouseId == null ? null : warehouseService.find(warehouseId);
            if (warehouse == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            WarehouseLocationForm form = WarehouseFormParser.parseLocation(request);
            WarehouseLocation editing = null;
            if (form.isEditing()) {
                Long locationId = AccountFormParser.parseId(form.getLocationId());
                editing = locationId == null ? null : locationService.find(warehouse.getId(), locationId);
                if (editing == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            Map<String, String> errors = locationService.validate(warehouse, form, editing);
            boolean saved = false;
            if (errors.isEmpty()) {
                try {
                    saved = locationService.save(warehouse, form, editing, CurrentUser.get(request).getId(),
                            request.getRemoteAddr());
                } catch (SQLIntegrityConstraintViolationException e) {
                    errors = locationService.validate(warehouse, form, editing);
                }
            }
            if (!errors.isEmpty()) {
                WarehouseLocationServlet.show(request, response, warehouse, form, errors);
                return;
            }
            String label = WarehouseLocation.TYPE_LABELS.get(form.getType()).toLowerCase() + " " + form.getCode();
            request.getSession().setAttribute(saved ? WarehouseLocationServlet.FLASH_MESSAGE
                            : WarehouseLocationServlet.FLASH_ERROR,
                    !saved ? "Vị trí không còn tồn tại." : (editing == null ? "Đã thêm " : "Đã lưu ") + label + ".");
            response.sendRedirect(request.getContextPath() + "/warehouses/locations?id=" + warehouse.getId());
        } catch (SQLException e) {
            log("Không lưu được vị trí kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
