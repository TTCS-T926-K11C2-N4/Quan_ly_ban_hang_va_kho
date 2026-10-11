package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.Permission;
import com.oms.service.CustomerBlockService;
import com.oms.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S3-07: xem trạng thái giao dịch của đại lý (?id=); người có CREDIT_MANAGE thấy form khoá hoặc mở giao dịch.
// Thiếu ?id thì về ô chọn đại lý ở trang hạn mức (chưa có danh sách đại lý của S3-03).
@WebServlet("/customers/block")
public class CustomerBlockServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/customers/customer-block.jsp";
    static final String FLASH_MESSAGE = "customerBlockFlashMessage";

    private static final CustomerService CUSTOMER_SERVICE = new CustomerService();
    private static final CustomerBlockService BLOCK_SERVICE = new CustomerBlockService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        if (idParam == null) {
            response.sendRedirect(request.getContextPath() + "/customers/credit-limit");
            return;
        }
        Long id = AccountFormParser.parseId(idParam);
        try {
            Customer customer = id == null ? null
                    : CUSTOMER_SERVICE.findVisible(CurrentUser.get(request).getId(), Permission.CUSTOMER_VIEW, id);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            show(request, response, customer, null, null, null);
        } catch (SQLException e) {
            log("Không tải được trạng thái giao dịch đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // reason: lý do đã gõ; reasonError: lỗi ô lý do; formError: lỗi chung (vd người khác vừa đổi trạng thái)
    static void show(HttpServletRequest request, HttpServletResponse response, Customer customer, String reason,
                     String reasonError, String formError) throws ServletException, IOException, SQLException {
        request.setAttribute("customer", customer);
        request.setAttribute("latestBlock", BLOCK_SERVICE.findLatest(customer.getId()));
        request.setAttribute("openOrderCount", BLOCK_SERVICE.countOpenOrders(customer.getId()));
        request.setAttribute("reason", reason);
        request.setAttribute("reasonError", reasonError);
        request.setAttribute("formError", formError);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
