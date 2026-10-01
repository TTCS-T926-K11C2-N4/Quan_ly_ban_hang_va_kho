package com.oms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// S2-02: số di động Việt Nam nhận 0xxx hoặc +84xxx, lưu thống nhất 10 chữ số
class PhoneUtilTest {

    @Test
    void acceptsLocalAndInternationalForms() {
        assertEquals("0912345678", PhoneUtil.normalizeVietnamMobile("0912345678"));
        assertEquals("0912345678", PhoneUtil.normalizeVietnamMobile("+84912345678"));
        assertEquals("0912345678", PhoneUtil.normalizeVietnamMobile("+84 912 345 678"));
        assertEquals("0987123456", PhoneUtil.normalizeVietnamMobile("0987.123.456"));
        assertEquals("0338123456", PhoneUtil.normalizeVietnamMobile("84338123456"));
    }

    @Test
    void rejectsWrongLengthOrPrefix() {
        assertNull(PhoneUtil.normalizeVietnamMobile("091234567"), "9 số");
        assertNull(PhoneUtil.normalizeVietnamMobile("09123456789"), "11 số");
        assertNull(PhoneUtil.normalizeVietnamMobile("0212345678"), "đầu số cố định 02");
        assertNull(PhoneUtil.normalizeVietnamMobile("0612345678"), "đầu số 06 không phải di động");
        assertNull(PhoneUtil.normalizeVietnamMobile("09123abc78"));
        assertNull(PhoneUtil.normalizeVietnamMobile(null));
    }
}
