package com.oms.model;

public class LoginResult {

    public enum Outcome {
        SUCCESS,
        // Sai tên đăng nhập hoặc mật khẩu: không cho biết là sai cái nào (S1-01)
        INVALID,
        // Khóa tạm do nhập sai nhiều lần
        TEMP_LOCKED,
        PENDING,
        LOCKED
    }

    private final Outcome outcome;
    private final long userId;
    private final int failedCount;
    private final int maxFailedLogins;
    private final int lockMinutes;
    private final long lockSeconds;

    private LoginResult(Outcome outcome, long userId, int failedCount, int maxFailedLogins, int lockMinutes,
                        long lockSeconds) {
        this.outcome = outcome;
        this.userId = userId;
        this.failedCount = failedCount;
        this.maxFailedLogins = maxFailedLogins;
        this.lockMinutes = lockMinutes;
        this.lockSeconds = lockSeconds;
    }

    public static LoginResult success(long userId) {
        return new LoginResult(Outcome.SUCCESS, userId, 0, 0, 0, 0);
    }

    // failedCount: số lần sai liên tiếp tính cả lần này
    public static LoginResult invalid(int failedCount, int maxFailedLogins, int lockMinutes) {
        return new LoginResult(Outcome.INVALID, 0, failedCount, maxFailedLogins, lockMinutes, 0);
    }

    public static LoginResult tempLocked(int maxFailedLogins, long lockSeconds) {
        return new LoginResult(Outcome.TEMP_LOCKED, 0, 0, maxFailedLogins, 0, lockSeconds);
    }

    public static LoginResult of(Outcome outcome) {
        return new LoginResult(outcome, 0, 0, 0, 0, 0);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public long getUserId() {
        return userId;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getMaxFailedLogins() {
        return maxFailedLogins;
    }

    public int getLockMinutes() {
        return lockMinutes;
    }

    public long getLockSeconds() {
        return lockSeconds;
    }
}
