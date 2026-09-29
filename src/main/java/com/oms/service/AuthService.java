package com.oms.service;

import com.oms.dao.PermissionDao;
import com.oms.dao.SystemSettingDao;
import com.oms.dao.UserDao;
import com.oms.model.LoginAccount;
import com.oms.model.LoginResult;
import com.oms.model.SessionUser;
import com.oms.security.UnknownLoginAttempts;
import com.oms.util.PasswordUtil;

import java.sql.SQLException;

// Đăng nhập (S1-01), đổi mật khẩu (S1-04), quên và đặt lại mật khẩu qua email (S1-03)
public class AuthService {

    // Giá trị dùng khi system_settings chưa có khóa tương ứng
    private static final int DEFAULT_MAX_FAILED_LOGINS = 5;
    private static final int DEFAULT_LOCK_MINUTES = 15;

    // Tài khoản không tồn tại vẫn chạy bcrypt một lần để thời gian phản hồi không để lộ tài khoản có tồn tại hay không
    private static final String DUMMY_HASH = PasswordUtil.hash("khong-phai-mat-khau-that");

    private final UserDao userDao = new UserDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final SystemSettingDao settingDao = new SystemSettingDao();

    // Tên đăng nhập không tồn tại đi qua đúng các bước như tài khoản thật (kiểm khóa tạm, chạy bcrypt, đếm số lần
    // sai) để thông báo và thời gian phản hồi không để lộ tài khoản có tồn tại hay không (S1-01)
    public LoginResult login(String identifier, String password) throws SQLException {
        int maxFailed = settingDao.getInt("auth.max_failed_logins", DEFAULT_MAX_FAILED_LOGINS);
        int lockMinutes = settingDao.getInt("auth.lock_minutes", DEFAULT_LOCK_MINUTES);

        LoginAccount account = userDao.findLoginAccount(identifier);
        if (account == null) {
            long lockSeconds = UnknownLoginAttempts.lockSeconds(identifier);
            if (lockSeconds > 0) {
                return LoginResult.tempLocked(maxFailed, lockSeconds);
            }
            PasswordUtil.matches(password, DUMMY_HASH);
            int failedCount = UnknownLoginAttempts.recordFailure(identifier, maxFailed, lockMinutes);
            return failedCount == 0
                    ? LoginResult.tempLocked(maxFailed, UnknownLoginAttempts.lockSeconds(identifier))
                    : LoginResult.invalid(failedCount, maxFailed, lockMinutes);
        }
        // Đang khóa tạm thì không kiểm tra mật khẩu, để không dò tiếp được trong thời gian khóa
        if (account.getLockSeconds() > 0) {
            return LoginResult.tempLocked(maxFailed, account.getLockSeconds());
        }
        if (!PasswordUtil.matches(password, account.getPasswordHash())) {
            userDao.recordFailedLogin(account.getId(), maxFailed, lockMinutes);
            LoginAccount updated = userDao.findLoginAccount(identifier);
            return updated.getLockSeconds() > 0
                    ? LoginResult.tempLocked(maxFailed, updated.getLockSeconds())
                    : LoginResult.invalid(updated.getFailedCount(), maxFailed, lockMinutes);
        }
        // Chỉ báo trạng thái chờ duyệt/bị khóa khi đã nhập đúng mật khẩu
        if ("PENDING".equals(account.getStatus())) {
            return LoginResult.of(LoginResult.Outcome.PENDING);
        }
        if ("LOCKED".equals(account.getStatus())) {
            return LoginResult.of(LoginResult.Outcome.LOCKED);
        }
        userDao.recordSuccessfulLogin(account.getId());
        return LoginResult.success(account.getId());
    }

    public SessionUser loadSessionUser(long userId) throws SQLException {
        return userDao.findSessionUser(userId, permissionDao.findCodesByUserId(userId));
    }
}
