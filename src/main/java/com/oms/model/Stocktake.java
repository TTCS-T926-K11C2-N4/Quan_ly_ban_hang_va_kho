package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Phiếu kiểm kê kho (S5-08): số tồn sổ được chụp lại lúc tạo phiếu (snapshot_at), nhập số đếm thực tế từng dòng
// (SKU + lô), hệ thống tính chênh lệch; chốt phiếu thì tồn được điều chỉnh về số đếm.
public class Stocktake {

    public static final String DRAFT = "DRAFT";
    public static final String COUNTING = "COUNTING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private static final Map<String, String> LABELS = Map.of(DRAFT, "Mới tạo", COUNTING, "Đang kiểm đếm",
            COMPLETED, "Đã chốt", CANCELLED, "Đã huỷ");
    private static final Map<String, String> TONES = Map.of(DRAFT, "neutral", COUNTING, "warning",
            COMPLETED, "success", CANCELLED, "danger");

    private final long id;
    private final String stocktakeNo;
    private final long warehouseId;
    private final String warehouseName;
    private final String scopeType;
    private final String categoryName;
    private final String status;
    private final LocalDateTime snapshotAt;
    private final String finalizedByName;
    private final LocalDateTime finalizedAt;
    private final String reason;
    private final String createdByName;
    private final int itemCount;
    private final int countedCount;
    private final int varianceCount;
    private final List<Item> items;

    public Stocktake(long id, String stocktakeNo, long warehouseId, String warehouseName, String scopeType,
                     String categoryName, String status, LocalDateTime snapshotAt, String finalizedByName,
                     LocalDateTime finalizedAt, String reason, String createdByName, int itemCount, int countedCount,
                     int varianceCount, List<Item> items) {
        this.id = id;
        this.stocktakeNo = stocktakeNo;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.scopeType = scopeType;
        this.categoryName = categoryName;
        this.status = status;
        this.snapshotAt = snapshotAt;
        this.finalizedByName = finalizedByName;
        this.finalizedAt = finalizedAt;
        this.reason = reason;
        this.createdByName = createdByName;
        this.itemCount = itemCount;
        this.countedCount = countedCount;
        this.varianceCount = varianceCount;
        this.items = List.copyOf(items);
    }

    public long getId() {
        return id;
    }

    public String getStocktakeNo() {
        return stocktakeNo;
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getScopeText() {
        return "CATEGORY".equals(scopeType) ? "Nhóm hàng: " + categoryName : "Toàn kho";
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return LABELS.getOrDefault(status, status);
    }

    public String getStatusTone() {
        return TONES.getOrDefault(status, "neutral");
    }

    // Còn nhập số đếm / chốt được
    public boolean isOpen() {
        return DRAFT.equals(status) || COUNTING.equals(status);
    }

    public String getSnapshotAtText() {
        return DateTimeUtil.formatDateTime(snapshotAt);
    }

    public String getFinalizedByName() {
        return finalizedByName;
    }

    public String getFinalizedAtText() {
        return DateTimeUtil.formatDateTime(finalizedAt);
    }

    public String getReason() {
        return reason;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getCountedCount() {
        return countedCount;
    }

    public int getVarianceCount() {
        return varianceCount;
    }

    public List<Item> getItems() {
        return items;
    }

    // Một dòng kiểm kê: SKU + lô; countedQty null = chưa đếm
    public static class Item {
        private final long id;
        private final long productId;
        private final String sku;
        private final String productName;
        private final String lotNo;
        private final String unitName;
        private final BigDecimal systemQty;
        private final BigDecimal countedQty;

        public Item(long id, long productId, String sku, String productName, String lotNo, String unitName,
                    BigDecimal systemQty, BigDecimal countedQty) {
            this.id = id;
            this.productId = productId;
            this.sku = sku;
            this.productName = productName;
            this.lotNo = lotNo;
            this.unitName = unitName;
            this.systemQty = systemQty;
            this.countedQty = countedQty;
        }

        public long getId() {
            return id;
        }

        public long getProductId() {
            return productId;
        }

        public String getSku() {
            return sku;
        }

        public String getProductName() {
            return productName;
        }

        public String getLotNo() {
            return lotNo;
        }

        public String getUnitName() {
            return unitName;
        }

        public BigDecimal getSystemQty() {
            return systemQty;
        }

        public BigDecimal getCountedQty() {
            return countedQty;
        }

        public boolean isCounted() {
            return countedQty != null;
        }

        // Chênh lệch = số đếm - số sổ; null khi chưa đếm
        public BigDecimal getVariance() {
            return countedQty == null ? null : countedQty.subtract(systemQty);
        }

        public boolean isDiffers() {
            return countedQty != null && countedQty.compareTo(systemQty) != 0;
        }
    }
}
