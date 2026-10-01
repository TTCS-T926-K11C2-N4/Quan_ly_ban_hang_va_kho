package com.oms.service;

import com.oms.model.Supplier;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S2-09: kiểm tra dữ liệu form nhà cung cấp (phần không cần DB)
class SupplierServiceTest {

    private final SupplierService supplierService = new SupplierService();

    private static Supplier supplier(String code, String name, String taxCode, String phone, String email) {
        return new Supplier(null, code, name, taxCode, "Trần Thị Bình", phone, email, "12 Láng Hạ, Hà Nội",
                "Thanh toán trong 30 ngày", Supplier.ACTIVE, false);
    }

    @Test
    void validSupplierHasNoErrors() {
        assertTrue(supplierService.validateFields(
                supplier("NCC-001", "Công ty TNHH Nước giải khát Việt", "0101234567", "02434567890", "lienhe@ncc.vn"))
                .isEmpty());
    }

    @Test
    void onlyCodeAndNameAreRequired() {
        Map<String, String> errors = supplierService.validateFields(
                new Supplier(null, null, null, null, null, null, null, null, null, Supplier.ACTIVE, false));
        assertEquals(Map.of("code", "Vui lòng nhập mã nhà cung cấp.", "name", "Vui lòng nhập tên nhà cung cấp."),
                errors);
    }

    @Test
    void codeAllowsOnlyUnaccentedLettersDigitsAndSeparators() {
        assertTrue(supplierService.validateFields(supplier("NCC Á", "Tên", null, null, null)).containsKey("code"));
        assertTrue(supplierService.validateFields(supplier("N".repeat(31), "Tên", null, null, null)).containsKey("code"));
        assertFalse(supplierService.validateFields(supplier("NCC_01.HN", "Tên", null, null, null)).containsKey("code"));
    }

    @Test
    void taxCodeMustBeTenDigitsWithOptionalBranchSuffix() {
        assertFalse(supplierService.validateFields(supplier("A", "Tên", "0101234567", null, null)).containsKey("taxCode"));
        assertFalse(supplierService.validateFields(supplier("A", "Tên", "0101234567-001", null, null))
                .containsKey("taxCode"));
        for (String taxCode : new String[] {"010123456", "01012345678", "0101234567-01", "MST0101234"}) {
            assertTrue(supplierService.validateFields(supplier("A", "Tên", taxCode, null, null)).containsKey("taxCode"),
                    taxCode);
        }
    }

    @Test
    void phoneAndEmailAreCheckedWhenProvided() {
        Map<String, String> errors = supplierService.validateFields(supplier("A", "Tên", null, "12345", "khong-hop-le"));
        assertTrue(errors.containsKey("phone"));
        assertTrue(errors.containsKey("email"));
        assertFalse(supplierService.validateFields(supplier("A", "Tên", null, "+862112345678", null))
                .containsKey("phone"));
    }

    @Test
    void longTextFieldsAreLimited() {
        Supplier tooLong = new Supplier(null, "A", "T".repeat(201), null, "C".repeat(151), null, null,
                "D".repeat(301), "P".repeat(201), Supplier.ACTIVE, false);
        assertEquals(Set.of("name", "contactName", "address", "paymentTerms"),
                supplierService.validateFields(tooLong).keySet());
    }
}
