package com.oms.util;

public final class NameUtil {

    private NameUtil() {
    }

    // Chữ cái đầu của họ và tên, vd "Nguyễn Đức Mạnh" -> "NM"
    public static String initials(String fullName) {
        String[] words = fullName.trim().split("\\s+");
        String first = words[0].substring(0, 1);
        String last = words.length > 1 ? words[words.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
