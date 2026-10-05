package com.oms.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S2-01: chuẩn hoá dữ liệu đọc từ Excel trước khi kiểm tra
class AccountImportServiceTest {

    @Test
    void phoneKeepsTenDigitsAndRestoresLeadingZero() {
        assertEquals("0987123456", AccountImportService.normalizePhone("0987 123 456"));
        assertEquals("0987123456", AccountImportService.normalizePhone("0987.123-456"));
        // Excel lưu số điện thoại dạng Number sẽ mất số 0 đầu
        assertEquals("0987123456", AccountImportService.normalizePhone("987123456"));
        // Sai độ dài thì để nguyên cho bước kiểm tra báo lỗi
        assertEquals("12345", AccountImportService.normalizePhone("12345"));
        assertEquals("", AccountImportService.normalizePhone(""));
    }

    @Test
    void rolesSplitByCommaOrSemicolonWithoutDuplicates() {
        assertEquals(List.of("Nhân viên kho", "ACCOUNTANT"),
                AccountImportService.splitRoles(" Nhân viên kho ; ACCOUNTANT, Nhân viên kho ,"));
        assertEquals(List.of(), AccountImportService.splitRoles(""));
    }

    // Dòng mẫu của tệp mẫu (tên đăng nhập "vd....") bị bỏ qua khi nhập, kể cả viết hoa
    @Test
    void sampleRowsAreRecognisedByUsernamePrefix() {
        assertTrue(AccountImportService.isSampleRow(List.of("Nguyễn Văn A", "vd.nguyenvana", "", "", "", "", "")));
        assertTrue(AccountImportService.isSampleRow(List.of("", "VD.TranThiB", "", "", "", "", "")));
        assertFalse(AccountImportService.isSampleRow(List.of("Vũ Đức", "vuduc", "", "", "", "", "")));
        assertFalse(AccountImportService.isSampleRow(List.of("", "", "", "", "", "", "")));
    }
}
