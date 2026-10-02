package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.SystemSettingDao;
import com.oms.dao.UserDao;
import com.oms.dao.UserTokenDao;
import com.oms.model.AuditLogEntry;
import com.oms.model.LoginAccount;
import com.oms.model.LoginResult;
import com.oms.model.SessionUser;
import com.oms.security.UnknownLoginAttempts;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;
import com.oms.util.MailSender;
import com.oms.util.PasswordUtil;
import com.oms.util.TokenUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

// Đăng nhập (S1-01), đổi mật khẩu (S1-04), quên và đặt lại mật khẩu qua email (S1-03)
public class AuthService {

    // Giá trị dùng khi system_settings chưa có khóa tương ứng
    private static final int DEFAULT_MAX_FAILED_LOGINS = 5;
    private static final int DEFAULT_LOCK_MINUTES = 15;
    private static final int DEFAULT_RESET_TOKEN_MINUTES = 30;
    // Nhật ký chỉ ghi là có đổi mật khẩu, không lưu mật khẩu hay mã băm
    private static final String PASSWORD_CHANGED_JSON = JsonUtil.object(Map.of("passwordChanged", true));

    // Tài khoản không tồn tại vẫn chạy bcrypt một lần để thời gian phản hồi không để lộ tài khoản có tồn tại hay không
    private static final String DUMMY_HASH = PasswordUtil.hash("khong-phai-mat-khau-that");

    private final UserDao userDao = new UserDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final SystemSettingDao settingDao = new SystemSettingDao();
    private final UserTokenDao userTokenDao = new UserTokenDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

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

    // Trả về thông báo lỗi đầu tiên; null nếu hợp lệ. Câu chữ khớp change-password.js.
    public String validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            return "Vui lòng nhập mật khẩu mới.";
        }
        if (!PasswordUtil.meetsPolicy(newPassword)) {
            return "Mật khẩu mới phải có 8–64 ký tự, gồm cả chữ và số.";
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return "Vui lòng xác nhận mật khẩu mới.";
        }
        if (!confirmPassword.equals(newPassword)) {
            return "Mật khẩu xác nhận không khớp.";
        }
        return null;
    }

    // Trả về thông báo lỗi; null nếu đổi thành công
    public String changePassword(long userId, String currentPassword, String newPassword, String confirmPassword,
                                 String ipAddress) throws SQLException {
        if (currentPassword == null || currentPassword.isEmpty()) {
            return "Vui lòng nhập mật khẩu hiện tại.";
        }
        String error = validateNewPassword(newPassword, confirmPassword);
        if (error != null) {
            return error;
        }
        if (!PasswordUtil.matches(currentPassword, userDao.findPasswordHash(userId))) {
            return "Mật khẩu hiện tại không đúng.";
        }
        if (newPassword.equals(currentPassword)) {
            return "Mật khẩu mới phải khác mật khẩu hiện tại.";
        }

        String passwordHash = PasswordUtil.hash(newPassword);
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                userDao.updatePassword(connection, userId, passwordHash);
                auditLogDao.insertUserAction(connection, userId, userId, AuditLogEntry.PASSWORD_CHANGE, null,
                        PASSWORD_CHANGED_JSON, null, ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
        return null;
    }

    // Email không tồn tại hoặc tài khoản không hoạt động thì im lặng bỏ qua: màn hình vẫn báo "đã gửi" như nhau
    public void requestPasswordReset(String email, String resetPageUrl) throws SQLException {
        Long userId = userDao.findActiveIdByEmail(email);
        if (userId == null) {
            return;
        }
        int validMinutes = settingDao.getInt("auth.reset_token_minutes", DEFAULT_RESET_TOKEN_MINUTES);
        String token = TokenUtil.generate();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                userTokenDao.expireUnusedPasswordResets(connection, userId);
                userTokenDao.insertPasswordReset(connection, userId, TokenUtil.sha256Hex(token), validMinutes);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }

        String link = resetPageUrl + "?token=" + token;
        String body = "Chào bạn,\n\n"
                + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn trên Hệ thống quản lý bán hàng & kho.\n"
                + "Mở liên kết dưới đây để đặt mật khẩu mới. Liên kết có hiệu lực trong " + validMinutes
                + " phút và chỉ dùng được một lần:\n\n"
                + link + "\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này. Mật khẩu hiện tại vẫn giữ nguyên.\n";
        MailSender.sendAsync(email, "Đặt lại mật khẩu", body);
    }

    public boolean isResetTokenValid(String token) throws SQLException {
        return token != null && !token.isEmpty() && userTokenDao.isValidPasswordReset(TokenUtil.sha256Hex(token));
    }

    // Trả về id tài khoản vừa đặt lại mật khẩu; null nếu liên kết không còn hiệu lực.
    // Gọi validateNewPassword trước.
    public Long resetPassword(String token, String newPassword, String ipAddress) throws SQLException {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String passwordHash = PasswordUtil.hash(newPassword);
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Long userId = userTokenDao.consumePasswordReset(connection, TokenUtil.sha256Hex(token));
                if (userId == null) {
                    connection.rollback();
                    return null;
                }
                userDao.updatePassword(connection, userId, passwordHash);
                auditLogDao.insertUserAction(connection, userId, userId, AuditLogEntry.PASSWORD_RESET, null,
                        PASSWORD_CHANGED_JSON, null, ipAddress);
                connection.commit();
                return userId;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
