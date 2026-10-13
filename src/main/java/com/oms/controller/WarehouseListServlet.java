package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.Warehouse;
import com.oms.model.WarehouseFilter;
import com.oms.service.WarehouseService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-03: danh sách kho có ba thẻ số liệu, tìm theo mã/tên/địa chỉ, lọc theo loại kho và trạng thái, phân trang.
// Nút thêm/sửa chỉ hiện với người có quyền khai báo kho (Quản lý kho, Admin).
@WebServlet("/warehouses")
public class WarehouseListServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/warehouses/warehouse-list.jsp";

    // Thông báo một lần sau khi thêm/sửa kho (Post/Redirect/Get)
    static final String FLASH_MESSAGE = "warehouseFlashMessage";
    static final String FLASH_ERROR = "warehouseFlashError";

    private final WarehouseService warehouseService = new WarehouseService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WarehouseFilter filter = WarehouseFormParser.parseFilter(request);
        try {
            request.setAttribute("summary", warehouseService.summary());
            request.setAttribute("warehousePage", warehouseService.search(filter,
                    WarehouseFormParser.parsePage(request)));
        } catch (SQLException e) {
            log("Không tải được danh sách kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("filter", filter);
        request.setAttribute("typeLabels", Warehouse.TYPE_LABELS);
        request.setAttribute("statusLabels", Warehouse.STATUS_LABELS);
        request.setAttribute("canManage", CurrentUser.get(request).can(Permission.WAREHOUSE_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
