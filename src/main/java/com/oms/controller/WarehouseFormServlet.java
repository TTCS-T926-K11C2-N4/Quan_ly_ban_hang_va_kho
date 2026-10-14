package com.oms.controller;

import com.oms.model.SelectOption;
import com.oms.model.Warehouse;
import com.oms.model.WarehouseForm;
import com.oms.service.WarehouseService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;

// S5-03: form Thêm kho mới (/warehouses/new) và Sửa kho (/warehouses/edit?id=); lỗi thì hiện lại form kèm lỗi từng ô
@WebServlet({"/warehouses/new", "/warehouses/edit"})
public class WarehouseFormServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/warehouses/warehouse-form.jsp";

    private final WarehouseService warehouseService = new WarehouseService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Warehouse editing = null;
            if (isEdit(request)) {
                editing = findEditing(request);
                if (editing == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            show(request, response, editing == null ? WarehouseForm.empty() : WarehouseForm.of(editing), editing,
                    Map.of());
        } catch (SQLException e) {
            log("Không tải được form kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Warehouse editing = null;
            if (isEdit(request)) {
                editing = findEditing(request);
                if (editing == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            WarehouseForm form = WarehouseFormParser.parse(request);
            Map<String, String> errors = warehouseService.validate(form, editing);
            boolean saved = false;
            if (errors.isEmpty()) {
                long actorId = CurrentUser.get(request).getId();
                try {
                    if (editing == null) {
                        warehouseService.create(form, actorId, request.getRemoteAddr());
                        saved = true;
                    } else {
                        saved = warehouseService.update(editing, form, actorId, request.getRemoteAddr());
                    }
                } catch (SQLIntegrityConstraintViolationException e) {
                    // Người khác vừa thêm trùng mã sau bước kiểm tra
                    errors = warehouseService.validate(form, editing);
                }
            }
            if (!errors.isEmpty()) {
                show(request, response, form, editing, errors);
                return;
            }
            request.getSession().setAttribute(saved ? WarehouseListServlet.FLASH_MESSAGE : WarehouseListServlet.FLASH_ERROR,
                    !saved ? "Kho không còn tồn tại."
                            : (editing == null ? "Đã thêm kho \"" : "Đã lưu thông tin kho \"") + form.getName() + "\".");
            response.sendRedirect(request.getContextPath() + "/warehouses");
        } catch (SQLException e) {
            log("Không lưu được kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static boolean isEdit(HttpServletRequest request) {
        return "/warehouses/edit".equals(request.getServletPath());
    }

    private Warehouse findEditing(HttpServletRequest request) throws SQLException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        return id == null ? null : warehouseService.find(id);
    }

    // editing = null: form ở chế độ thêm mới
    private void show(HttpServletRequest request, HttpServletResponse response, WarehouseForm form, Warehouse editing,
                      Map<String, String> errors) throws ServletException, IOException, SQLException {
        List<SelectOption> managers = warehouseService.getManagerCandidates();
        request.setAttribute("form", form);
        request.setAttribute("editing", editing);
        request.setAttribute("errors", errors);
        request.setAttribute("managers", managers);
        // Người phụ trách đang lưu nhưng không còn chọn được (đổi vai trò, bị khoá): vẫn hiện để lưu lại không mất
        if (editing != null && editing.getManagerUserId() != null
                && managers.stream().noneMatch(manager -> manager.getId() == editing.getManagerUserId())) {
            request.setAttribute("currentManager", new SelectOption(editing.getManagerUserId(), editing.getManagerName()));
        }
        request.setAttribute("typeLabels", Warehouse.TYPE_LABELS);
        request.setAttribute("statusLabels", Warehouse.STATUS_LABELS);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
