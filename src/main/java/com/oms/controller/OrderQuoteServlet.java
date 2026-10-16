package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.CustomerCredit;
import com.oms.model.OrderForm;
import com.oms.model.OrderQuote;
import com.oms.model.OrderStock;
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
            OrderStock stock = orderService.stock(customer, quote);
            List<Map<String, Object>> lines = new ArrayList<>();
            for (int i = 0; i < quote.getLines().size(); i++) {
                OrderQuote.Line line = quote.getLines().get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("amount", line.getError() == null ? money(line.getAmount()) : "—");
                item.put("discount", line.getError() == null ? money(line.getDiscount()) : "—");
                item.put("total", line.getError() == null ? money(line.getTotal()) : "—");
                item.put("error", line.getError());
                // S4-01: giá bảng giá làm gợi ý cho ô Đơn giá; giá sàn để cảnh báo khi giá thực bán xuống dưới
                item.put("sku", line.getProduct() == null ? null : line.getProduct().getSku());
                item.put("listPrice", line.getError() == null ? MoneyUtil.format(line.getListPrice()) : null);
                item.put("floorPrice", line.getError() == null ? money(line.getFloorPrice()) : null);
                item.put("unitPrice", line.getError() == null ? money(line.getUnitPrice()) : null);
                item.put("netPrice", line.getError() == null ? money(line.getNetUnitPrice()) : null);
                item.put("manual", line.isManualPrice());
                item.put("belowFloor", line.isBelowFloor());
                item.put("stock", stock == null ? null : stock(stock.getLines().get(i)));
                lines.add(item);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("lines", lines);
            data.put("subtotal", money(quote.getSubtotal()));
            data.put("discount", quote.getDiscount().signum() == 0 ? money(quote.getDiscount())
                    : "-" + money(quote.getDiscount()));
            data.put("total", money(quote.getTotal()));
            CustomerCredit credit = orderService.credit(customer);
            boolean overLimit = credit != null && credit.exceeds(quote.getTotal());
            data.put("needsApproval", quote.isNeedsApproval() || overLimit);
            data.put("credit", credit == null ? null : credit(credit, quote.getTotal()));
            data.put("warehouse", stock == null ? null : stock.getWarehouseName());
            OrderJson.write(response, JsonUtil.object(data));
        } catch (SQLException e) {
            log("Không tính được tiền đơn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // S4-03: tồn khả dụng của dòng; dòng lỗi giá/số lượng thì chưa kiểm tồn (null)
    private static Map<String, Object> stock(OrderStock.Line line) {
        if (!line.isChecked()) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("soldOut", line.isSoldOut());
        data.put("available", line.getAvailableText());
        data.put("shortage", line.isShortage());
        data.put("max", line.getMaxText());
        data.put("maxQty", line.getMaxQtyText());
        return data;
    }

    // S4-02: số liệu khối công nợ, tính lại theo tổng đơn đang gõ
    private static Map<String, Object> credit(CustomerCredit credit, BigDecimal orderTotal) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("granted", credit.isLimitGranted());
        data.put("limit", money(credit.getCreditLimit()));
        data.put("maxDebtDays", credit.getMaxDebtDays());
        data.put("ledger", money(credit.getLedgerBalance()));
        data.put("openOrders", money(credit.getOpenOrders()));
        data.put("available", money(credit.getAvailable()));
        data.put("short", credit.getAvailable().signum() < 0);
        data.put("usedPercent", credit.getUsedPercent());
        data.put("after", money(credit.getAvailableAfter(orderTotal)));
        data.put("overLimit", credit.exceeds(orderTotal));
        data.put("exceededBy", money(credit.getExceededBy(orderTotal)));
        return data;
    }

    private static String money(BigDecimal amount) {
        return MoneyUtil.format(amount) + " ₫";
    }
}
