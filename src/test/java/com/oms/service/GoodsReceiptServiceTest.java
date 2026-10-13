package com.oms.service;

import com.oms.dao.GoodsReceiptDao;
import com.oms.model.GoodsReceiptForm;
import com.oms.model.ReceiptProduct;
import com.oms.model.SelectOption;
import com.oms.util.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoodsReceiptServiceTest {

    private final GoodsReceiptService service = new GoodsReceiptService();

    // Nước suối: đơn vị cơ sở chai, 1 thùng = 24 chai
    private static final ReceiptProduct WATER = new ReceiptProduct(11, "SP011", "Nước suối Lavie chai 500ml", false,
            List.of(new ReceiptProduct.Unit(2, "Chai", BigDecimal.ONE, true),
                    new ReceiptProduct.Unit(6, "Thùng", new BigDecimal("24.0000"), false)));

    private static GoodsReceiptService.Options options() {
        return new GoodsReceiptService.Options(List.of(WATER), List.of(new SelectOption(2, "NCC-02", "Công ty An Phú")),
                List.of(new SelectOption(1, "KHO-TT", "Kho trung tâm")),
                Map.of(1L, List.of(new SelectOption(5, "A-01", "A-01 · Kệ bia lon"))));
    }

    private static GoodsReceiptForm form(String date, GoodsReceiptForm.Line... lines) {
        return new GoodsReceiptForm(null, null, "2", "1", date, null, null, List.of(lines));
    }

    // S5-04 AC2: nhập theo đơn vị bất kỳ, quy về đơn vị cơ sở: 10 thùng x 24 = 240 chai
    @Test
    void convertsEnteredUnitToBaseUnit() {
        assertEquals(new BigDecimal("240.000"), GoodsReceiptService.toBase(new BigDecimal("10"), WATER.findUnit(6)));
        assertEquals(new BigDecimal("36.000"), GoodsReceiptService.toBase(new BigDecimal("1.5"), WATER.findUnit(6)));
    }

    @Test
    void parsesQuantityMoneyAndDates() {
        assertEquals(new BigDecimal("10.5"), GoodsReceiptService.parseQty("10,5", false));
        assertNull(GoodsReceiptService.parseQty("0", false));
        assertNull(GoodsReceiptService.parseQty("-2", false));
        assertEquals(new BigDecimal("-2"), GoodsReceiptService.parseQty("-2", true));
        assertNull(GoodsReceiptService.parseQty("1.2345", false));
        assertEquals(new BigDecimal("120000"), GoodsReceiptService.parseMoney("120.000"));
        assertEquals(new BigDecimal("120000"), GoodsReceiptService.parseMoney("120000"));
        assertNull(GoodsReceiptService.parseMoney("12.00"));
        assertNull(GoodsReceiptService.parseMoney("-5"));
        assertNull(GoodsReceiptService.parseDate("31/12/2026"));
    }

    // S5-04 AC1: nhà cung cấp, ngày nhập, kho nhập bắt buộc; phải có ít nhất một dòng hàng
    @Test
    void requiresHeaderAndAtLeastOneLine() throws SQLException {
        GoodsReceiptForm empty = new GoodsReceiptForm(null, null, null, null, null, null, null,
                List.of(new GoodsReceiptForm.Line(null, null, null, null, null, null, null, null)));
        Map<String, String> errors = service.validate(empty, options(), null, null);
        assertEquals("Vui lòng chọn nhà cung cấp.", errors.get("supplierId"));
        assertEquals("Vui lòng chọn kho nhận.", errors.get("warehouseId"));
        assertEquals("Vui lòng chọn ngày nhập.", errors.get("receiptDate"));
        assertEquals("Thêm ít nhất một dòng hàng nhập.", errors.get("lines"));
    }

    @Test
    void validatesEachLine() throws SQLException {
        GoodsReceiptForm.Line bad = new GoodsReceiptForm.Line("11", "SP011", "99", "0", "abc", "7", null, null);
        GoodsReceiptForm.Line good = new GoodsReceiptForm.Line("11", "SP011", "6", "10", "120.000", "5", null, null);
        Map<String, String> errors = service.validate(form(DateTimeUtil.today().toString(), bad, good), options(),
                null, null);
        assertEquals(Map.of("lines", "Kiểm tra lại các dòng hàng được đánh dấu đỏ."), errors);
        assertEquals("Chọn đơn vị tính đã khai báo của sản phẩm.", bad.getErrors().get("unitId"));
        assertEquals("Số lượng phải lớn hơn 0, tối đa 3 số lẻ.", bad.getErrors().get("qty"));
        assertEquals("Đơn giá là số tiền không âm, vd 120.000.", bad.getErrors().get("unitPrice"));
        assertEquals("Vị trí không thuộc kho nhận.", bad.getErrors().get("locationId"));
        assertTrue(good.getErrors().isEmpty());
    }

    @Test
    void rejectsFutureReceiptDateAndOtherWarehouses() throws SQLException {
        GoodsReceiptForm.Line line = new GoodsReceiptForm.Line("11", "SP011", "2", "5", "3.200", null, null, null);
        GoodsReceiptForm future = new GoodsReceiptForm(null, null, "2", "9",
                DateTimeUtil.today().plusDays(1).toString(), null, null, List.of(line));
        Map<String, String> errors = service.validate(future, options(), null, null);
        assertEquals("Ngày nhập không được sau hôm nay.", errors.get("receiptDate"));
        assertEquals("Bạn chỉ lập phiếu nhập cho kho đang hoạt động mà mình phụ trách.", errors.get("warehouseId"));
    }

    // S5-04 AC5 (phiếu điều chỉnh giảm): tồn không được âm, không nhỏ hơn lượng đang giữ chỗ
    @Test
    void adjustmentCannotDropBelowReservedStock() {
        GoodsReceiptDao.PostingItem minus = new GoodsReceiptDao.PostingItem(11, "SP011", 3, "MAC-DINH",
                new BigDecimal("-50"), null);
        assertThrows(GoodsReceiptService.StockShortageException.class,
                () -> GoodsReceiptService.checkShortage(minus, new BigDecimal("-10"), BigDecimal.ZERO));
        GoodsReceiptService.StockShortageException reserved = assertThrows(
                GoodsReceiptService.StockShortageException.class,
                () -> GoodsReceiptService.checkShortage(minus, new BigDecimal("30"), new BigDecimal("48")));
        assertEquals("SKU SP011: tồn sau điều chỉnh (30) nhỏ hơn lượng đang giữ chỗ cho đơn đã duyệt (48).",
                reserved.getMessage());
        assertDoesNotThrow(() -> GoodsReceiptService.checkShortage(minus, new BigDecimal("48"), new BigDecimal("48")));
    }
}
