package com.oms.service;

import com.oms.dao.SalesOrderDao;
import com.oms.model.Customer;
import com.oms.model.CustomerCredit;
import com.oms.model.DeliveryAddress;
import com.oms.model.OrderForm;
import com.oms.model.OrderProduct;
import com.oms.model.OrderQuote;
import com.oms.model.OrderStock;
import com.oms.model.PricingRules;
import com.oms.util.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesOrderServiceTest {

    // Ngày giao phải từ hôm nay trở đi: tính theo ngày chạy test để test không hỏng khi qua ngày
    private static final String TOMORROW = DateTimeUtil.today().plusDays(1).toString();

    private static final long CAN = 1;
    private static final long CASE = 2;

    // SP001: đơn vị cơ sở Lon, 1 Thùng = 24 Lon; nhóm hàng 7
    private static final OrderProduct WATER = new OrderProduct(10, "SP001", "Nước uống đóng chai", 7, List.of(
            new OrderProduct.Unit(CAN, "Lon", new BigDecimal("1.0000"), true),
            new OrderProduct.Unit(CASE, "Thùng", new BigDecimal("24.0000"), false)));
    private static final Map<Long, OrderProduct> PRODUCTS = Map.of(10L, WATER);

    private static PricingRules rules(List<PricingRules.DiscountPolicy> policies) {
        return new PricingRules(Map.of(PricingRules.key(10, CAN),
                new PricingRules.PriceItem(100, new BigDecimal("10000.00"), new BigDecimal("9500.00"))), policies);
    }

    private static OrderForm.Line line(String unitId, String qty) {
        return new OrderForm.Line("10", "SP001 - Nước uống đóng chai", unitId, qty, null);
    }

    // Bảng giá chỉ có giá theo Lon: giá Thùng = giá Lon x 24; số lượng quy về đơn vị cơ sở để lưu qty_base
    @Test
    void pricesLargerUnitFromBasePriceAndFactor() {
        OrderQuote quote = SalesOrderService.price(List.of(line("2", "10")), PRODUCTS, rules(List.of()));
        OrderQuote.Line priced = quote.getLines().get(0);
        assertNull(priced.getError());
        assertEquals(new BigDecimal("240000.00"), priced.getUnitPrice());
        assertEquals(new BigDecimal("228000.00"), priced.getFloorPrice());
        assertEquals(new BigDecimal("240.000"), priced.getQtyBase());
        assertEquals(new BigDecimal("2400000.00"), priced.getAmount());
        assertEquals(100L, priced.getPriceListItemId());
        assertEquals(new BigDecimal("2400000.00"), quote.getTotal());
    }

    // S3-01 AC3: nhiều chính sách cùng khớp thì lấy chính sách có lợi nhất cho khách; bậc theo số lượng cơ sở
    @Test
    void appliesBestMatchingDiscountTier() {
        PricingRules.DiscountPolicy byProduct = new PricingRules.DiscountPolicy(1, 10L, null, "PERCENT", List.of(
                new PricingRules.Tier(new BigDecimal("24"), new BigDecimal("2")),
                new PricingRules.Tier(new BigDecimal("240"), new BigDecimal("4"))));
        PricingRules.DiscountPolicy byCategory = new PricingRules.DiscountPolicy(2, null, 7L, "AMOUNT_PER_UNIT",
                List.of(new PricingRules.Tier(new BigDecimal("100"), new BigDecimal("300"))));
        OrderQuote quote = SalesOrderService.price(List.of(line("2", "10")), PRODUCTS,
                rules(List.of(byProduct, byCategory)));
        OrderQuote.Line priced = quote.getLines().get(0);
        // 4% x 2.400.000 = 96.000 lớn hơn 300đ x 240 lon = 72.000 nên lấy chính sách theo SKU
        assertEquals(new BigDecimal("96000.00"), priced.getDiscount());
        assertEquals(1L, priced.getDiscountPolicyId());
        assertEquals(new BigDecimal("2304000.00"), quote.getTotal());
    }

    // S3-01: chính sách theo nhóm hàng cha áp cho sản phẩm trong nhóm con (SP001 thuộc nhóm 7, nhóm con của 3);
    // đúng ví dụ trong docs/quy-tac-chiet-khau.md: 3 thùng = 72 lon, 250đ x 72 = 18.000 lớn hơn 2% x 720.000 = 14.400
    @Test
    void categoryPolicyCoversSubcategoriesAndBestWins() {
        PricingRules.DiscountPolicy byProduct = new PricingRules.DiscountPolicy(1, 10L, null, "PERCENT", List.of(
                new PricingRules.Tier(new BigDecimal("48"), new BigDecimal("2")),
                new PricingRules.Tier(new BigDecimal("96"), new BigDecimal("4"))));
        PricingRules.DiscountPolicy byParentCategory = new PricingRules.DiscountPolicy(2, null, 3L, Set.of(3L, 7L),
                "AMOUNT_PER_UNIT", List.of(new PricingRules.Tier(new BigDecimal("24"), new BigDecimal("250"))));
        OrderQuote.Line priced = SalesOrderService.price(List.of(line("2", "3")), PRODUCTS,
                rules(List.of(byProduct, byParentCategory))).getLines().get(0);
        assertEquals(new BigDecimal("720000.00"), priced.getAmount());
        assertEquals(new BigDecimal("18000.00"), priced.getDiscount());
        assertEquals(2L, priced.getDiscountPolicyId());

        PricingRules.DiscountPolicy otherCategory = new PricingRules.DiscountPolicy(3, null, 4L, Set.of(4L, 8L),
                "PERCENT", List.of(new PricingRules.Tier(new BigDecimal("1"), new BigDecimal("50"))));
        assertNull(SalesOrderService.price(List.of(line("2", "3")), PRODUCTS, rules(List.of(otherCategory)))
                .getLines().get(0).getDiscountPolicyId());
    }

    @Test
    void belowSmallestTierHasNoDiscount() {
        PricingRules.DiscountPolicy policy = new PricingRules.DiscountPolicy(1, 10L, null, "PERCENT",
                List.of(new PricingRules.Tier(new BigDecimal("48"), new BigDecimal("5"))));
        OrderQuote.Line priced = SalesOrderService.price(List.of(line("1", "47")), PRODUCTS, rules(List.of(policy)))
                .getLines().get(0);
        assertEquals(0, priced.getDiscount().signum());
        assertNull(priced.getDiscountPolicyId());
    }

    // S4-01: không có giá hiệu lực cho SKU thì chặn dòng hàng kèm lý do
    @Test
    void lineWithoutPriceOrInvalidQtyIsRejected() {
        PricingRules noPrices = new PricingRules(Map.of(), List.of());
        List<OrderQuote.Line> lines = SalesOrderService.price(List.of(line("1", "5"), line("1", "0"),
                line("9", "5"), new OrderForm.Line(null, "nước", "1", "5", null)), PRODUCTS, noPrices).getLines();
        assertEquals("Chưa có giá của SP001 trong bảng giá đang hiệu lực của nhóm khách hàng.", lines.get(0).getError());
        assertEquals("Số lượng phải lớn hơn 0, tối đa 3 chữ số thập phân.", lines.get(1).getError());
        assertEquals("Chọn đơn vị tính đã khai báo cho SP001.", lines.get(2).getError());
        assertEquals("Chọn sản phẩm trong danh sách gợi ý theo mã hoặc tên hàng.", lines.get(3).getError());
    }

    // S4-01: sửa giá tay được, nhưng thấp hơn giá sàn (9.500/Lon) thì đơn bị đánh dấu cần duyệt
    @Test
    void manualPriceBelowFloorNeedsApproval() {
        OrderQuote above = SalesOrderService.price(List.of(
                new OrderForm.Line("10", "SP001", "1", "10", "9.800")), PRODUCTS, rules(List.of()));
        OrderQuote.Line priced = above.getLines().get(0);
        assertTrue(priced.isManualPrice());
        assertEquals(new BigDecimal("10000.00"), priced.getListPrice());
        assertEquals(new BigDecimal("98000.00"), priced.getAmount());
        assertTrue(!priced.isBelowFloor() && !above.isNeedsApproval());

        OrderQuote below = SalesOrderService.price(List.of(line("1", "10"),
                new OrderForm.Line("10", "SP001", "1", "10", "9000")), PRODUCTS, rules(List.of()));
        assertTrue(below.getLines().get(1).isBelowFloor());
        assertTrue(below.isNeedsApproval());
        assertEquals(1, below.getBelowFloorLines().size());

        // Giá sửa tay 9.800 trên sàn nhưng chiết khấu 4% còn 9.408/Lon thì vẫn dưới sàn 9.500
        PricingRules.DiscountPolicy fourPercent = new PricingRules.DiscountPolicy(1, 10L, null, "PERCENT",
                List.of(new PricingRules.Tier(new BigDecimal("1"), new BigDecimal("4"))));
        OrderQuote discounted = SalesOrderService.price(List.of(
                new OrderForm.Line("10", "SP001", "1", "10", "9.800"), line("1", "10")), PRODUCTS,
                rules(List.of(fourPercent)));
        assertTrue(discounted.getLines().get(0).isBelowFloor());
        assertEquals(new BigDecimal("9408"), discounted.getLines().get(0).getNetUnitPrice());
        assertTrue(!discounted.getLines().get(1).isBelowFloor());

        assertEquals("Đơn giá phải là số tiền lớn hơn 0, vd 15.500.", SalesOrderService.price(List.of(
                new OrderForm.Line("10", "SP001", "1", "10", "0")), PRODUCTS, rules(List.of()))
                .getLines().get(0).getError());
    }

    @Test
    void parsesQuantityWithUpToThreeDecimals() {
        assertEquals(new BigDecimal("1.5"), SalesOrderService.parseQty("1,5"));
        assertEquals(new BigDecimal("2.125"), SalesOrderService.parseQty("2.125"));
        assertNull(SalesOrderService.parseQty("1.2345"));
        assertNull(SalesOrderService.parseQty("-1"));
        assertNull(SalesOrderService.parseQty("0"));
    }

    private static Customer customer(boolean blocked) {
        return new Customer(5, "DL00125", "Đại lý Minh Anh", null, null, null, null, 1, 3L,
                new BigDecimal("0.00"), 0, blocked, blocked ? "Nợ quá hạn" : null, Customer.ACTIVE, 0);
    }

    private static final List<DeliveryAddress> ADDRESSES = List.of(new DeliveryAddress(8, "Cửa hàng", "Hà Nội",
            null, true));

    // S3-07 AC1: đại lý bị khoá không tạo được đơn mới
    @Test
    void blockedCustomerCannotStartNewOrder() {
        OrderForm form = new OrderForm("5", "8", null, null, null, null, List.of());
        Map<String, String> errors = new SalesOrderService().validate(form, customer(true), null, false, ADDRESSES,
                null, null, null);
        assertEquals("Đại lý đang bị khoá giao dịch nên không tạo được đơn mới.", errors.get("customerId"));
    }

    // Lưu nháp được khi chưa đủ ngày giao và dòng hàng; gửi đơn thì bắt buộc
    @Test
    void submitRequiresDateAndLinesButDraftDoesNot() {
        OrderForm form = new OrderForm("5", "8", null, null, null, null, List.of());
        SalesOrderService service = new SalesOrderService();
        assertTrue(service.validate(form, customer(false), null, false, ADDRESSES, null, null, null).isEmpty());
        Map<String, String> errors = service.validate(form, customer(false), null, true, ADDRESSES, null, null, null);
        assertEquals("Vui lòng chọn ngày giao mong muốn.", errors.get("requestedDate"));
        assertEquals("Đơn hàng cần ít nhất một dòng hàng.", errors.get("lines"));
    }

    @Test
    void rejectsPastDeliveryDateAndForeignAddress() {
        String yesterday = DateTimeUtil.today().minusDays(1).toString();
        OrderForm form = new OrderForm("5", "99", yesterday, null, null, null, List.of());
        Map<String, String> errors = new SalesOrderService().validate(form, customer(false), null, false, ADDRESSES,
                null, null, null);
        assertEquals("Ngày giao không được trước hôm nay.", errors.get("requestedDate"));
        assertEquals("Điểm giao không thuộc đại lý đã chọn.", errors.get("deliveryAddressId"));
    }

    // S3-07 AC3: đơn nháp đã có của đại lý bị khoá vẫn xử lý tiếp được
    @Test
    void existingDraftOfBlockedCustomerCanContinue() {
        OrderForm form = new OrderForm("5", "8", null, null, 42L, "0", List.of());
        SalesOrderDao.Draft draft = new SalesOrderDao.Draft(5, "DL001", "DH261007-0001", form, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        assertTrue(new SalesOrderService().validate(form, customer(true), draft, false, ADDRESSES, null, null, null)
                .isEmpty());
    }

    private static CustomerCredit credit(String limit, String ledger, String openOrders, String overdue) {
        return new CustomerCredit(new BigDecimal(limit), 30, new BigDecimal(ledger), new BigDecimal(openOrders),
                new BigDecimal(overdue), overdue.equals("0") ? null : LocalDate.of(2026, 9, 19));
    }

    // S4-02 AC2: tổng đơn + công nợ (sổ công nợ và đơn đã gửi chưa xuất hoá đơn) vượt hạn mức thì cần duyệt
    @Test
    void orderOverCreditLimitNeedsApproval() {
        CustomerCredit credit = credit("50000000", "30000000", "15000000", "0");
        assertEquals(new BigDecimal("5000000"), credit.getAvailable());
        assertEquals(90, credit.getUsedPercent());
        assertTrue(!credit.exceeds(new BigDecimal("5000000")));
        assertTrue(credit.exceeds(new BigDecimal("5000001")));
        assertEquals(new BigDecimal("1000000"), credit.getExceededBy(new BigDecimal("6000000")));
        // Hạn mức 0: chưa được cho nợ, đơn nào có tiền cũng vượt
        CustomerCredit noLimit = credit("0", "0", "0", "0");
        assertTrue(!noLimit.isLimitGranted() && noLimit.exceeds(new BigDecimal("1000")));
    }

    // S4-02 AC3: có khoản quá hạn thì chặn đơn mới, kể cả lưu nháp; đơn nháp đã có chỉ lưu được, không gửi được
    @Test
    void overdueCustomerCannotStartOrSubmitOrder() {
        CustomerCredit overdue = credit("30000000", "22000000", "0", "8000000");
        SalesOrderService service = new SalesOrderService();
        OrderForm form = new OrderForm("5", "8", null, null, null, null, List.of());
        assertEquals("Đại lý có 8.000.000 ₫ nợ quá hạn (hạn trả từ 19/09/2026) nên không tạo được đơn mới.",
                service.validate(form, customer(false), null, false, ADDRESSES, null, overdue, null).get("customerId"));

        OrderForm draftForm = new OrderForm("5", "8", null, null, 42L, "0", List.of());
        SalesOrderDao.Draft draft = new SalesOrderDao.Draft(5, "DL001", "DH261007-0001", draftForm, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        assertTrue(service.validate(draftForm, customer(false), draft, false, ADDRESSES, null, overdue, null).isEmpty());
        assertTrue(service.validate(draftForm, customer(false), draft, true, ADDRESSES, null, overdue, null).get("form")
                .startsWith("Đại lý có 8.000.000 ₫ nợ quá hạn"));
    }

    // S4-03: khả dụng = thực tế − giữ chỗ; hai dòng cùng SKU cộng chung; gợi ý tối đa theo đơn vị đang đặt
    @Test
    void stockShortageSuggestsMaxQuantity() {
        // Kho có 100 Lon, 25 Lon đang giữ chỗ cho đơn khác -> khả dụng 75 Lon
        Map<Long, OrderStock.Level> levels = Map.of(10L,
                new OrderStock.Level(new BigDecimal("100.000"), new BigDecimal("25.000")));
        OrderQuote fits = SalesOrderService.price(List.of(line("2", "3")), PRODUCTS, rules(List.of()));
        OrderStock.Line ok = SalesOrderService.checkStock(fits, levels, "Kho trung tâm").getLines().get(0);
        assertTrue(ok.isChecked() && !ok.isSoldOut() && !ok.isShortage());
        assertEquals("75 Lon (≈ 3 Thùng)", ok.getAvailableText());

        // 3 Thùng (72 Lon) + 10 Lon = 82 Lon > 75: cả hai dòng báo vượt; dòng Thùng còn tối đa 2 Thùng (65 Lon)
        OrderQuote over = SalesOrderService.price(List.of(line("2", "3"), line("1", "10")), PRODUCTS,
                rules(List.of()));
        OrderStock stock = SalesOrderService.checkStock(over, levels, "Kho trung tâm");
        assertTrue(stock.getLines().get(0).isShortage() && stock.getLines().get(1).isShortage());
        assertEquals("2 Thùng", stock.getLines().get(0).getMaxText());
        assertEquals("3 Lon", stock.getLines().get(1).getMaxText());

        // Lưu nháp vẫn được, gửi đơn thì bị chặn kèm gợi ý
        OrderForm form = new OrderForm("5", "8", TOMORROW, null, null, null, List.of());
        SalesOrderService service = new SalesOrderService();
        assertTrue(service.validate(form, customer(false), null, false, ADDRESSES, null, null, stock).isEmpty());
        assertEquals("Vượt tồn khả dụng: Kho trung tâm còn 75 Lon (≈ 3 Thùng). Đặt tối đa 2 Thùng.",
                service.validate(form, customer(false), null, true, ADDRESSES, null, null, stock).get("lines.0"));
    }

    // SKU chưa có dòng tồn ở kho (chưa từng nhập kho) coi như tồn 0: hết hàng, gửi đơn bị chặn
    @Test
    void missingStockRowIsSoldOut() {
        OrderQuote quote = SalesOrderService.price(List.of(line("2", "1")), PRODUCTS, rules(List.of()));
        OrderStock stock = SalesOrderService.checkStock(quote, Map.of(), "Kho trung tâm");
        OrderStock.Line line = stock.getLines().get(0);
        assertTrue(line.isChecked() && line.isSoldOut() && line.isShortage());
        OrderForm form = new OrderForm("5", "8", TOMORROW, null, null, null, List.of());
        assertEquals("Hết hàng: Kho trung tâm không còn tồn khả dụng của mặt hàng này.", new SalesOrderService()
                .validate(form, customer(false), null, true, ADDRESSES, null, null, stock).get("lines.0"));
    }

    // S2-04: nhật ký đơn ghi phần đầu đơn và tổng tiền đã tính, gửi đơn thì trạng thái là Chờ duyệt
    @Test
    void auditValuesOfSubmittedOrder() {
        OrderForm form = new OrderForm("5", "8", TOMORROW, null, null, null, List.of(line("2", "2")));
        OrderQuote quote = SalesOrderService.price(form.getLines(), PRODUCTS, rules(List.of()));
        Map<String, Object> values = SalesOrderService.orderValues("DH261008-0001", "DL001", true, form, quote,
                true);
        assertEquals(true, values.get("overCreditLimit"));
        assertEquals(true, values.get("needsApproval"));
        assertEquals("DH261008-0001", values.get("orderNo"));
        assertEquals("PENDING_APPROVAL", values.get("status"));
        assertEquals(8L, values.get("deliveryAddressId"));
        assertEquals(1, values.get("lineCount"));
        assertEquals("480000", values.get("subtotal"));
        assertEquals("480000", values.get("total"));
    }

    // Giá trị trước khi sửa lấy từ số đã lưu của đơn nháp, không tính lại theo bảng giá hiện tại
    @Test
    void auditValuesBeforeEditingDraft() {
        OrderForm form = new OrderForm("5", "8", null, null, 42L, "3", List.of(line("1", "5"), line("2", "1")));
        SalesOrderDao.Draft draft = new SalesOrderDao.Draft(5, "DL001", "DH261007-0001", form,
                new BigDecimal("290000.00"), new BigDecimal("0.00"), new BigDecimal("290000.00"));
        Map<String, Object> values = SalesOrderService.draftValues(draft);
        assertEquals("DRAFT", values.get("status"));
        assertEquals("DL001", values.get("customerCode"));
        assertEquals(2, values.get("lineCount"));
        assertNull(values.get("requestedDate"));
        assertEquals("0", values.get("discount"));
        assertEquals("290000", values.get("total"));
    }
}
