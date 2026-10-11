package com.oms.service;

import com.oms.model.CustomerForm;
import com.oms.model.CustomerProfile;
import com.oms.model.SelectOption;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Kiểm tra form hồ sơ đại lý ở chế độ sửa (không tra mã trùng trong CSDL)
class CustomerProfileServiceTest {

    private static final CustomerProfileService.Options OPTIONS = new CustomerProfileService.Options(
            List.of(new SelectOption(1, "DL1", "Đại lý cấp 1")), List.of(new SelectOption(2, "HN", "Hà Nội")),
            List.of(new SelectOption(3, "KHO-HN", "Kho Hà Nội")), List.of(new SelectOption(9, null, "Phạm Minh Châu")));

    // Đại lý đang do nhân viên id 30 (đã nghỉ, không còn trong danh sách đang hoạt động) phụ trách
    private static final CustomerProfile EDITING = new CustomerProfile(5, "DL00125", "Đại lý Minh Anh", null, null, null,
            null, 1, "Đại lý cấp 1", 2, "Hà Nội", 30L, "Nguyễn Văn Thử", 3L, "Kho Hà Nội", "ACTIVE", false, true, 4);

    private static CustomerForm form(String name, String taxCode, String phone, String email, String groupId,
                                     String salesRepId) {
        return new CustomerForm(5L, "dl00999", name, taxCode, phone, email, null, groupId, "2", salesRepId, "3",
                "ACTIVE", "4");
    }

    private final CustomerProfileService service = new CustomerProfileService();

    // S3-03 AC1: tên, nhóm khách hàng, khu vực, kho bắt buộc; MST, SĐT, email không bắt buộc nhưng phải đúng định dạng
    @Test
    void validatesProfileFields() throws SQLException {
        Map<String, String> errors = service.validate(form(null, "12345", "0903", "abc", null, null), EDITING,
                false, null, OPTIONS).errors();
        assertEquals("Vui lòng nhập tên đại lý.", errors.get("name"));
        assertEquals("Mã số thuế gồm 10 số, 10 số kèm -3 số chi nhánh, hoặc 12 số.", errors.get("taxCode"));
        assertEquals("Số điện thoại gồm đúng 10 chữ số.", errors.get("phone"));
        assertEquals("Email không hợp lệ.", errors.get("email"));
        assertEquals("Chọn nhóm khách hàng.", errors.get("customerGroupId"));
    }

    // S3-03 AC2: mã đại lý không đổi khi sửa dù form gửi mã khác
    @Test
    void keepsCodeWhenEditing() throws SQLException {
        CustomerProfileService.Checked checked = service.validate(form("Đại lý Minh Anh", "0107456789-001",
                "0903123456", "a@b.vn", "1", null), EDITING, false, null, OPTIONS);
        assertTrue(checked.isValid());
        assertEquals("DL00125", checked.values().code());
    }

    // Không có quyền phân công thì giữ nguyên người phụ trách; có quyền thì giữ được người cũ đã nghỉ
    // nhưng không chọn được người không hoạt động khác
    @Test
    void salesRepFollowsAssignPermission() throws SQLException {
        assertEquals(30L, service.validate(form("A", null, null, null, "1", "9"), EDITING, false, null, OPTIONS)
                .values().salesRepId());
        assertEquals(30L, service.validate(form("A", null, null, null, "1", "30"), EDITING, true, null, OPTIONS)
                .values().salesRepId());
        assertEquals(9L, service.validate(form("A", null, null, null, "1", "9"), EDITING, true, null, OPTIONS)
                .values().salesRepId());
        assertEquals("Người phụ trách phải là nhân viên kinh doanh đang hoạt động.",
                service.validate(form("A", null, null, null, "1", "77"), EDITING, true, null, OPTIONS).errors()
                        .get("salesRepId"));
    }
}
