package com.oms.service;

import com.oms.model.OrderForm;
import com.oms.model.OrderProduct;
import com.oms.model.OrderQuote;
import com.oms.model.PricingRules;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderCopyServiceTest {

    private static final long CAN = 1;
    private static final OrderProduct WATER = new OrderProduct(10, "SP001", "Nước uống đóng chai", 7, List.of(
            new OrderProduct.Unit(CAN, "Lon", new BigDecimal("1.0000"), true)));
    private static final Map<Long, OrderProduct> PRODUCTS = Map.of(10L, WATER);

    private static PricingRules rules(String price) {
        return new PricingRules(Map.of(PricingRules.key(10, CAN),
                new PricingRules.PriceItem(100, new BigDecimal(price), new BigDecimal("9000.00"))), List.of());
    }

    private static OrderForm.Line water(String qty) {
        return new OrderForm.Line("10", "SP001 - Nước uống đóng chai", "1", qty, null);
    }

    private static OrderForm.Line gone() {
        return new OrderForm.Line("99", "SP099 - Hàng đã ngừng bán", "1", "5", null);
    }

    // AC2: bản sao không thừa hưởng giá cũ; giá luôn lấy từ bảng giá hiện hành
    @Test
    void copiedLinesUseCurrentPriceList() {
        List<OrderForm.Line> lines = List.of(water("10"));
        OrderQuote oldQuote = SalesOrderService.price(lines, PRODUCTS, rules("10000.00"));
        OrderQuote newQuote = SalesOrderService.price(lines, PRODUCTS, rules("12000.00"));
        assertEquals(0, new BigDecimal("100000").compareTo(oldQuote.getTotal()));
        assertEquals(0, new BigDecimal("120000").compareTo(newQuote.getTotal()));
        assertTrue(lines.get(0).getPrice() == null);
    }

    // Dòng hàng đã ngừng bán bị bỏ qua và được báo lại, các dòng còn lại vẫn sao chép
    @Test
    void skipsLinesThatAreNoLongerSellable() {
        List<OrderForm.Line> lines = List.of(water("10"), gone());
        OrderQuote quote = SalesOrderService.price(lines, PRODUCTS, rules("10000.00"));
        OrderCopyService.Split split = OrderCopyService.split(lines, quote);
        assertEquals(1, split.getKept().size());
        assertEquals("10", split.getKept().get(0).getProductId());
        assertEquals(1, split.getSkipped().size());
        assertTrue(split.getSkipped().get(0).startsWith("SP099 - Hàng đã ngừng bán: "));
    }

    @Test
    void keepsNothingWhenEveryLineIsGone() {
        List<OrderForm.Line> lines = List.of(gone());
        OrderQuote quote = SalesOrderService.price(lines, PRODUCTS, rules("10000.00"));
        assertTrue(OrderCopyService.split(lines, quote).getKept().isEmpty());
    }
}
