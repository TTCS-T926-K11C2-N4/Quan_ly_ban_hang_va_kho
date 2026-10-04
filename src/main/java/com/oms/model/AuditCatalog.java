package com.oms.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Tên hiển thị của loại đối tượng và hành động trong audit_logs (S2-04).
// Module mới ghi nhật ký thì khai báo thêm ở đây để bộ lọc và bảng nhật ký hiện đúng tên tiếng Việt;
// mã chưa khai báo vẫn hiện (bằng mã gốc) và thuộc nhóm "Khác".
public final class AuditCatalog {

    public static final String GROUP_CREATE = "CREATE";
    public static final String GROUP_UPDATE = "UPDATE";
    public static final String GROUP_DELETE = "DELETE";
    public static final String GROUP_STATUS = "STATUS";
    public static final String GROUP_OTHER = "OTHER";

    // Thứ tự này là thứ tự trong ô lọc "Đối tượng". Tồn kho, bảng giá, hạn mức công nợ, hoá đơn khai báo sẵn
    // theo AC của S2-04; các module đó (EP-02, EP-05, EP-07) dùng đúng mã này khi ghi audit_logs.
    private static final Map<String, String> ENTITIES = new LinkedHashMap<>();
    private static final Map<String, String> GROUPS = new LinkedHashMap<>();
    private static final Map<String, String[]> ACTIONS = new LinkedHashMap<>();

    static {
        ENTITIES.put("INVENTORY", "Tồn kho");
        ENTITIES.put("PRICE", "Bảng giá");
        ENTITIES.put("CREDIT_LIMIT", "Hạn mức công nợ");
        ENTITIES.put("INVOICE", "Hoá đơn");
        ENTITIES.put("PRODUCT", "Sản phẩm");
        ENTITIES.put("PRODUCT_CATEGORY", "Nhóm hàng");
        ENTITIES.put("UNIT", "Đơn vị tính");
        ENTITIES.put("SUPPLIER", "Nhà cung cấp");
        ENTITIES.put("USER", "Tài khoản");

        GROUPS.put(GROUP_CREATE, "Thêm mới");
        GROUPS.put(GROUP_UPDATE, "Sửa");
        GROUPS.put(GROUP_DELETE, "Xoá");
        GROUPS.put(GROUP_STATUS, "Đổi trạng thái");
        GROUPS.put(GROUP_OTHER, "Khác");

        action("USER_REGISTER", "Đăng ký", GROUP_CREATE);
        action("USER_CREATE", "Thêm mới", GROUP_CREATE);
        action("USER_UPDATE", "Sửa", GROUP_UPDATE);
        action("USER_ACTIVATE", "Kích hoạt", GROUP_STATUS);
        action("USER_LOCK", "Khoá", GROUP_STATUS);
        action("USER_UNLOCK", "Mở khoá", GROUP_STATUS);
        action("PASSWORD_CHANGE", "Đổi mật khẩu", GROUP_UPDATE);
        action("PASSWORD_RESET", "Đặt lại mật khẩu", GROUP_UPDATE);
        action("PROFILE_UPDATE", "Sửa hồ sơ", GROUP_UPDATE);
        action("AVATAR_UPDATE", "Đổi ảnh đại diện", GROUP_UPDATE);
        action("CATEGORY_CREATE", "Thêm mới", GROUP_CREATE);
        action("CATEGORY_UPDATE", "Sửa", GROUP_UPDATE);
        action("CATEGORY_DELETE", "Xoá", GROUP_DELETE);
        action("CATEGORY_DEACTIVATE", "Ngừng hoạt động", GROUP_STATUS);
        action("CATEGORY_ACTIVATE", "Hoạt động lại", GROUP_STATUS);
        action("PRODUCT_MOVE_CATEGORY", "Chuyển sản phẩm", GROUP_UPDATE);
        action("PRODUCT_CREATE", "Thêm mới", GROUP_CREATE);
        action("PRODUCT_UPDATE", "Sửa", GROUP_UPDATE);
        action("PRODUCT_DELETE", "Xoá", GROUP_DELETE);
        action("PRODUCT_DISCONTINUE", "Ngừng kinh doanh", GROUP_STATUS);
        action("PRODUCT_ACTIVATE", "Kinh doanh lại", GROUP_STATUS);
        action("UNIT_CREATE", "Thêm mới", GROUP_CREATE);
        action("UNIT_UPDATE", "Sửa", GROUP_UPDATE);
        action("UNIT_DELETE", "Xoá", GROUP_DELETE);
        action("SUPPLIER_CREATE", "Thêm mới", GROUP_CREATE);
        action("SUPPLIER_UPDATE", "Sửa", GROUP_UPDATE);
        action("SUPPLIER_DELETE", "Xoá", GROUP_DELETE);
        action("SUPPLIER_DEACTIVATE", "Ngừng giao dịch", GROUP_STATUS);
        action("SUPPLIER_ACTIVATE", "Giao dịch lại", GROUP_STATUS);
        action("PRICE_LIST_CREATE", "Thêm mới", GROUP_CREATE);
        action("PRICE_LIST_UPDATE", "Sửa", GROUP_UPDATE);
        action("PRICE_LIST_DELETE", "Xoá", GROUP_DELETE);
        action("PRICE_LIST_VERSION", "Tạo phiên bản", GROUP_CREATE);
        action("PRICE_LIST_SHORTEN", "Rút ngắn hiệu lực", GROUP_UPDATE);
    }

    private AuditCatalog() {
    }

    private static void action(String code, String label, String group) {
        ACTIONS.put(code, new String[] {label, group});
    }

    public static Map<String, String> entities() {
        return ENTITIES;
    }

    public static Map<String, String> groups() {
        return GROUPS;
    }

    public static String entityLabel(String entityType) {
        return ENTITIES.getOrDefault(entityType, entityType);
    }

    public static String actionLabel(String action) {
        String[] known = ACTIONS.get(action);
        return known == null ? action : known[0];
    }

    public static String actionGroup(String action) {
        String[] known = ACTIONS.get(action);
        return known == null ? GROUP_OTHER : known[1];
    }

    // Mã hành động thuộc một nhóm (để lọc); nhóm Khác thì trả về rỗng (lọc bằng NOT IN mọi mã đã khai báo)
    public static List<String> actionsInGroup(String group) {
        return ACTIONS.entrySet().stream().filter(entry -> entry.getValue()[1].equals(group))
                .map(Map.Entry::getKey).toList();
    }

    public static List<String> knownActions() {
        return List.copyOf(ACTIONS.keySet());
    }
}
