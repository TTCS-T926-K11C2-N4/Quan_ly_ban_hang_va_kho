package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.UserDao;
import com.oms.model.AccountDetail;
import com.oms.model.AuditLogEntry;
import com.oms.model.ProfileForm;
import com.oms.util.DbConnection;
import com.oms.util.PhoneUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

// Hồ sơ cá nhân của người đang đăng nhập (S2-02)
public class ProfileService {

    private static final int NAME_MAX_LENGTH = 150;

    private final UserDao userDao = new UserDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public AccountDetail getProfile(long userId) throws SQLException {
        return userDao.findDetail(userId, null);
    }

    // Trả về lỗi theo tên ô (name trong form); rỗng nghĩa là hợp lệ
    public Map<String, String> validate(ProfileForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getFullName() == null) {
            errors.put("fullName", "Vui lòng nhập họ và tên.");
        } else if (form.getFullName().length() > NAME_MAX_LENGTH) {
            errors.put("fullName", "Họ và tên tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }

        if (form.getPhone() == null) {
            errors.put("phone", "Vui lòng nhập số điện thoại.");
        } else if (!PhoneUtil.isVietnamMobile(form.getPhone())) {
            errors.put("phone", "Số điện thoại không đúng định dạng Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
        }
        return errors;
    }

    // Chỉ ghi họ tên và số điện thoại: tài khoản, email, vai trò, kho, địa bàn không đổi được ở đây
    // dù request có gửi kèm, vì câu UPDATE không có các cột đó.
    public void update(long userId, ProfileForm form, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                userDao.updateOwnProfile(connection, userId, form.getFullName(), form.getPhone());
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
