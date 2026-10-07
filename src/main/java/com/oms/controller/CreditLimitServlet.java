package com.oms.controller;

import com.oms.model.CreditLimitForm;
import com.oms.model.Customer;
import com.oms.model.Permission;
import com.oms.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

// S3-05: xem hạn mức công nợ của đại lý (?id=); ?edit=1 mở form sửa cho người có quyền CREDIT_MANAGE.
// Chưa có danh sách đại lý (S3-03) nên thiếu ?id thì hiện ô chọn đại lý trong phạm vi được xem.
@WebServlet("/customers/credit-limit")
public class CreditLimitServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/customers/credit-limit.jsp";
    static final String FLASH_MESSAGE = "creditLimitFlashMessage";

    private static final CustomerService CUSTOMER_SERVICE = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        String idParam = request.getParameter("id");
        try {
            if (idParam == null) {
                request.setAttribute("customers", CUSTOMER_SERVICE.findVisibleCustomers(userId,
                        Permission.CUSTOMER_VIEW));
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }
            Long id = AccountFormParser.parseId(idParam);
            Customer customer = id == null ? null : CUSTOMER_SERVICE.findVisible(userId, Permission.CUSTOMER_VIEW, id);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            boolean editing = "1".equals(request.getParameter("edit"))
                    && CurrentUser.get(request).can(Permission.CREDIT_MANAGE);
            show(request, response, customer, editing ? CreditLimitForm.of(customer) : null, Map.of());
        } catch (SQLException e) {
            log("Không tải được hạn mức công nợ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // form = null: chỉ xem, form sửa đóng
    static void show(HttpServletRequest request, HttpServletResponse response, Customer customer, CreditLimitForm form,
                     Map<String, String> errors) throws ServletException, IOException {
        request.setAttribute("customer", customer);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
