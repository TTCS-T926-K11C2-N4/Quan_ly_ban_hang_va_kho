package com.oms.security;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Đếm số lần sai và khóa tạm cho tên đăng nhập KHÔNG tồn tại, giống hệt tài khoản thật (đếm trong users),
// để thông báo "đã nhập sai n/5 lần" không làm lộ tài khoản có tồn tại hay không (S1-01).
// Lưu trong bộ nhớ: khởi động lại Tomcat thì đếm lại từ đầu.
public final class UnknownLoginAttempts {

    // Chặn việc gõ hàng loạt tên ngẫu nhiên làm đầy bộ nhớ
    private static final int MAX_ENTRIES = 10_000;

    private static final Map<String, Attempt> ATTEMPTS = new ConcurrentHashMap<>();

    private UnknownLoginAttempts() {
    }

    // Số giây còn bị khóa tạm; 0 nếu không bị khóa
    public static long lockSeconds(String identifier) {
        Attempt attempt = ATTEMPTS.get(key(identifier));
        if (attempt == null || attempt.lockedUntil == null) {
            return 0;
        }
        long seconds = attempt.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(seconds, 0);
    }

    // Trả về số lần sai liên tiếp sau lần này; 0 nghĩa là vừa đủ maxFailed lần và bị khóa tạm lockMinutes phút.
    // Giống users.failed_login_count: khóa xong thì đếm lại từ đầu.
    public static int recordFailure(String identifier, int maxFailed, int lockMinutes) {
        if (ATTEMPTS.size() >= MAX_ENTRIES) {
            ATTEMPTS.values().removeIf(attempt -> attempt.lockedUntil == null
                    || attempt.lockedUntil.isBefore(Instant.now()));
        }
        Attempt updated = ATTEMPTS.compute(key(identifier), (name, attempt) -> {
            int count = (attempt == null ? 0 : attempt.failedCount) + 1;
            return count >= maxFailed
                    ? new Attempt(0, Instant.now().plusSeconds(lockMinutes * 60L))
                    : new Attempt(count, attempt == null ? null : attempt.lockedUntil);
        });
        return updated.failedCount;
    }

    // Tài khoản thật so khớp email không phân biệt hoa thường nên ở đây cũng vậy
    private static String key(String identifier) {
        return identifier.toLowerCase(Locale.ROOT);
    }

    private record Attempt(int failedCount, Instant lockedUntil) {
    }
}
