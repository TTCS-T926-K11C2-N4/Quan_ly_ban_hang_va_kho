package com.oms.service;

import com.oms.model.ProfileForm;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S2-02: kiểm tra dữ liệu người dùng tự sửa trên trang hồ sơ cá nhân
class ProfileServiceTest {

    private final ProfileService profileService = new ProfileService();

    @Test
    void validFormHasNoErrors() {
        assertTrue(profileService.validate(new ProfileForm("Nguyễn Văn An", "0912345678")).isEmpty());
    }

    @Test
    void fullNameAndPhoneAreRequired() {
        Map<String, String> errors = profileService.validate(new ProfileForm(null, null));
        assertEquals("Vui lòng nhập họ và tên.", errors.get("fullName"));
        assertEquals("Vui lòng nhập số điện thoại.", errors.get("phone"));
    }

    @Test
    void fullNameLongerThan150CharactersIsRejected() {
        Map<String, String> errors = profileService.validate(new ProfileForm("a".repeat(151), "0912345678"));
        assertTrue(errors.containsKey("fullName"));
        assertTrue(profileService.validate(new ProfileForm("a".repeat(150), "0912345678")).isEmpty());
    }

    @Test
    void nonVietnamesePhoneIsRejected() {
        Map<String, String> errors = profileService.validate(new ProfileForm("Nguyễn Văn An", "0212345678"));
        assertEquals(Map.of("phone",
                "Số điện thoại không đúng định dạng Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09)."), errors);
    }
}
