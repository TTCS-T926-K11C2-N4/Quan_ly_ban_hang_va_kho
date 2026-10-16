package com.oms.model;

// Dữ liệu form Thêm/Sửa vị trí trong kho (S5-03), giữ nguyên chuỗi người dùng nhập để hiện lại khi có lỗi.
// locationId null khi thêm mới.
public class WarehouseLocationForm {

    private final String locationId;
    private final String type;
    private final String parentId;
    private final String code;
    private final String name;
    private final String pickSequence;

    public WarehouseLocationForm(String locationId, String type, String parentId, String code, String name,
                                 String pickSequence) {
        this.locationId = locationId;
        this.type = type;
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.pickSequence = pickSequence;
    }

    // Mặc định thêm khu; parentId đặt sẵn khi bấm "Thêm kệ" trong một khu
    public static WarehouseLocationForm empty(String type, Long parentId) {
        return new WarehouseLocationForm(null, type == null ? WarehouseLocation.ZONE : type,
                parentId == null ? null : parentId.toString(), null, null, "0");
    }

    public static WarehouseLocationForm of(WarehouseLocation location) {
        return new WarehouseLocationForm(Long.toString(location.getId()), location.getType(),
                location.getParentId() == null ? null : location.getParentId().toString(), location.getCode(),
                location.getName(), Integer.toString(location.getPickSequence()));
    }

    public String getLocationId() {
        return locationId;
    }

    public boolean isEditing() {
        return locationId != null;
    }

    public String getType() {
        return type;
    }

    public String getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPickSequence() {
        return pickSequence;
    }
}
