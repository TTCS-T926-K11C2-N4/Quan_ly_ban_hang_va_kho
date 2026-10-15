package com.oms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Code128Test {

    // Ví dụ chuẩn của Code 128B: "A" = Start B (104) + 'A' (33) + kiểm tra (104 + 33 = 137 mod 103 = 34) + Stop (106)
    @Test
    void encodesStartCharactersChecksumAndStop() {
        assertArrayEquals(new int[] {104, 33, 34, 106}, Code128.encode("A"));
    }

    // Mỗi ký hiệu rộng đúng 11 module, ký hiệu dừng 13: tổng = 11 * (số ký hiệu thường) + 13
    @Test
    void symbolWidthsAreValid() {
        String orderNo = "DH261014-0021";
        int[] values = Code128.encode(orderNo);
        assertEquals(11 * (values.length - 1) + 13, Code128.modules(values));
    }

    @Test
    void rendersSvgWithBars() {
        String svg = Code128.svg("DH261014-0021", 40);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("<rect"));
        assertTrue(svg.endsWith("</svg>"));
    }

    @Test
    void rejectsNonPrintableCharacters() {
        assertThrows(IllegalArgumentException.class, () -> Code128.encode("Đơn"));
    }
}
