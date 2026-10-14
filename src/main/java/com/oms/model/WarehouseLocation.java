package com.oms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Vị trí lưu trong kho (S5-03): khu (ZONE) hoặc kệ (RACK). Kệ có thể nằm trong một khu (parentId) hoặc đứng riêng.
public class WarehouseLocation {

    public static final String ZONE = "ZONE";
    public static final String RACK = "RACK";
    // Thứ tự khoá là thứ tự nút chọn trên form
    public static final Map<String, String> TYPE_LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(ZONE, "Khu");
        labels.put(RACK, "Kệ");
        TYPE_LABELS = Collections.unmodifiableMap(labels);
    }

    private final long id;
    private final long warehouseId;
    private final Long parentId;
    private final String code;
    private final String name;
    private final String type;
    private final int pickSequence;
    private final boolean active;
    private final int skuCount;
    private final List<WarehouseLocation> children = new ArrayList<>();

    public WarehouseLocation(long id, long warehouseId, Long parentId, String code, String name, String type,
                             int pickSequence, boolean active, int skuCount) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.type = type;
        this.pickSequence = pickSequence;
        this.active = active;
        this.skuCount = skuCount;
    }

    public long getId() {
        return id;
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return TYPE_LABELS.getOrDefault(type, type);
    }

    public boolean isZone() {
        return ZONE.equals(type);
    }

    public int getPickSequence() {
        return pickSequence;
    }

    public boolean isActive() {
        return active;
    }

    // Số SKU đang lấy vị trí này làm vị trí lưu chính (stock_balances.location_id)
    public int getSkuCount() {
        return skuCount;
    }

    // Kệ thuộc khu này (chỉ khu mới có), theo thứ tự đi soạn hàng
    public List<WarehouseLocation> getChildren() {
        return children;
    }

    // "A-01 · Kệ nước giải khát" hoặc chỉ mã nếu không đặt tên
    public String getLabel() {
        return name == null || name.isBlank() ? code : code + " · " + name;
    }
}
