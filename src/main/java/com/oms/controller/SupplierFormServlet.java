package com.oms.controller;

import com.oms.model.Supplier;
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

// Thêm (/suppliers/new) và sửa (/suppliers/edit?id=) nhà cung cấp (S2-09), dùng chung một form
@WebServlet({"/suppliers/new", "/suppliers/edit"})
public class SupplierFormServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/suppliers/supplier-form.jsp";
    private static final String EDIT_PATH = "/suppliers/edit";

    private final SupplierService supplierService = new SupplierService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isEditing(request)) {
            showForm(request, response, null, Map.of());
            return;
        }
        try {
            Supplier supplier = findSupplier(request);
            if (supplier == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showForm(request, response, supplier, Map.of());
        } catch (SQLException e) {
            log("Không tải được nhà cung cấp để sửa", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long actorUserId = CurrentUser.get(request).getId();
        try {
            Supplier form;
            if (isEditing(request)) {
                Supplier existing = findSupplier(request);
                if (existing == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                form = SupplierFormParser.read(request, existing.getId(), existing.getStatus(), existing.isHasReceipts());
            } else {
                form = SupplierFormParser.read(request, null, Supplier.ACTIVE, false);
            }

            Map<String, String> errors = supplierService.validate(form);
            if (!errors.isEmpty()) {
                showForm(request, response, form, errors);
                return;
            }
            try {
                if (form.getId() == null) {
                    supplierService.create(form, actorUserId);
                } else {
                    supplierService.update(form, actorUserId);
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                // Người khác vừa tạo trùng mã giữa lúc kiểm tra và lúc lưu
                showForm(request, response, form, supplierService.validate(form));
                return;
            }

            String message = (form.getId() == null ? "Đã thêm nhà cung cấp " : "Đã cập nhật nhà cung cấp ")
                    + form.getCode() + ".";
            request.getSession().setAttribute(SupplierListServlet.FLASH_MESSAGE, message);
            response.sendRedirect(request.getContextPath() + "/suppliers");
        } catch (SQLException e) {
            log("Không lưu được nhà cung cấp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static boolean isEditing(HttpServletRequest request) {
        return EDIT_PATH.equals(request.getServletPath());
    }

    private Supplier findSupplier(HttpServletRequest request) throws SQLException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        return id == null ? null : supplierService.findById(id);
    }

    private static void showForm(HttpServletRequest request, HttpServletResponse response, Supplier form,
                                 Map<String, String> errors) throws ServletException, IOException {
        request.setAttribute("editing", isEditing(request));
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
