package com.oms.controller;

import com.oms.model.Permission;
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
import java.util.List;
import java.util.Map;

// S5-03: thông tin một kho và cây vị trí lưu trong kho (Khu > Kệ), mở bằng nút "Xem vị trí" ở danh sách kho.
// Người có quyền khai báo kho thấy thêm form thêm/sửa vị trí: ?edit=locationId mở form sửa,
// ?type=RACK&parent=zoneId mở sẵn form thêm kệ vào khu đó.
@WebServlet("/warehouses/locations")
public class WarehouseLocationServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/warehouses/warehouse-locations.jsp";

    static final String FLASH_MESSAGE = "locationFlashMessage";
    static final String FLASH_ERROR = "locationFlashError";

    private static final WarehouseLocationService LOCATION_SERVICE = new WarehouseLocationService();

    private final WarehouseService warehouseService = new WarehouseService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            Warehouse warehouse = id == null ? null : warehouseService.find(id);
            if (warehouse == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            WarehouseLocationForm form = null;
            if (CurrentUser.get(request).can(Permission.WAREHOUSE_MANAGE)) {
                Long editId = AccountFormParser.parseId(request.getParameter("edit"));
                WarehouseLocation editing = editId == null ? null : LOCATION_SERVICE.find(warehouse.getId(), editId);
                form = editing != null ? WarehouseLocationForm.of(editing)
                        : WarehouseLocationForm.empty(WarehouseLocation.TYPE_LABELS.containsKey(request.getParameter("type"))
                                ? request.getParameter("type") : null,
                                AccountFormParser.parseId(request.getParameter("parent")));
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, FLASH_ERROR, "flashError");
            show(request, response, warehouse, form, Map.of());
        } catch (SQLException e) {
            log("Không tải được vị trí kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // form = null: người xem không có quyền khai báo, không hiện form
    static void show(HttpServletRequest request, HttpServletResponse response, Warehouse warehouse,
                     WarehouseLocationForm form, Map<String, String> errors)
            throws ServletException, IOException, SQLException {
        List<WarehouseLocation> tree = LOCATION_SERVICE.findTree(warehouse.getId());
        long zoneCount = tree.stream().filter(WarehouseLocation::isZone).count();
        long rackCount = tree.stream().mapToLong(node -> node.isZone() ? node.getChildren().size() : 1).sum();
        request.setAttribute("warehouse", warehouse);
        request.setAttribute("tree", tree);
        request.setAttribute("zoneCount", zoneCount);
        request.setAttribute("rackCount", rackCount);
        request.setAttribute("zones", LOCATION_SERVICE.findActiveZones(warehouse.getId()));
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("typeLabels", WarehouseLocation.TYPE_LABELS);
        request.setAttribute("canManage", form != null);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
