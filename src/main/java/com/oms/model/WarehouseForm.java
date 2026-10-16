package com.oms.model;

// Dữ liệu form Thêm/Sửa kho (S5-03), giữ nguyên chuỗi người dùng nhập để hiện lại khi có lỗi
public class WarehouseForm {

    private final String code;
    private final String name;
    private final String description;
    private final String address;
    private final String managerUserId;
    private final String type;
    private final String status;

    public WarehouseForm(String code, String name, String description, String address, String managerUserId,
                         String type, String status) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.address = address;
        this.managerUserId = managerUserId;
        this.type = type;
        this.status = status;
    }

    // Form trống khi thêm mới: mặc định kho bán được, đang hoạt động
    public static WarehouseForm empty() {
        return new WarehouseForm(null, null, null, null, null, Warehouse.SELLABLE, Warehouse.ACTIVE);
    }

    public static WarehouseForm of(Warehouse warehouse) {
        return new WarehouseForm(warehouse.getCode(), warehouse.getName(), warehouse.getDescription(),
                warehouse.getAddress(),
                warehouse.getManagerUserId() == null ? null : warehouse.getManagerUserId().toString(),
                warehouse.getType(), warehouse.getStatus());
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getManagerUserId() {
        return managerUserId;
    }

    // null nếu không chọn hoặc không phải số (validate đã báo lỗi trước khi lưu)
    public Long getManagerId() {
        try {
            return managerUserId == null ? null : Long.valueOf(managerUserId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }
}
