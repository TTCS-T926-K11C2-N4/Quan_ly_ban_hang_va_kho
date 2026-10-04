package com.oms.service;

import com.oms.model.Supplier;
import com.oms.model.SupplierForm;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierServiceTest {

    private final SupplierService service = new SupplierService();

    // S2-09 AC1: đủ mã, tên, mã số thuế, người liên hệ, điều khoản thanh toán. Các ô sai định dạng báo lỗi
    // trước khi hỏi CSDL nên test chạy không cần MySQL.
    @Test
    void reportsEveryInvalidField() throws SQLException {
        SupplierForm form = new SupplierForm("X", null, "12345", null, "TT 100 ngày", "PAUSED");
        Map<String, String> errors = service.validate(form, null);
        assertEquals(Map.of(
                "code", "Mã gồm 2–30 ký tự: chữ không dấu, số, gạch dưới hoặc gạch ngang.",
                "name", "Vui lòng nhập tên nhà cung cấp.",
                "taxCode", "Mã số thuế gồm 10 chữ số, hoặc thêm mã chi nhánh 3 chữ số (vd 0101234567-001).",
                "contactName", "Vui lòng nhập người liên hệ.",
                "paymentTerms", "Điều khoản thanh toán không hợp lệ.",
                "status", "Vui lòng chọn trạng thái."), errors);
    }

    @Test
    void requiredFieldsHaveOwnMessages() throws SQLException {
        Map<String, String> errors = service.validate(new SupplierForm(null, null, null, null, null, null), null);
        assertEquals("Vui lòng nhập mã nhà cung cấp.", errors.get("code"));
        assertEquals("Vui lòng nhập mã số thuế.", errors.get("taxCode"));
        assertEquals("Vui lòng chọn điều khoản thanh toán.", errors.get("paymentTerms"));
    }

    // AC2: đã có phiếu nhập thì không xoá được, chỉ ngừng giao dịch
    @Test
    void supplierWithReceiptsCannotBeDeleted() {
        Supplier used = new Supplier(1, "NCC001", "An Phát", "0101234567", "A", "TT 30 ngày", Supplier.ACTIVE, true);
        Supplier unused = new Supplier(2, "NCC002", "Hoàng Gia", "0109876543", "B", "TT 15 ngày", Supplier.ACTIVE, false);
        assertTrue(service.validateDelete(used).contains("chỉ ngừng giao dịch"));
        assertNull(service.validateDelete(unused));
    }
}
