package com.oms.util;

import java.util.regex.Pattern;

// Số điện thoại di động Việt Nam (S2-02): 10 số, đầu 03/05/07/08/09
public final class PhoneUtil {

    private static final Pattern VIETNAM_MOBILE = Pattern.compile("0[35789]\\d{8}");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-]");

    private PhoneUtil() {
    }

    // Người dùng hay gõ "0912 345 678", "0912.345.678" hoặc "+84912345678": đưa về dạng 0912345678 để lưu
    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = SEPARATORS.matcher(phone).replaceAll("");
        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        }
        return digits.isEmpty() ? null : digits;
    }

    // Kiểm tra số đã qua normalize
    public static boolean isVietnamMobile(String phone) {
        return phone != null && VIETNAM_MOBILE.matcher(phone).matches();
    }
}
