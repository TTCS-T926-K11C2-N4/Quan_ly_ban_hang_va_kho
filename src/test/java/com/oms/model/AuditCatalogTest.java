package com.oms.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditCatalogTest {

    // S2-04: bộ lọc "Đối tượng" phải có các loại mà AC yêu cầu ghi nhật ký
    @Test
    void entitiesCoverInventoryPriceCreditLimitAndInvoice() {
        assertTrue(AuditCatalog.entities().keySet().containsAll(
                List.of("INVENTORY", "PRICE", "CREDIT_LIMIT", "INVOICE")));
    }

    @Test
    void unknownActionKeepsCodeAndFallsIntoOtherGroup() {
        assertEquals("STOCK_ADJUST", AuditCatalog.actionLabel("STOCK_ADJUST"));
        assertEquals(AuditCatalog.GROUP_OTHER, AuditCatalog.actionGroup("STOCK_ADJUST"));
        assertFalse(AuditCatalog.knownActions().contains("STOCK_ADJUST"));
    }

    @Test
    void summaryNamesActionEntityAndReference() {
        AuditLogRow row = new AuditLogRow(1, null, "Admin", "admin", "PRODUCT_UPDATE", "PRODUCT", 7L, "SP007",
                null, null, null, null);
        assertEquals("Sửa sản phẩm SP007", row.getSummary());
        AuditLogRow deleted = new AuditLogRow(2, null, "Admin", "admin", "USER_LOCK", "USER", 9L, null,
                null, null, null, null);
        assertEquals("Khoá tài khoản #9", deleted.getSummary());
    }
}
