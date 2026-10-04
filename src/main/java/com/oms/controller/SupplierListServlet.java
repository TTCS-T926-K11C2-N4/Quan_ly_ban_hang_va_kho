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
import java.util.Map;

// S2-09: danh sách nhà cung cấp có tìm kiếm, phân trang; form thêm/sửa nằm ngay dưới danh sách (theo thiết kế).
// ?edit=id mở form sửa nhà cung cấp đó.
@WebServlet("/suppliers")
public class SupplierListServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/suppliers/supplier-list.jsp";

    // Thông báo một lần sau khi thêm/sửa/xoá/đổi trạng thái (Post/Redirect/Get)
    static final String FLASH_MESSAGE = "supplierFlashMessage";
    static final String FLASH_ERROR = "supplierFlashError";

    private static final SupplierService SUPPLIER_SERVICE = new SupplierService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long editId = AccountFormParser.parseId(request.getParameter("edit"));
        Supplier editing = null;
        try {
            editing = editId == null ? null : SUPPLIER_SERVICE.find(editId);
        } catch (SQLException e) {
            log("Không tải được nhà cung cấp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        show(request, response, editing == null ? SupplierForm.empty() : SupplierForm.of(editing), editing, Map.of());
    }

    // editing = null: form ở chế độ thêm mới
    static void show(HttpServletRequest request, HttpServletResponse response, SupplierForm form, Supplier editing,
                     Map<String, String> errors) throws ServletException, IOException {
        String keyword = request.getParameter("keyword");
        keyword = keyword == null ? "" : keyword.trim();
        try {
            request.setAttribute("supplierPage", SUPPLIER_SERVICE.search(keyword, parsePage(request)));
        } catch (SQLException e) {
            request.getServletContext().log("Không tải được danh sách nhà cung cấp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("keyword", keyword);
        request.setAttribute("form", form);
        request.setAttribute("editing", editing);
        request.setAttribute("errors", errors);
        request.setAttribute("paymentTerms", SupplierService.PAYMENT_TERMS);
        if (form.getPaymentTerms() != null && !SupplierService.PAYMENT_TERMS.contains(form.getPaymentTerms())) {
            request.setAttribute("otherPaymentTerms", form.getPaymentTerms());
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static int parsePage(HttpServletRequest request) {
        try {
            String page = request.getParameter("page");
            return page == null ? 1 : Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
