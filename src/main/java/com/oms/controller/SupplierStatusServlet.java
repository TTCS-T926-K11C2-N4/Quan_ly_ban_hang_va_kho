package com.oms.controller;

import com.oms.model.Supplier;
import com.oms.service.SupplierService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S2-09: ngừng giao dịch (thay cho xoá khi đã có phiếu nhập) hoặc giao dịch lại
@WebServlet("/suppliers/status")
public class SupplierStatusServlet extends HttpServlet {

    private final SupplierService supplierService = new SupplierService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        String status = request.getParameter("status");
        if (!Supplier.ACTIVE.equals(status) && !Supplier.INACTIVE.equals(status)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            Supplier supplier = id == null ? null : supplierService.find(id);
            if (supplier == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (supplierService.changeStatus(supplier, status, CurrentUser.get(request).getId(), request.getRemoteAddr())) {
                request.getSession().setAttribute(SupplierListServlet.FLASH_MESSAGE, (Supplier.ACTIVE.equals(status)
                        ? "Đã cho giao dịch lại với \"" : "Đã ngừng giao dịch với \"") + supplier.getName() + "\".");
            }
            response.sendRedirect(request.getContextPath() + "/suppliers");
        } catch (SQLException e) {
            log("Không đổi được trạng thái nhà cung cấp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
