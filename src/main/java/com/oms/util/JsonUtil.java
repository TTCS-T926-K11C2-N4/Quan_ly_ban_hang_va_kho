package com.oms.util;

import java.util.Map;

// Ghi giá trị trước/sau vào cột JSON của audit_logs mà không cần thêm thư viện JSON.
// Chỉ hỗ trợ một tầng: khoá là chuỗi, giá trị là chuỗi, số, true/false hoặc null; kiểu khác ghi bằng toString().
public final class JsonUtil {

    private JsonUtil() {
    }

    // Trả về null khi map null hoặc rỗng để cột JSON để trống
    public static String toJson(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (json.length() > 1) {
                json.append(',');
            }
            appendString(json, entry.getKey());
            json.append(':');
            Object value = entry.getValue();
            if (value == null) {
                json.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                appendString(json, value.toString());
            }
        }
        return json.append('}').toString();
    }

    private static void appendString(StringBuilder json, String value) {
        json.append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        json.append('"');
    }
}
