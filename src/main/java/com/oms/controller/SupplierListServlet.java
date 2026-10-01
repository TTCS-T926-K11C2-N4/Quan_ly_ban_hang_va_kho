package com.oms.controller;

import com.oms.model.Supplier;
import com.oms.model.SupplierFilter;
import com.oms.service.SupplierService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// Danh sách nhà cung cấp có tìm kiếm, lọc trạng thái và phân trang (S2-09)
@WebServlet("/suppliers")
public class SupplierListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/suppliers/supplier-list.jsp";
    private static final int KEYWORD_MAX_LENGTH = 100;

    // Thông báo một lần sau khi thêm/sửa/ngừng giao dịch/xoá (Post/Redirect/Get)
    static final String FLASH_MESSAGE = "flashSupplierMessage";
    static final String FLASH_ERROR = "flashSupplierError";

    private final SupplierService supplierService = new SupplierService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = SupplierFormParser.normalize(request.getParameter("keyword"));
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            keyword = keyword.substring(0, KEYWORD_MAX_LENGTH);
        }
        String statusFilter = SupplierFormParser.normalize(request.getParameter("statusFilter"));
        if (!List.of(Supplier.ACTIVE, Supplier.INACTIVE).contains(statusFilter)) {
            statusFilter = null;
        }

        try {
            request.setAttribute("supplierPage",
                    supplierService.search(new SupplierFilter(keyword, statusFilter), parsePage(request.getParameter("page"))));
        } catch (SQLException e) {
            log("Không tải được danh sách nhà cung cấp", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("keyword", keyword);
        request.setAttribute("statusFilter", statusFilter);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static int parsePage(String value) {
        try {
            return value == null ? 1 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
