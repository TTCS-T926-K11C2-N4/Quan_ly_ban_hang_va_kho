package com.oms.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Kho hàng (S5-03). warehouse_type DEFECT là kho hàng lỗi: không bán, không làm kho phục vụ mặc định của đại lý.
public class Warehouse {

    public static final String SELLABLE = "SELLABLE";
    public static final String DEFECT = "DEFECT";
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    // Thứ tự khoá là thứ tự hiện trong ô chọn và nút chọn
    public static final Map<String, String> TYPE_LABELS = labels(SELLABLE, "Bán được", DEFECT, "Hàng lỗi");
    public static final Map<String, String> STATUS_LABELS = labels(ACTIVE, "Hoạt động", INACTIVE, "Tạm ngừng");

    private final long id;
    private final String code;
    private final String name;
    private final String description;
    private final String address;
    private final Long managerUserId;
    private final String managerName;
    private final String type;
    private final String status;
    private final int locationCount;
    private final boolean hasStock;

    public Warehouse(long id, String code, String name, String description, String address, Long managerUserId,
                     String managerName, String type, String status, int locationCount, boolean hasStock) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.address = address;
        this.managerUserId = managerUserId;
        this.managerName = managerName;
        this.type = type;
        this.status = status;
        this.locationCount = locationCount;
        this.hasStock = hasStock;
    }

    private static Map<String, String> labels(String firstCode, String firstLabel, String secondCode,
                                              String secondLabel) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(firstCode, firstLabel);
        labels.put(secondCode, secondLabel);
        return Collections.unmodifiableMap(labels);
    }

    public long getId() {
        return id;
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

    public Long getManagerUserId() {
        return managerUserId;
    }

    public String getManagerName() {
        return managerName;
    }

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return TYPE_LABELS.getOrDefault(type, type);
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return STATUS_LABELS.getOrDefault(status, status);
    }

    public boolean isDefect() {
        return DEFECT.equals(type);
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public int getLocationCount() {
        return locationCount;
    }

    // Còn hàng trong kho (tồn thực tế > 0): không tạm ngừng được
    public boolean isHasStock() {
        return hasStock;
    }
}
