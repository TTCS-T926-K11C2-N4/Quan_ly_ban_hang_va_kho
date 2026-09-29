package com.oms.model;

// Dữ liệu cần để kiểm tra một lần đăng nhập
public class LoginAccount {

    private final long id;
    private final String passwordHash;
    private final String status;
    private final int failedCount;
    private final long lockSeconds;

    public LoginAccount(long id, String passwordHash, String status, int failedCount, long lockSeconds) {
        this.id = id;
        this.passwordHash = passwordHash;
        this.status = status;
        this.failedCount = failedCount;
        this.lockSeconds = lockSeconds;
    }

    public long getId() {
        return id;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    // Giá trị users.status: PENDING | ACTIVE | LOCKED
    public String getStatus() {
        return status;
    }

    // Số lần nhập sai mật khẩu liên tiếp (users.failed_login_count)
    public int getFailedCount() {
        return failedCount;
    }

    // Số giây còn bị khóa tạm do nhập sai mật khẩu nhiều lần; 0 nếu không bị khóa tạm
    public long getLockSeconds() {
        return lockSeconds;
    }
}
