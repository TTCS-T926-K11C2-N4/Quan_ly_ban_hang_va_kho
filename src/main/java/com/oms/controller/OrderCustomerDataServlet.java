package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.DeliveryAddress;
import com.oms.model.Permission;
import com.oms.service.CustomerService;
import com.oms.service.SalesOrderService;
import com.oms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// S3-09: khi đổi đại lý trên màn tạo đơn, orders.js lấy điểm giao và trạng thái khoá của đại lý đó (JSON)
@WebServlet("/orders/customer-data")
public class OrderCustomerDataServlet extends HttpServlet {

    private final CustomerService customerService = new CustomerService();
    private final SalesOrderService orderService = new SalesOrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("customerId"));
        try {
            Customer customer = id == null ? null
                    : customerService.findVisible(CurrentUser.get(request).getId(), Permission.ORDER_MANAGE, id);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            List<Map<String, Object>> addresses = new ArrayList<>();
            for (DeliveryAddress address : orderService.getDeliveryAddresses(customer.getId())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", address.getId());
                item.put("text", address.getText());
                item.put("isDefault", address.isDefaultAddress());
                addresses.add(item);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("blocked", customer.isBlocked());
            data.put("blockReason", customer.getBlockReason());
            data.put("active", customer.isActive());
            data.put("addresses", addresses);
            OrderJson.write(response, JsonUtil.object(data));
        } catch (SQLException e) {
            log("Không tải được điểm giao của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
