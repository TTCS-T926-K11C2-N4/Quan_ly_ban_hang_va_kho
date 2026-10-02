package com.oms.util;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

// Dựng chuỗi JSON nhỏ cho cột old_values/new_values của audit_logs, không cần thêm thư viện JSON
public final class JsonUtil {

    private JsonUtil() {
    }

    // Giá trị nhận: String, Number, Boolean, null, Collection và Map (khoá đổi thành chuỗi) của các loại đó
    public static String object(Map<String, ?> fields) {
        return fields.entrySet().stream()
                .map(entry -> quote(entry.getKey()) + ":" + value(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Collection<?> items) {
            return items.stream().map(JsonUtil::value).collect(Collectors.joining(",", "[", "]"));
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> quote(String.valueOf(entry.getKey())) + ":" + value(entry.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        return quote(value.toString());
    }

    static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
