package com.oms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S2-02: kiểm tra định dạng số điện thoại Việt Nam
class PhoneUtilTest {

    @Test
    void normalizeRemovesSeparatorsAndCountryCode() {
        assertEquals("0912345678", PhoneUtil.normalize("0912 345 678"));
        assertEquals("0912345678", PhoneUtil.normalize("0912.345.678"));
        assertEquals("0912345678", PhoneUtil.normalize("0912-345-678"));
        assertEquals("0912345678", PhoneUtil.normalize("+84912345678"));
        assertEquals("0912345678", PhoneUtil.normalize("+84 912 345 678"));
        assertNull(PhoneUtil.normalize(null));
        assertNull(PhoneUtil.normalize(" "));
    }

    @Test
    void acceptsVietnamMobilePrefixes() {
        for (String phone : new String[] {"0312345678", "0512345678", "0712345678", "0812345678", "0912345678"}) {
            assertTrue(PhoneUtil.isVietnamMobile(phone), phone);
        }
    }

    @Test
    void rejectsWrongLengthPrefixOrCharacters() {
        for (String phone : new String[] {"091234567", "09123456789", "0112345678", "0212345678", "0612345678",
                "1912345678", "09123a5678", "+84912345678", ""}) {
            assertFalse(PhoneUtil.isVietnamMobile(phone), phone);
        }
        assertFalse(PhoneUtil.isVietnamMobile(null));
    }
}
