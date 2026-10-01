package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.UserDao;
import com.oms.model.AccountDetail;
import com.oms.model.AuditLogEntry;
import com.oms.util.DbConnection;
import com.oms.util.PhoneUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

// Hồ sơ cá nhân (S2-02): người dùng tự xem và sửa họ tên, số điện thoại; tài khoản, vai trò, kho, địa bàn
// chỉ quản trị viên đổi được nên ở đây không đọc các trường đó từ request.
public class ProfileService {

    private static final int NAME_MAX_LENGTH = 150;

    private final AccountService accountService = new AccountService();
    private final UserDao userDao = new UserDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public AccountDetail getProfile(long userId) throws SQLException {
        return accountService.getDetail(userId);
    }

    // Lỗi theo tên ô (fullName, phone); rỗng nghĩa là hợp lệ
    public Map<String, String> validate(String fullName, String phone) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (fullName == null || fullName.isBlank()) {
            errors.put("fullName", "Vui lòng nhập họ và tên.");
        } else if (fullName.trim().length() > NAME_MAX_LENGTH) {
            errors.put("fullName", "Họ và tên tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }
        if (phone == null || phone.isBlank()) {
            errors.put("phone", "Vui lòng nhập số điện thoại.");
        } else if (PhoneUtil.normalizeVietnamMobile(phone) == null) {
            errors.put("phone", "Số điện thoại phải đúng định dạng di động Việt Nam (0xxx hoặc +84xxx, đủ 10 số).");
        }
        return errors;
    }

    // Gọi validate trước. Số điện thoại lưu dạng 0xxxxxxxxx.
    public void update(long userId, String fullName, String phone, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                userDao.updateOwnProfile(connection, userId, fullName.trim(), PhoneUtil.normalizeVietnamMobile(phone));
                auditLogDao.insertUserAction(connection, userId, userId, AuditLogEntry.PROFILE_UPDATE, null, null,
                        ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
