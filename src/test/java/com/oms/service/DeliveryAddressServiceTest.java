package com.oms.service;

import com.oms.model.DeliveryAddressEntry;
import com.oms.model.DeliveryAddressForm;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryAddressServiceTest {

    private static DeliveryAddressForm form(String address, String receiver, String phone, String routeNote) {
        return new DeliveryAddressForm(null, null, address, receiver, phone, routeNote, false);
    }

    // S3-04 AC1: địa chỉ, người nhận, số điện thoại bắt buộc; ghi chú đường đi không bắt buộc
    @Test
    void requiresAddressReceiverAndPhone() {
        Map<String, String> errors = new DeliveryAddressService().validate(form(null, null, null, null));
        assertEquals("Vui lòng nhập địa chỉ giao hàng.", errors.get("address"));
        assertEquals("Vui lòng nhập tên người nhận hàng.", errors.get("receiverName"));
        assertEquals("Vui lòng nhập số điện thoại người nhận.", errors.get("receiverPhone"));
        assertEquals(3, errors.size());
    }

    @Test
    void acceptsCompleteAddressWithoutRouteNote() {
        assertTrue(new DeliveryAddressService()
                .validate(form("123 Nguyễn Huệ, Q.1", "Nguyễn Minh Anh", "0903123456", null)).isEmpty());
    }

    @Test
    void phoneMustHaveTenDigits() {
        DeliveryAddressService service = new DeliveryAddressService();
        assertEquals("Số điện thoại gồm đúng 10 chữ số.",
                service.validate(form("A", "B", "090312345", null)).get("receiverPhone"));
        assertEquals("Số điện thoại gồm đúng 10 chữ số.",
                service.validate(form("A", "B", "09031234ab", null)).get("receiverPhone"));
    }

    @Test
    void rejectsTooLongRouteNote() {
        Map<String, String> errors = new DeliveryAddressService()
                .validate(form("A", "B", "0903123456", "x".repeat(DeliveryAddressService.ROUTE_NOTE_MAX_LENGTH + 1)));
        assertEquals("Ghi chú đường đi tối đa 500 ký tự.", errors.get("routeNote"));
    }

    // Nhật ký ghi mã đại lý để trang Nhật ký hiện đúng đại lý sở hữu điểm giao
    @Test
    void auditValuesCarryCustomerCodeAndDefaultFlag() {
        DeliveryAddressEntry entry = new DeliveryAddressEntry(7, 3, "Kho chính", "123 Nguyễn Huệ", "Anh",
                "0903123456", null, true, true, false);
        Map<String, Object> values = DeliveryAddressService.values("DL001", entry);
        assertEquals("DL001", values.get("code"));
        assertEquals("Kho chính", values.get("label"));
        assertEquals(true, values.get("isDefault"));
        assertEquals(true, values.get("active"));
    }
}
