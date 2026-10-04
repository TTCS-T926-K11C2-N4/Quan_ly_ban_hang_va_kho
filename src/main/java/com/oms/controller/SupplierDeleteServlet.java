package com.oms.controller;

import com.oms.model.Supplier;
import com.oms.service.SupplierService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

// S2-09: xoá nhà cung cấp chưa có phiếu nhập; đã có thì chỉ ngừng giao dịch (AC2)
@WebServlet("/suppliers/delete")
public class SupplierDeleteServlet extends HttpServlet {

    private final SupplierService supplierService = new SupplierService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            Supplier supplier = id == null ? null : supplierService.find(id);
            if (supplier == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            HttpSession session = request.getSession();
            String error = supplierService.validateDelete(supplier);
            if (error == null) {
                try {
                    supplierService.delete(supplier, CurrentUser.get(request).getId(), request.getRemoteAddr());
                    session.setAttribute(SupplierListServlet.FLASH_MESSAGE,
                            "Đã xoá nhà cung cấp \"" + supplier.getName() + "\".");
                } catch (SQLIntegrityConstraintViolationException e) {
                    // Vừa có phiếu nhập gắn vào sau bước kiểm tra
                    error = "Nhà cung cấp \"" + supplier.getName()
                            + "\" vừa có phiếu nhập nên không xoá được, chỉ ngừng giao dịch.";
                }
            }
            if (error != null) {
                session.setAttribute(SupplierListServlet.FLASH_ERROR, error);
            }
            response.sendRedirect(request.getContextPath() + "/suppliers");
        } catch (SQLException e) {
            log("Không xoá được nhà cung cấp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
