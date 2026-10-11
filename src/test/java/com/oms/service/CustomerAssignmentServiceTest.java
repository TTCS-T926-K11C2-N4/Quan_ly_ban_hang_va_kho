package com.oms.service;

import com.oms.model.SelectOption;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerAssignmentServiceTest {

    // Nhân viên kinh doanh đang hoạt động: id 2 phụ trách Hà Nội, id 9 chưa có địa bàn
    private static final List<SelectOption> ACTIVE_REPS = List.of(
            new SelectOption(2, "Hà Nội", "Hoàng Văn Thái"), new SelectOption(9, null, "Phạm Minh Châu"));

    private final CustomerAssignmentService service = new CustomerAssignmentService();

    // S3-06 AC1: phải chọn đại lý và một nhân viên kinh doanh đang hoạt động; lý do không bắt buộc
    @Test
    void assignNeedsCustomersAndActiveSalesRep() {
        Map<String, String> errors = service.validateAssign(List.of(), null, null, ACTIVE_REPS);
        assertEquals("Chọn ít nhất một đại lý để phân công.", errors.get("customerIds"));
        assertEquals("Chọn nhân viên kinh doanh phụ trách.", errors.get("toSalesRepId"));

        assertEquals("Nhân viên này không còn là nhân viên kinh doanh đang hoạt động.",
                service.validateAssign(List.of(5L), 99L, null, ACTIVE_REPS).get("toSalesRepId"));
        assertTrue(service.validateAssign(List.of(5L, 6L), 2L, null, ACTIVE_REPS).isEmpty());
    }

    @Test
    void assignLimitsSelectionSize() {
        List<Long> tooMany = Collections.nCopies(CustomerAssignmentService.MAX_SELECTED + 1, 1L);
        assertEquals("Mỗi lần phân công tối đa 200 đại lý.",
                service.validateAssign(tooMany, 2L, null, ACTIVE_REPS).get("customerIds"));
    }

    // S3-06 AC3: chuyển giao hàng loạt bắt buộc lý do, người nhận khác người giao và đang hoạt động
    @Test
    void transferNeedsReasonAndDifferentActiveReceiver() {
        Map<String, String> errors = service.validateTransfer(2L, 2L, null, ACTIVE_REPS);
        assertEquals("Nhân viên nhận phải khác nhân viên bàn giao.", errors.get("toSalesRepId"));
        assertEquals("Vui lòng nhập lý do chuyển giao.", errors.get("reason"));

        assertEquals("Nhân viên nhận phải là nhân viên kinh doanh đang hoạt động.",
                service.validateTransfer(2L, 99L, "Nghỉ việc", ACTIVE_REPS).get("toSalesRepId"));
        // Người bàn giao đã nghỉ (không còn trong danh sách đang hoạt động) vẫn chuyển giao được
        assertTrue(service.validateTransfer(30L, 9L, "Nghỉ việc", ACTIVE_REPS).isEmpty());
    }

    // Nhật ký ghi tên người phụ trách thay vì mã để đọc được ngay trên trang Nhật ký
    @Test
    void auditValuesUseSalesRepNames() {
        Map<String, Object> values = CustomerAssignmentService.values("DL001", "Hoàng Văn Thái");
        assertEquals("DL001", values.get("code"));
        assertEquals("Hoàng Văn Thái", values.get("salesRep"));
        assertNull(CustomerAssignmentService.values("DL001", null).get("salesRep"));
    }
}
