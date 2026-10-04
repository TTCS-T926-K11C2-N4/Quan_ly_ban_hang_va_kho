package com.oms.controller;

import com.oms.model.UnitRow;
import com.oms.service.UnitService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

// S2-07: xoá đơn vị tính chưa được sản phẩm, bảng giá hay chứng từ nào dùng
@WebServlet("/units/delete")
public class UnitDeleteServlet extends HttpServlet {

    private final UnitService unitService = new UnitService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            UnitRow unit = id == null ? null : unitService.find(id);
            if (unit == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            HttpSession session = request.getSession();
            String error = unitService.validateDelete(unit);
            if (error == null) {
                try {
                    unitService.delete(unit, CurrentUser.get(request).getId(), request.getRemoteAddr());
                    session.setAttribute(UnitListServlet.FLASH_MESSAGE,
                            "Đã xoá đơn vị \"" + unit.getName() + "\".");
                } catch (SQLIntegrityConstraintViolationException e) {
                    error = "Đơn vị \"" + unit.getName()
                            + "\" đang được dùng trong bảng giá hoặc chứng từ nên không xoá được.";
                }
            }
            if (error != null) {
                session.setAttribute(UnitListServlet.FLASH_ERROR, error);
            }
            response.sendRedirect(request.getContextPath() + "/units");
        } catch (SQLException e) {
            log("Không xoá được đơn vị tính", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
