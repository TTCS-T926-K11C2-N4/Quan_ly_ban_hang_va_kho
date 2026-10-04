package com.oms.controller;

import com.oms.model.SupplierForm;
import com.oms.service.SupplierService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

// S2-09: lưu form "Thêm nhà cung cấp" ở cuối trang danh sách; lỗi thì hiện lại trang kèm lỗi từng ô
@WebServlet("/suppliers/new")
public class SupplierCreateServlet extends HttpServlet {

    private final SupplierService supplierService = new SupplierService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SupplierForm form = SupplierFormParser.parse(request);
        try {
            Map<String, String> errors = supplierService.validate(form, null);
            if (errors.isEmpty()) {
                try {
                    supplierService.create(form, CurrentUser.get(request).getId(), request.getRemoteAddr());
                } catch (SQLIntegrityConstraintViolationException e) {
                    // Người khác vừa thêm trùng mã sau bước kiểm tra
                    errors = supplierService.validate(form, null);
                }
            }
            if (!errors.isEmpty()) {
                SupplierListServlet.show(request, response, form, null, errors);
                return;
            }
            request.getSession().setAttribute(SupplierListServlet.FLASH_MESSAGE,
                    "Đã thêm nhà cung cấp \"" + form.getName() + "\".");
            response.sendRedirect(request.getContextPath() + "/suppliers");
        } catch (SQLException e) {
            log("Không thêm được nhà cung cấp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
