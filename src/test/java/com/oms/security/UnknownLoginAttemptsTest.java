package com.oms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S1-01: tên đăng nhập không tồn tại được đếm và khóa tạm giống tài khoản thật
class UnknownLoginAttemptsTest {

    @Test
    void countsUpAndLocksAtMax() {
        String name = "khong-ton-tai-1";
        for (int expected = 1; expected <= 4; expected++) {
            assertEquals(expected, UnknownLoginAttempts.recordFailure(name, 5, 15));
            assertEquals(0, UnknownLoginAttempts.lockSeconds(name));
        }
        // Lần thứ 5: bị khóa tạm 15 phút và đếm lại từ đầu, giống users.failed_login_count
        assertEquals(0, UnknownLoginAttempts.recordFailure(name, 5, 15));
        long lockSeconds = UnknownLoginAttempts.lockSeconds(name);
        assertTrue(lockSeconds > 14 * 60 && lockSeconds <= 15 * 60, "còn khoảng 15 phút: " + lockSeconds);
    }

    @Test
    void ignoresLetterCase() {
        assertEquals(1, UnknownLoginAttempts.recordFailure("Ai.Do@Example.com", 5, 15));
        assertEquals(2, UnknownLoginAttempts.recordFailure("ai.do@example.com", 5, 15));
    }

    @Test
    void separateNamesCountSeparately() {
        assertEquals(1, UnknownLoginAttempts.recordFailure("ten-a", 5, 15));
        assertEquals(1, UnknownLoginAttempts.recordFailure("ten-b", 5, 15));
    }
}
