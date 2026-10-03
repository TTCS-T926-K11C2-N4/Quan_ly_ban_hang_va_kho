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
import java.util.Locale;
import java.util.Map;

// S2-07: thêm đơn vị tính; mã đổi sang chữ hoa
@WebServlet("/units/new")
public class UnitCreateServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/units/unit-form.jsp";

    private final UnitService unitService = new UnitService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showForm(request, response, null, null, null, Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String code = code(request);
        String name = text(request, "name");
        try {
            Map<String, String> errors = unitService.validate(code, name, null);
            if (!errors.isEmpty()) {
                showForm(request, response, null, code, name, errors);
                return;
            }
            try {
                unitService.create(code, name, CurrentUser.get(request).getId(), request.getRemoteAddr());
            } catch (SQLIntegrityConstraintViolationException e) {
                // Người khác vừa thêm trùng mã sau bước kiểm tra
                showForm(request, response, null, code, name, unitService.validate(code, name, null));
                return;
            }
            request.getSession().setAttribute(UnitListServlet.FLASH_MESSAGE, "Đã thêm đơn vị \"" + name + "\".");
            response.sendRedirect(request.getContextPath() + "/units");
        } catch (SQLException e) {
            log("Không thêm được đơn vị tính", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // unit = null khi thêm mới
    static void showForm(HttpServletRequest request, HttpServletResponse response, UnitRow unit, String code,
                         String name, Map<String, String> errors) throws ServletException, IOException {
        request.setAttribute("unit", unit);
        request.setAttribute("code", code);
        request.setAttribute("name", name);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    static String code(HttpServletRequest request) {
        String code = text(request, "code");
        return code == null ? null : code.toUpperCase(Locale.ROOT);
    }

    static String text(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
