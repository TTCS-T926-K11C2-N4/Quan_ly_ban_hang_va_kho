package com.oms.service;

import com.oms.model.WarehouseLocation;
import com.oms.model.WarehouseLocationForm;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseLocationServiceTest {

    private static WarehouseLocation location(long id, Long parentId, String code, String type) {
        return new WarehouseLocation(id, 1, parentId, code, null, type, 0, true, 0);
    }

    // S5-03 AC2: vị trí ở mức khu hoặc kệ; kệ nằm dưới khu chứa nó, kệ đứng riêng ở gốc cây
    @Test
    void buildsZoneRackTree() {
        List<WarehouseLocation> tree = WarehouseLocationService.buildTree(List.of(
                location(1, null, "A", WarehouseLocation.ZONE),
                location(2, 1L, "A-01", WarehouseLocation.RACK),
                location(3, 1L, "A-02", WarehouseLocation.RACK),
                location(4, null, "R-99", WarehouseLocation.RACK),
                location(5, 99L, "X-01", WarehouseLocation.RACK)));
        assertEquals(List.of("A", "R-99", "X-01"), tree.stream().map(WarehouseLocation::getCode).toList());
        assertEquals(List.of("A-01", "A-02"), tree.get(0).getChildren().stream().map(WarehouseLocation::getCode).toList());
    }

    @Test
    void zoneCannotHaveParentAndCodeIsRequired() {
        Map<String, String> errors = WarehouseLocationService.validateFields(
                new WarehouseLocationForm(null, WarehouseLocation.ZONE, "1", null, null, "abc"));
        assertEquals("Vui lòng nhập mã vị trí.", errors.get("code"));
        assertEquals("Khu không nằm trong khu khác; chỉ kệ mới chọn khu chứa.", errors.get("parentId"));
        assertEquals("Thứ tự soạn hàng là số nguyên từ 0 đến 99999.", errors.get("pickSequence"));
    }

    @Test
    void acceptsRackInZone() {
        assertTrue(WarehouseLocationService.validateFields(
                new WarehouseLocationForm(null, WarehouseLocation.RACK, "1", "A-01-02", "Kệ bia", null)).isEmpty());
        assertEquals(0, WarehouseLocationService.parsePickSequence(null));
        assertNull(WarehouseLocationService.parsePickSequence("100000"));
    }

    @Test
    void cannotDeactivateZoneWithActiveRackOrLocationInUse() {
        WarehouseLocationService service = new WarehouseLocationService();
        WarehouseLocation zone = location(1, null, "A", WarehouseLocation.ZONE);
        List<WarehouseLocation> tree = WarehouseLocationService.buildTree(List.of(zone,
                location(2, 1L, "A-01", WarehouseLocation.RACK)));
        assertEquals("Khu A còn kệ đang dùng; ngừng dùng các kệ trong khu trước.",
                service.validateDeactivate(zone, tree));
        WarehouseLocation used = new WarehouseLocation(3, 1, null, "B-01", null, WarehouseLocation.RACK, 0, true, 2);
        assertEquals("Vị trí B-01 đang là vị trí lưu chính của 2 SKU nên chưa ngừng dùng được.",
                service.validateDeactivate(used, tree));
    }
}
