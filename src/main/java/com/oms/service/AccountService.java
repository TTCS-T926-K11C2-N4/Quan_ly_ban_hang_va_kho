package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.RegionDao;
import com.oms.dao.RoleDao;
import com.oms.dao.UserDao;
import com.oms.dao.WarehouseDao;
import com.oms.model.AccountFilter;
import com.oms.model.AccountForm;
import com.oms.model.AccountListItem;
import com.oms.model.AuditLogEntry;
import com.oms.model.EditableAccount;
import com.oms.model.PageResult;
import com.oms.model.RegistrationForm;
import com.oms.model.Role;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;
import com.oms.util.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AccountService {

    public static final int PAGE_SIZE = 20;

    // Tài khoản đại lý phải gắn với một khách hàng nên không tạo ở form quản trị
    private static final String CUSTOMER_ROLE = "CUSTOMER";

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,50}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{10}");
    private static final int NAME_MAX_LENGTH = 150;
    private static final int EMAIL_MAX_LENGTH = 150;

    private final UserDao userDao = new UserDao();
    private final RoleDao roleDao = new RoleDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final RegionDao regionDao = new RegionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public PageResult<AccountListItem> search(AccountFilter filter, int requestedPage) throws SQLException {
        long total = userDao.count(filter);
        int totalPages = Math.max(1, (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE));
        // Trang vượt quá (vd sau khi lọc bớt kết quả) thì đưa về trang cuối
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        List<AccountListItem> items = userDao.findPage(filter, (page - 1) * PAGE_SIZE, PAGE_SIZE);
        return new PageResult<>(items, page, PAGE_SIZE, total);
    }

    public List<Role> getAllRoles() throws SQLException {
        return roleDao.findAll();
    }

    public List<Role> getAssignableRoles() throws SQLException {
        List<Role> roles = roleDao.findAll();
        roles.removeIf(role -> CUSTOMER_ROLE.equals(role.getCode()));
        return roles;
    }

    public List<SelectOption> getWarehouses() throws SQLException {
        return warehouseDao.findActive();
    }

    public List<SelectOption> getRegions() throws SQLException {
        return regionDao.findActive();
    }

    // Trả về lỗi theo tên ô (name trong form); rỗng nghĩa là hợp lệ
    public Map<String, String> validateNewAccount(AccountForm form) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        validateFullName(form.getFullName(), errors);
        validateUsername(form.getUsername(), errors);
        validateEmail(form.getEmail(), null, errors);
        validateContactAndAssignments(form, false, errors);
        return errors;
    }

    // Tên đăng nhập không sửa được nên không kiểm tra; email trùng thì bỏ qua chính tài khoản đang sửa
    public Map<String, String> validateAccountUpdate(EditableAccount account, AccountForm form, long actorUserId)
            throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        validateFullName(form.getFullName(), errors);
        validateEmail(form.getEmail(), account.getId(), errors);
        validateContactAndAssignments(form, account.hasCustomerRole(), errors);
        return errors;
    }

    // keepsCustomerRole: tài khoản đại lý vẫn giữ vai trò Đại lý (không có trong form) nên được bỏ trống vai trò nội bộ
    private void validateContactAndAssignments(AccountForm form, boolean keepsCustomerRole, Map<String, String> errors)
            throws SQLException {
        if (form.getPhone() == null) {
            errors.put("phone", "Vui lòng nhập số điện thoại.");
        } else if (!PHONE_PATTERN.matcher(form.getPhone()).matches()) {
            errors.put("phone", "Số điện thoại phải gồm đúng 10 chữ số.");
        }

        List<String> assignableCodes = getAssignableRoles().stream().map(Role::getCode).toList();
        if (form.getRoleCodes().isEmpty()) {
            if (!keepsCustomerRole) {
                errors.put("roleCodes", "Vui lòng chọn ít nhất một vai trò.");
            }
        } else if (!assignableCodes.containsAll(form.getRoleCodes())) {
            errors.put("roleCodes", "Vai trò không hợp lệ.");
        }

        if (form.getWarehouseId() != null && !containsId(getWarehouses(), form.getWarehouseId())) {
            errors.put("warehouseId", "Kho không hợp lệ.");
        }

        if (form.getRegionId() != null && !containsId(getRegions(), form.getRegionId())) {
            errors.put("regionId", "Địa bàn không hợp lệ.");
        }
    }

    public EditableAccount findForEdit(long userId) throws SQLException {
        return userDao.findForEdit(userId);
    }

    // Cập nhật thông tin + thay vai trò nội bộ/kho/địa bàn trong một transaction.
    // activate = true chỉ có tác dụng với tài khoản đang PENDING (tự đăng ký, chờ duyệt).
    public void update(EditableAccount account, AccountForm form, boolean activate, long actorUserId, String ipAddress)
            throws SQLException {
        boolean activating = activate && account.isPending();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId = account.getId();
                userDao.updateProfile(connection, userId, form.getFullName(), form.getEmail(), form.getPhone(),
                        activating);
                auditLogDao.insertUserAction(connection, actorUserId, userId, AuditLogEntry.USER_UPDATE, null, null,
                        ipAddress);
                if (activating) {
                    auditLogDao.insertUserAction(connection, actorUserId, userId, AuditLogEntry.USER_ACTIVATE, null, null,
                            ipAddress);
                }
                userDao.deleteRolesExcept(connection, userId, CUSTOMER_ROLE);
                userDao.insertRoles(connection, userId, form.getRoleCodes());
                userDao.deleteWarehouses(connection, userId);
                if (form.getWarehouseId() != null) {
                    userDao.insertWarehouse(connection, userId, form.getWarehouseId());
                }
                userDao.deleteRegions(connection, userId);
                if (form.getRegionId() != null) {
                    userDao.insertRegion(connection, userId, form.getRegionId());
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Tạo tài khoản kèm vai trò/kho/địa bàn trong một transaction; trả về mật khẩu tạm (chỉ hiển thị một lần)
    public String create(AccountForm form, long actorUserId, String ipAddress) throws SQLException {
        String temporaryPassword = PasswordUtil.generateTemporary();
        String passwordHash = PasswordUtil.hash(temporaryPassword);

        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId = userDao.insert(connection, form.getUsername(), form.getEmail(), form.getPhone(),
                        form.getFullName(), passwordHash, "ACTIVE", true);
                userDao.insertRoles(connection, userId, form.getRoleCodes());
                if (form.getWarehouseId() != null) {
                    userDao.insertWarehouse(connection, userId, form.getWarehouseId());
                }
                if (form.getRegionId() != null) {
                    userDao.insertRegion(connection, userId, form.getRegionId());
                }
                auditLogDao.insertUserAction(connection, actorUserId, userId, AuditLogEntry.USER_CREATE, null, null,
                        ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
        return temporaryPassword;
    }

    public Map<String, String> validateRegistration(RegistrationForm form) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        validateFullName(form.getFullName(), errors);
        validateEmail(form.getEmail(), null, errors);
        validateUsername(form.getUsername(), errors);

        if (form.getPassword() == null || form.getPassword().isEmpty()) {
            errors.put("password", "Vui lòng nhập mật khẩu.");
        } else if (!PasswordUtil.meetsPolicy(form.getPassword())) {
            errors.put("password", "Mật khẩu phải có 8–64 ký tự, gồm cả chữ và số.");
        }

        if (form.getConfirmPassword() == null || form.getConfirmPassword().isEmpty()) {
            errors.put("confirmPassword", "Vui lòng nhập lại mật khẩu.");
        } else if (!form.getConfirmPassword().equals(form.getPassword())) {
            errors.put("confirmPassword", "Mật khẩu xác nhận không khớp.");
        }

        if (!form.isTermsAccepted()) {
            errors.put("acceptTerms", "Bạn cần đồng ý với Điều khoản sử dụng và Chính sách bảo mật.");
        }
        return errors;
    }

    // Tài khoản tự đăng ký chờ quản trị viên gán vai trò và kích hoạt nên không gán vai trò nào
    public void register(RegistrationForm form, String ipAddress) throws SQLException {
        String passwordHash = PasswordUtil.hash(form.getPassword());
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId = userDao.insert(connection, form.getUsername(), form.getEmail(), null,
                        form.getFullName(), passwordHash, "PENDING", false);
                auditLogDao.insertUserAction(connection, null, userId, AuditLogEntry.USER_REGISTER, null, null, ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void validateFullName(String fullName, Map<String, String> errors) {
        if (fullName == null) {
            errors.put("fullName", "Vui lòng nhập họ và tên.");
        } else if (fullName.length() > NAME_MAX_LENGTH) {
            errors.put("fullName", "Họ và tên tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }
    }

    private void validateUsername(String username, Map<String, String> errors) throws SQLException {
        if (username == null) {
            errors.put("username", "Vui lòng nhập tên đăng nhập.");
        } else if (!USERNAME_PATTERN.matcher(username).matches()) {
            errors.put("username", "Tên đăng nhập gồm 3–50 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        } else if (userDao.existsByUsername(username)) {
            errors.put("username", "Tên đăng nhập đã tồn tại.");
        }
    }

    // excludeUserId: khi sửa tài khoản, email của chính tài khoản đó không tính là trùng
    private void validateEmail(String email, Long excludeUserId, Map<String, String> errors) throws SQLException {
        if (email == null) {
            errors.put("email", "Vui lòng nhập email.");
        } else if (email.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", "Email không đúng định dạng.");
        } else if (userDao.existsByEmail(email, excludeUserId)) {
            errors.put("email", "Email đã được sử dụng.");
        }
    }

    private static boolean containsId(List<SelectOption> options, long id) {
        return options.stream().anyMatch(option -> option.getId() == id);
    }
}
