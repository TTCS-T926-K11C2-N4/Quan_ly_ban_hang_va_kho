package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.Permission;
import com.oms.model.SessionUser;
import com.oms.service.CustomerProfileService;
import com.oms.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S3-03: tab Hồ sơ trong Chi tiết đại lý (?id=). Người sửa được đại lý (CUSTOMER_MANAGE trong phạm vi của mình) thấy
// nút Chỉnh sửa, Ngừng giao dịch / Giao dịch lại, và Xoá khi đại lý chưa phát sinh giao dịch.
@WebServlet("/customers/profile")
public class CustomerProfileServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/customers/customer-profile.jsp";
    static final String FLASH_MESSAGE = "customerProfileFlashMessage";
    // Xoá xong thì quay về danh sách đại lý, thông báo hiện ở đó
    static final String FLASH_DELETED = "customerDeletedFlashMessage";

    private static final CustomerService CUSTOMER_SERVICE = new CustomerService();
    private final CustomerProfileService profileService = new CustomerProfileService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        if (id == null) {
            response.sendRedirect(request.getContextPath() + "/customers");
            return;
        }
        try {
            Customer customer = CUSTOMER_SERVICE.findVisible(user.getId(), Permission.CUSTOMER_VIEW, id);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("customer", customer);
            request.setAttribute("profile", profileService.find(id));
            request.setAttribute("canManage", canManage(user, id));
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được hồ sơ đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Nhân viên kinh doanh chỉ sửa được đại lý mình phụ trách (data_scope ASSIGNED)
    static boolean canManage(SessionUser user, long customerId) throws SQLException {
        return user.can(Permission.CUSTOMER_MANAGE)
                && CUSTOMER_SERVICE.findVisible(user.getId(), Permission.CUSTOMER_MANAGE, customerId) != null;
    }
}
