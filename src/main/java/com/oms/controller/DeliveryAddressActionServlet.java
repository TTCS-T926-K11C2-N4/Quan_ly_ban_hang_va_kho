package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.DeliveryAddressEntry;
import com.oms.model.Permission;
import com.oms.service.CustomerService;
import com.oms.service.DeliveryAddressService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S3-04: thao tác nhanh trên một điểm giao: đặt làm mặc định, xoá (hoặc ngừng dùng nếu đã có đơn), dùng lại
@WebServlet("/customers/delivery-addresses/action")
public class DeliveryAddressActionServlet extends HttpServlet {

    private final CustomerService customerService = new CustomerService();
    private final DeliveryAddressService addressService = new DeliveryAddressService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (!"default".equals(action) && !"remove".equals(action) && !"activate".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        Long addressId = AccountFormParser.parseId(request.getParameter("addressId"));
        long userId = CurrentUser.get(request).getId();
        String ip = request.getRemoteAddr();
        try {
            Customer customer = id == null ? null : customerService.findVisible(userId, Permission.CUSTOMER_MANAGE, id);
            DeliveryAddressEntry entry = customer == null || addressId == null ? null
                    : addressService.findEntry(customer.getId(), addressId);
            if (entry == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String message = switch (action) {
                case "default" -> addressService.setDefault(customer, entry, userId, ip)
                        ? "Đã đặt \"" + entry.getAddress() + "\" làm điểm giao mặc định."
                        : "Điểm giao đã ngừng dùng nên không đặt làm mặc định được. Hãy dùng lại điểm giao trước.";
                case "remove" -> addressService.remove(customer, entry, userId, ip)
                        == DeliveryAddressService.RemoveResult.DELETED
                        ? "Đã xoá điểm giao hàng."
                        : "Điểm giao đã có đơn hàng nên không xoá được, đã chuyển sang ngừng dùng.";
                default -> {
                    addressService.activate(customer, entry, userId, ip);
                    yield "Đã dùng lại điểm giao hàng.";
                }
            };
            request.getSession().setAttribute(DeliveryAddressServlet.FLASH_MESSAGE, message);
            response.sendRedirect(request.getContextPath() + "/customers/delivery-addresses?id=" + customer.getId());
        } catch (SQLException e) {
            log("Không cập nhật được điểm giao hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
