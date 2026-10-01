package com.oms.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// S2-04: giá trị trước/sau ghi vào cột JSON của audit_logs
class JsonUtilTest {

    @Test
    void writesStringsNumbersBooleansAndNull() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sku", "COCA-330");
        values.put("quantity", 120);
        values.put("price", new BigDecimal("9500.50"));
        values.put("locked", true);
        values.put("note", null);
        assertEquals("{\"sku\":\"COCA-330\",\"quantity\":120,\"price\":9500.50,\"locked\":true,\"note\":null}",
                JsonUtil.toJson(values));
    }

    @Test
    void escapesQuotesBackslashesAndControlCharacters() {
        assertEquals("{\"reason\":\"Hàng \\\"hỏng\\\" \\\\ vỡ\\nlô 2\\u0001\"}",
                JsonUtil.toJson(Map.of("reason", "Hàng \"hỏng\" \\ vỡ\nlô 2\u0001")));
    }

    @Test
    void emptyOrNullMapGivesNull() {
        assertNull(JsonUtil.toJson(null));
        assertNull(JsonUtil.toJson(new HashMap<>()));
    }
}
