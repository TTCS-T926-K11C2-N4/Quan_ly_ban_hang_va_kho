package com.oms.service;

import com.oms.model.CustomerCredit;
import com.oms.model.OrderForm;
import com.oms.model.OrderProduct;
import com.oms.model.OrderQuote;
import com.oms.model.PricingRules;
import com.oms.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderApprovalServiceTest {

    // SP001: Lon (cơ sở), Thùng = 24 Lon; giá 10.000/Lon, sàn 9.500/Lon
    private static final OrderProduct WATER = new OrderProduct(10, "SP001", "Nước uống đóng chai", 7, List.of(
            new OrderProduct.Unit(1, "Lon", new BigDecimal("1.0000"), true),
            new OrderProduct.Unit(2, "Thùng", new BigDecimal("24.0000"), false)));
    private static final PricingRules RULES = new PricingRules(Map.of(PricingRules.key(10, 1),
            new PricingRules.PriceItem(100, new BigDecimal("10000.00"), new BigDecimal("9500.00"))), List.of());

    private static OrderQuote quote(String unitId, String qty, String price) {
        return SalesOrderService.price(List.of(new OrderForm.Line("10", "SP001", unitId, qty, price)),
                Map.of(10L, WATER), RULES);
    }

    private static CustomerCredit credit(String limit, String used) {
        return new CustomerCredit(new BigDecimal(limit), 30, new BigDecimal(used), BigDecimal.ZERO, BigDecimal.ZERO,
                null);
    }

    // Đơn không vi phạm: không có vi phạm nào nên hệ thống tự duyệt khi gửi
    @Test
    void orderWithinLimitAndAboveFloorHasNoViolation() {
        assertTrue(OrderApprovalService.violations(quote("1", "10", null), credit("1000000", "0")).isEmpty());
    }

    // AC1: lý do và mức vi phạm chụp lại lúc gửi, đúng dạng JSON đọc lại bằng JSON_TABLE
    @Test
    void recordsCreditAndFloorViolations() {
        // 10 Lon x 9.000 = 90.000; hạn mức 50.000, đã dùng 20.000 -> vượt 60.000
        List<Map<String, Object>> violations = OrderApprovalService.violations(quote("1", "10", "9000"),
                credit("50000", "20000"));
        assertEquals(2, violations.size());
        assertEquals("{\"type\":\"OVER_CREDIT_LIMIT\",\"exceeded_by\":60000,\"credit_limit\":50000,\"used\":20000}",
                JsonUtil.object(violations.get(0)));
        assertEquals("{\"type\":\"BELOW_FLOOR_PRICE\",\"line_no\":1,\"sku\":\"SP001\",\"unit_price\":9000,"
                + "\"net_price\":9000,\"floor_price\":9500}", JsonUtil.object(violations.get(1)));
    }

    // AC2: Từ chối và Trả lại sửa bắt buộc ý kiến; Duyệt thì không
    @Test
    void rejectAndReturnRequireComment() {
        assertNull(OrderApprovalService.validateComment(OrderApprovalService.Decision.APPROVE, null));
        assertEquals("Vui lòng nhập ý kiến khi từ chối đơn.",
                OrderApprovalService.validateComment(OrderApprovalService.Decision.REJECT, null));
        assertEquals("Vui lòng nhập ý kiến khi trả lại sửa.",
                OrderApprovalService.validateComment(OrderApprovalService.Decision.RETURN, null));
        assertEquals("Ý kiến tối đa 500 ký tự.", OrderApprovalService.validateComment(
                OrderApprovalService.Decision.REJECT, "x".repeat(501)));
        assertNull(OrderApprovalService.Decision.of("delete"));
        assertEquals(OrderApprovalService.Decision.RETURN, OrderApprovalService.Decision.of("return"));
    }
}
