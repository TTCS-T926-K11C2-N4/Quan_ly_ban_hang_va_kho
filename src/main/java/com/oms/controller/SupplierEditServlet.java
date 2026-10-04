package com.oms.controller;

import com.oms.model.Supplier;
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

// S2-09: lưu form "Sửa nhà cung cấp" (mở bằng /suppliers?edit=id)
@WebServlet("/suppliers/edit")
public class SupplierEditServlet extends HttpServlet {

    private final SupplierService supplierService = new SupplierService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            Supplier editing = id == null ? null : supplierService.find(id);
            if (editing == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            SupplierForm form = SupplierFormParser.parse(request);
            Map<String, String> errors = supplierService.validate(form, editing);
            boolean saved = false;
            if (errors.isEmpty()) {
                try {
                    saved = supplierService.update(editing, form, CurrentUser.get(request).getId(),
                            request.getRemoteAddr());
                } catch (SQLIntegrityConstraintViolationException e) {
                    errors = supplierService.validate(form, editing);
                }
            }
            if (!errors.isEmpty()) {
                SupplierListServlet.show(request, response, form, editing, errors);
                return;
            }
            request.getSession().setAttribute(saved ? SupplierListServlet.FLASH_MESSAGE : SupplierListServlet.FLASH_ERROR,
                    saved ? "Đã lưu nhà cung cấp \"" + form.getName() + "\"." : "Nhà cung cấp không còn tồn tại.");
            response.sendRedirect(request.getContextPath() + "/suppliers");
        } catch (SQLException e) {
            log("Không sửa được nhà cung cấp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
