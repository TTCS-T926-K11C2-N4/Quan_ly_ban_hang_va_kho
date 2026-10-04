package com.oms.controller;

import com.oms.model.UnitRow;
import com.oms.service.UnitService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

// S2-07: sửa mã, tên đơn vị tính (sản phẩm, bảng giá, chứng từ tham chiếu theo id nên số liệu không đổi)
@WebServlet("/units/edit")
public class UnitEditServlet extends HttpServlet {

    private final UnitService unitService = new UnitService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UnitRow unit = find(request);
            if (unit == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            UnitCreateServlet.showForm(request, response, unit, unit.getCode(), unit.getName(), Map.of());
        } catch (SQLException e) {
            log("Không tải được đơn vị tính", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UnitRow unit = find(request);
            if (unit == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String code = UnitCreateServlet.code(request);
            String name = UnitCreateServlet.text(request, "name");
            Map<String, String> errors = unitService.validate(code, name, unit);
            if (!errors.isEmpty()) {
                UnitCreateServlet.showForm(request, response, unit, code, name, errors);
                return;
            }
            try {
                unitService.update(unit, code, name, CurrentUser.get(request).getId(), request.getRemoteAddr());
            } catch (SQLIntegrityConstraintViolationException e) {
                UnitCreateServlet.showForm(request, response, unit, code, name, unitService.validate(code, name, unit));
                return;
            }
            request.getSession().setAttribute(UnitListServlet.FLASH_MESSAGE, "Đã cập nhật đơn vị \"" + name + "\".");
            response.sendRedirect(request.getContextPath() + "/units");
        } catch (SQLException e) {
            log("Không cập nhật được đơn vị tính", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private UnitRow find(HttpServletRequest request) throws SQLException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        return id == null ? null : unitService.find(id);
    }
}
