package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.DeliveryAddressEntry;
import com.oms.model.DeliveryAddressForm;
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
import java.util.Map;

// S3-04: lưu thẻ thêm mới (không có addressId) hoặc thẻ đang sửa một điểm giao
@WebServlet("/customers/delivery-addresses/save")
public class DeliveryAddressSaveServlet extends HttpServlet {

    private final CustomerService customerService = new CustomerService();
    private final DeliveryAddressService addressService = new DeliveryAddressService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        Long addressId = AccountFormParser.parseId(request.getParameter("addressId"));
        long userId = CurrentUser.get(request).getId();
        try {
            Customer customer = id == null ? null : customerService.findVisible(userId, Permission.CUSTOMER_MANAGE, id);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            DeliveryAddressEntry entry = null;
            if (addressId != null) {
                entry = addressService.findEntry(customer.getId(), addressId);
                if (entry == null || !entry.isActive()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            DeliveryAddressForm form = parse(request, addressId);
            Map<String, String> errors = addressService.validate(form);
            if (!errors.isEmpty()) {
                DeliveryAddressServlet.show(request, response, customer, true, form, errors);
                return;
            }
            String message;
            if (entry == null) {
                addressService.create(customer, form, userId, request.getRemoteAddr());
                message = "Đã thêm điểm giao hàng cho \"" + customer.getName() + "\".";
            } else {
                addressService.update(customer, entry, form, userId, request.getRemoteAddr());
                message = "Đã cập nhật điểm giao hàng.";
            }
            request.getSession().setAttribute(DeliveryAddressServlet.FLASH_MESSAGE, message);
            response.sendRedirect(request.getContextPath() + "/customers/delivery-addresses?id=" + customer.getId());
        } catch (SQLException e) {
            log("Không lưu được điểm giao hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static DeliveryAddressForm parse(HttpServletRequest request, Long addressId) {
        String phone = normalize(request.getParameter("receiverPhone"));
        // Cho gõ "0903 123 456" hay "0903.123.456"; lưu dạng liền chữ số
        phone = phone == null ? null : phone.replaceAll("[\\s.-]", "");
        return new DeliveryAddressForm(addressId, normalize(request.getParameter("label")),
                normalize(request.getParameter("address")), normalize(request.getParameter("receiverName")),
                phone, normalize(request.getParameter("routeNote")), request.getParameter("makeDefault") != null);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
