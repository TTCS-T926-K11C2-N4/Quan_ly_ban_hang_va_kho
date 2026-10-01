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

// Thao tác nhanh từ danh sách nhà cung cấp, chỉ nhận POST (S2-09):
//   /suppliers/status : id + active=true|false để giao dịch lại / ngừng giao dịch
//   /suppliers/delete : id; nhà cung cấp đã có phiếu nhập thì không xoá, báo dùng ngừng giao dịch
@WebServlet({"/suppliers/status", "/suppliers/delete"})
public class SupplierActionServlet extends HttpServlet {

    private static final String DELETE_PATH = "/suppliers/delete";

    private final SupplierService supplierService = new SupplierService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Long id = AccountFormParser.parseId(request.getParameter("id"));
            Supplier supplier = id == null ? null : supplierService.findById(id);
            if (supplier == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            HttpSession session = request.getSession();
            long actorUserId = CurrentUser.get(request).getId();
            if (DELETE_PATH.equals(request.getServletPath())) {
                if (supplierService.delete(supplier.getId())) {
                    session.setAttribute(SupplierListServlet.FLASH_MESSAGE,
                            "Đã xoá nhà cung cấp " + supplier.getCode() + ".");
                } else {
                    session.setAttribute(SupplierListServlet.FLASH_ERROR, "Nhà cung cấp " + supplier.getCode()
                            + " đã có phiếu nhập kho nên không xoá được. Hãy chọn Ngừng giao dịch.");
                }
            } else {
                boolean active = Boolean.parseBoolean(request.getParameter("active"));
                boolean changed = supplierService.changeStatus(supplier.getId(), active, actorUserId);
                String message = !changed
                        ? "Nhà cung cấp " + supplier.getCode() + " đã ở trạng thái này."
                        : active
                                ? "Đã cho giao dịch lại với nhà cung cấp " + supplier.getCode() + "."
                                : "Đã ngừng giao dịch với nhà cung cấp " + supplier.getCode() + ".";
                session.setAttribute(SupplierListServlet.FLASH_MESSAGE, message);
            }
            response.sendRedirect(request.getContextPath() + "/suppliers");
        } catch (SQLException e) {
            log("Không thực hiện được thao tác trên nhà cung cấp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
