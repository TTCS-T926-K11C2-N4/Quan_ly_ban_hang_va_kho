package com.oms.service;

import com.oms.model.Warehouse;
import com.oms.model.WarehouseForm;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseServiceTest {

    private static WarehouseForm form(String code, String name, String address, String manager, String type,
                                      String status) {
        return new WarehouseForm(code, name, null, address, manager, type, status);
    }

    // S5-03 AC1: mỗi kho có mã, tên, địa chỉ, trạng thái (người phụ trách không bắt buộc)
    @Test
    void requiresCodeNameAddressTypeAndStatus() {
        Map<String, String> errors = WarehouseService.validateFields(form(null, null, null, null, null, null));
        assertEquals(Map.of(
                "code", "Vui lòng nhập mã kho.",
                "name", "Vui lòng nhập tên kho.",
                "address", "Vui lòng nhập địa chỉ kho.",
                "type", "Vui lòng chọn loại kho.",
                "status", "Vui lòng chọn trạng thái."), errors);
    }

    @Test
    void acceptsValidWarehouseWithoutManager() {
        assertTrue(WarehouseService.validateFields(form("KHO-05", "Kho Hải Phòng", "Lê Chân, Hải Phòng", null,
                Warehouse.SELLABLE, Warehouse.ACTIVE)).isEmpty());
    }

    @Test
    void rejectsBadCodeAndManager() {
        Map<String, String> errors = WarehouseService.validateFields(form("KHO 05", "Kho", "Hà Nội", "abc",
                Warehouse.DEFECT, Warehouse.INACTIVE));
        assertEquals("Mã kho gồm 2–30 ký tự: chữ không dấu, số, gạch dưới hoặc gạch ngang.", errors.get("code"));
        assertEquals("Người phụ trách không hợp lệ.", errors.get("managerUserId"));
        assertEquals(2, errors.size());
    }

    @Test
    void labelsFollowDesign() {
        Warehouse defect = new Warehouse(2, "KHO-LOI", "Kho hàng lỗi", "Khu cách ly hàng lỗi", "Gia Lâm, Hà Nội",
                null, null, Warehouse.DEFECT, Warehouse.INACTIVE, 0, false);
        assertEquals("Hàng lỗi", defect.getTypeLabel());
        assertEquals("Tạm ngừng", defect.getStatusLabel());
        assertEquals("[SELLABLE, DEFECT]", Warehouse.TYPE_LABELS.keySet().toString());
    }
}
