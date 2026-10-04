package com.oms.service;

import com.oms.model.Product;
import com.oms.model.SelectOption;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductImportServiceTest {

    // Khoá đã chuẩn hoá (chữ thường) như Lookups dựng từ danh mục đơn vị: tra theo mã hoặc tên
    private static final SelectOption LOC = new SelectOption(7, "LOC", "Lốc");
    private static final SelectOption THUNG = new SelectOption(8, "THUNG", "Thùng");
    private static final Map<String, SelectOption> UNITS = Map.of("loc", LOC, "lốc", LOC, "thung", THUNG, "thùng", THUNG);

    // S2-08 + S2-07: ô "Lốc=6; Thùng=24" ra hệ số theo id đơn vị; mã hay tên, hoa hay thường đều nhận
    @Test
    void conversionsAcceptUnitCodeOrNameAndDecimalComma() {
        ProductImportService.ParsedConversions parsed = ProductImportService.parseConversions(" lốc = 6 ;THUNG=0,5", UNITS);
        assertTrue(parsed.errors.isEmpty(), parsed.errors.toString());
        assertEquals(Map.of(7L, new BigDecimal("6"), 8L, new BigDecimal("0.5")), parsed.factors);
    }

    @Test
    void conversionsReportEachBadPair() {
        ProductImportService.ParsedConversions parsed =
                ProductImportService.parseConversions("Lốc=6; Lốc=12; Két=24; Thùng=1; Thùng 24", UNITS);
        assertEquals(4, parsed.errors.size(), parsed.errors.toString());
        assertTrue(parsed.errors.get(0).contains("bị ghi hai lần"));
        assertTrue(parsed.errors.get(1).contains("Không tìm thấy đơn vị quy đổi: Két"));
        assertTrue(parsed.errors.get(2).contains("Hệ số của \"Thùng\""));
        assertTrue(parsed.errors.get(3).contains("sai dạng"));
        assertEquals(Map.of(7L, new BigDecimal("6")), parsed.factors);
    }

    @Test
    void statusAcceptsVietnameseLabel() {
        assertEquals(Product.ACTIVE, ProductImportService.parseStatus("Đang kinh doanh"));
        assertEquals(Product.DISCONTINUED, ProductImportService.parseStatus("  ngừng   KINH doanh "));
        assertNull(ProductImportService.parseStatus("Tạm dừng"));
    }
}
