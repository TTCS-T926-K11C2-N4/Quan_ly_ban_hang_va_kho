package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.OrderForm;
import com.oms.model.OrderQuote;
import com.oms.model.Permission;
import com.oms.service.CustomerService;
import com.oms.service.SalesOrderService;
import com.oms.util.JsonUtil;
import com.oms.util.MoneyUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// S3-09 AC3: tính lại tiền hàng, chiết khấu, tổng phải thu ngay khi thêm/sửa dòng hàng. orders.js gửi cả form,
// server tính bằng đúng hàm dùng lúc lưu đơn nên số trên màn hình khớp số được lưu.
@WebServlet("/orders/quote")
public class OrderQuoteServlet extends HttpServlet {

    private final CustomerService customerService = new CustomerService();
    private final SalesOrderService orderService = new SalesOrderService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OrderForm form = OrderFormParser.parse(request);
        Long id = AccountFormParser.parseId(form.getCustomerId());
        try {
            Customer customer = id == null ? null
                    : customerService.findVisible(CurrentUser.get(request).getId(), Permission.ORDER_MANAGE, id);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            OrderQuote quote = orderService.quote(customer, form.getLines(), orderService.getProducts());
            List<Map<String, Object>> lines = new ArrayList<>();
            for (OrderQuote.Line line : quote.getLines()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("amount", line.getError() == null ? money(line.getAmount()) : "—");
                item.put("discount", line.getError() == null ? money(line.getDiscount()) : "—");
                item.put("total", line.getError() == null ? money(line.getTotal()) : "—");
                item.put("error", line.getError());
                lines.add(item);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("lines", lines);
            data.put("subtotal", money(quote.getSubtotal()));
            data.put("discount", quote.getDiscount().signum() == 0 ? money(quote.getDiscount())
                    : "-" + money(quote.getDiscount()));
            data.put("total", money(quote.getTotal()));
            OrderJson.write(response, JsonUtil.object(data));
        } catch (SQLException e) {
            log("Không tính được tiền đơn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static String money(BigDecimal amount) {
        return MoneyUtil.format(amount) + " ₫";
    }
}
