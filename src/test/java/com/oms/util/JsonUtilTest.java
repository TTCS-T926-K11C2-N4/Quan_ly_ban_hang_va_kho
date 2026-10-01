package com.oms.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonUtilTest {

    @Test
    void buildsObjectWithEscapedStringsNumbersNullAndLists() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Bia \"lon\"\nmới");
        values.put("parentId", null);
        values.put("level", 2);
        values.put("ids", List.of(1L, 2L));
        assertEquals("{\"name\":\"Bia \\\"lon\\\"\\nmới\",\"parentId\":null,\"level\":2,\"ids\":[1,2]}",
                JsonUtil.object(values));
    }
}
