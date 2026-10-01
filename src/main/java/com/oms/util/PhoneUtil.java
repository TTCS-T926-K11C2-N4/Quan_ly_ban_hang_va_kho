package com.oms.util;

import java.util.regex.Pattern;

// Số điện thoại di động Việt Nam (S2-02): nhận 0xxxxxxxxx hoặc +84xxxxxxxxx, có thể có khoảng trắng, dấu chấm, gạch ngang
public final class PhoneUtil {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-()]");
    // Đầu số di động hiện hành: 03, 05, 07, 08, 09
    private static final Pattern VIETNAM_MOBILE = Pattern.compile("0[35789]\\d{8}");

    private PhoneUtil() {
    }

    // Trả về dạng lưu thống nhất 0xxxxxxxxx (10 chữ số); null nếu không phải số di động Việt Nam hợp lệ
    public static String normalizeVietnamMobile(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = SEPARATORS.matcher(raw.trim()).replaceAll("");
        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        return VIETNAM_MOBILE.matcher(digits).matches() ? digits : null;
    }
}
