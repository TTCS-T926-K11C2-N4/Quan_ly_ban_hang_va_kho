package com.oms.model;

import com.oms.util.NameUtil;

import java.util.Set;

// Người dùng đang đăng nhập, lưu trong session dưới khóa SESSION_KEY (JSP đọc thẳng ${currentUser}).
// Không đổi sau khi tạo: đổi mật khẩu/đổi vai trò thì thay bằng đối tượng mới.
public class SessionUser {

    public static final String SESSION_KEY = "currentUser";

    private static final String CUSTOMER_ROLE = "CUSTOMER";

    private final long id;
    private final String username;
    private final String fullName;
    private final String roleName;
    private final String scope;
    private final boolean mustChangePassword;
    private final Set<String> roleCodes;
    private final Set<String> permissions;
    private final Long avatarFileId;

    public SessionUser(long id, String username, String fullName, String roleName, String scope,
                       boolean mustChangePassword, Set<String> roleCodes, Set<String> permissions, Long avatarFileId) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.roleName = roleName;
        this.scope = scope;
        this.mustChangePassword = mustChangePassword;
        this.roleCodes = Set.copyOf(roleCodes);
        this.permissions = Set.copyOf(permissions);
        this.avatarFileId = avatarFileId;
    }

    public long getId() {
        return id;
    }

    // null nếu chưa có ảnh đại diện (sidebar hiện chữ viết tắt). Đổi ảnh thì id đổi theo nên dùng
    // làm tham số v= trong đường dẫn ảnh để trình duyệt không hiện ảnh cũ trong bộ nhớ đệm.
    public Long getAvatarFileId() {
        return avatarFileId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    // Các vai trò nối bằng dấu phẩy (một người có thể giữ nhiều vai trò)
    public String getRoleName() {
        return roleName;
    }

    // Kho và địa bàn đang phụ trách, vd "Kho trung tâm • Thái Nguyên"; rỗng nếu không gắn kho/địa bàn nào
    public String getScope() {
        return scope;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    // Tài khoản đại lý (chỉ có vai trò Đại lý) dùng trang chủ riêng, không xem trang Tổng quan nội bộ (S1-01)
    public boolean isCustomer() {
        return roleCodes.equals(Set.of(CUSTOMER_ROLE));
    }

    public String getHomePath() {
        return isCustomer() ? "/portal" : "/dashboard";
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    // Dùng trong JSP: ${currentUser.can('USER_MANAGE')}
    public boolean can(String permission) {
        return permissions.contains(permission);
    }

    public String getInitials() {
        return NameUtil.initials(fullName);
    }
}
