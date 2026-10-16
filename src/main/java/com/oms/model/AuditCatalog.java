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
        ENTITIES.put("GOODS_RECEIPT", "Phiếu nhập kho");
        ENTITIES.put("PICK_LIST", "Phiếu soạn hàng");
        ENTITIES.put("DELIVERY_NOTE", "Phiếu xuất kho");
        ENTITIES.put("DELIVERY_TRIP", "Chuyến giao hàng");
        ENTITIES.put("PRICE", "Bảng giá");
        ENTITIES.put("DISCOUNT_POLICY", "Chính sách chiết khấu");
        ENTITIES.put("CREDIT_LIMIT", "Hạn mức công nợ");
        ENTITIES.put("INVOICE", "Hoá đơn");
        ENTITIES.put("PRODUCT", "Sản phẩm");
        ENTITIES.put("PRODUCT_CATEGORY", "Nhóm hàng");
        ENTITIES.put("UNIT", "Đơn vị tính");
        ENTITIES.put("SUPPLIER", "Nhà cung cấp");
        ENTITIES.put("WAREHOUSE", "Kho hàng");
        ENTITIES.put("WAREHOUSE_LOCATION", "Vị trí kho");
        ENTITIES.put("CUSTOMER", "Đại lý");
        ENTITIES.put("DELIVERY_ADDRESS", "Điểm giao hàng");
        ENTITIES.put("ORDER", "Đơn hàng");
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
        action("GOODS_RECEIPT_CREATE", "Thêm mới", GROUP_CREATE);
        action("GOODS_RECEIPT_UPDATE", "Sửa phiếu nháp", GROUP_UPDATE);
        action("GOODS_RECEIPT_POST", "Ghi sổ", GROUP_STATUS);
        action("PICK_LIST_CREATE", "Thêm mới", GROUP_CREATE);
        action("PICK_LIST_PROGRESS", "Ghi nhận soạn hàng", GROUP_UPDATE);
        action("PICK_LIST_COMPLETE", "Hoàn tất soạn", GROUP_STATUS);
        action("PICK_LIST_CANCEL", "Huỷ phiếu soạn", GROUP_STATUS);
        action("PICK_LIST_LOT_CHANGE", "Đổi lô xuất", GROUP_UPDATE);
        action("DELIVERY_NOTE_ISSUE", "Xác nhận xuất kho", GROUP_CREATE);
        action("TRIP_CREATE", "Lập chuyến giao", GROUP_CREATE);
        action("TRIP_UPDATE", "Sửa chuyến giao", GROUP_UPDATE);
        action("TRIP_DEPART", "Xuất phát chuyến giao", GROUP_STATUS);
        action("TRIP_COMPLETE", "Hoàn thành chuyến giao", GROUP_STATUS);
        action("TRIP_CANCEL", "Huỷ chuyến giao", GROUP_STATUS);
        action("DELIVERY_CONFIRM", "Giao thành công", GROUP_STATUS);
        action("DELIVERY_FAIL", "Giao không thành công", GROUP_STATUS);
        action("LOT_TRACKING_UPDATE", "Bật / tắt quản lý lô", GROUP_UPDATE);
        action("LOT_EXPIRED_ISSUE_ALLOW", "Cho phép xuất lô quá hạn", GROUP_STATUS);
        action("LOT_EXPIRED_ISSUE_REVOKE", "Thu hồi cho phép xuất lô quá hạn", GROUP_STATUS);
        action("LOT_NEAR_EXPIRY_UPDATE", "Đặt ngưỡng lô sắp hết hạn", GROUP_UPDATE);
        action("SUPPLIER_CREATE", "Thêm mới", GROUP_CREATE);
        action("SUPPLIER_UPDATE", "Sửa", GROUP_UPDATE);
        action("SUPPLIER_DELETE", "Xoá", GROUP_DELETE);
        action("SUPPLIER_DEACTIVATE", "Ngừng giao dịch", GROUP_STATUS);
        action("SUPPLIER_ACTIVATE", "Giao dịch lại", GROUP_STATUS);
        action("WAREHOUSE_CREATE", "Thêm mới", GROUP_CREATE);
        action("WAREHOUSE_UPDATE", "Sửa", GROUP_UPDATE);
        action("WAREHOUSE_LOCATION_CREATE", "Thêm mới", GROUP_CREATE);
        action("WAREHOUSE_LOCATION_UPDATE", "Sửa", GROUP_UPDATE);
        action("WAREHOUSE_LOCATION_DEACTIVATE", "Ngừng dùng", GROUP_STATUS);
        action("WAREHOUSE_LOCATION_ACTIVATE", "Dùng lại", GROUP_STATUS);
        action("CUSTOMER_CREATE", "Thêm mới", GROUP_CREATE);
        action("CUSTOMER_UPDATE", "Sửa hồ sơ", GROUP_UPDATE);
        action("CUSTOMER_DELETE", "Xoá", GROUP_DELETE);
        action("CUSTOMER_DEACTIVATE", "Ngừng giao dịch", GROUP_STATUS);
        action("CUSTOMER_ACTIVATE", "Giao dịch lại", GROUP_STATUS);
        action("CUSTOMER_BLOCK", "Khoá giao dịch", GROUP_STATUS);
        action("CUSTOMER_UNBLOCK", "Mở giao dịch", GROUP_STATUS);
        action("CUSTOMER_ASSIGN", "Phân công NV kinh doanh", GROUP_UPDATE);
        action("SALES_REGION_TRANSFER", "Chuyển giao địa bàn", GROUP_UPDATE);
        action("DELIVERY_ADDRESS_CREATE", "Thêm mới", GROUP_CREATE);
        action("DELIVERY_ADDRESS_UPDATE", "Sửa", GROUP_UPDATE);
        action("DELIVERY_ADDRESS_DELETE", "Xoá", GROUP_DELETE);
        action("DELIVERY_ADDRESS_SET_DEFAULT", "Đặt mặc định", GROUP_UPDATE);
        action("DELIVERY_ADDRESS_DEACTIVATE", "Ngừng dùng", GROUP_STATUS);
        action("DELIVERY_ADDRESS_ACTIVATE", "Dùng lại", GROUP_STATUS);
        action("PRICE_LIST_CREATE", "Thêm mới", GROUP_CREATE);
        action("PRICE_LIST_UPDATE", "Sửa", GROUP_UPDATE);
        action("PRICE_LIST_DELETE", "Xoá", GROUP_DELETE);
        action("PRICE_LIST_VERSION", "Tạo phiên bản", GROUP_CREATE);
        action("PRICE_LIST_SHORTEN", "Rút ngắn hiệu lực", GROUP_UPDATE);
        action("CREDIT_LIMIT_UPDATE", "Sửa", GROUP_UPDATE);
        action("DISCOUNT_POLICY_CREATE", "Thêm mới", GROUP_CREATE);
        action("DISCOUNT_POLICY_UPDATE", "Sửa", GROUP_UPDATE);
        action("DISCOUNT_POLICY_DELETE", "Xoá", GROUP_DELETE);
        action("DISCOUNT_POLICY_DEACTIVATE", "Ngừng áp dụng", GROUP_STATUS);
        action("DISCOUNT_POLICY_ACTIVATE", "Áp dụng lại", GROUP_STATUS);
        action("ORDER_CREATE", "Thêm mới", GROUP_CREATE);
        action("ORDER_COPY", "Sao chép đơn", GROUP_CREATE);
        action("ORDER_UPDATE", "Sửa đơn nháp", GROUP_UPDATE);
        action("STOCK_MIN_UPDATE", "Đặt tồn tối thiểu", GROUP_UPDATE);
        action("ORDER_SUBMIT", "Gửi duyệt", GROUP_STATUS);
        action("ORDER_CANCEL", "Huỷ đơn", GROUP_STATUS);
        action("ORDER_APPROVE", "Duyệt đơn", GROUP_STATUS);
        action("ORDER_REJECT", "Từ chối đơn", GROUP_STATUS);
        action("ORDER_RETURN", "Trả lại sửa", GROUP_STATUS);
        action("STOCKTAKE_CREATE", "Tạo phiếu kiểm kê", GROUP_CREATE);
        action("STOCKTAKE_COUNT", "Nhập số đếm kiểm kê", GROUP_UPDATE);
        action("STOCKTAKE_FINALIZE", "Chốt kiểm kê", GROUP_STATUS);
        action("STOCKTAKE_CANCEL", "Huỷ phiếu kiểm kê", GROUP_STATUS);
        action("ORDER_RESERVATION_EXPIRE", "Hết hạn giữ chỗ", GROUP_STATUS);
        action("TRANSFER_CREATE", "Tạo phiếu chuyển kho", GROUP_CREATE);
        action("TRANSFER_UPDATE", "Sửa phiếu chuyển kho", GROUP_UPDATE);
        action("TRANSFER_SHIP", "Xuất kho chuyển đi", GROUP_STATUS);
        action("TRANSFER_RECEIVE", "Nhận hàng chuyển kho", GROUP_STATUS);
        action("TRANSFER_CANCEL", "Huỷ phiếu chuyển kho", GROUP_STATUS);
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
