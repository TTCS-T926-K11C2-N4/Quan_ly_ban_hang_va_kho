package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Phiếu điều chỉnh tồn kho (S8-03): hàng hỏng, hàng mất, hàng hết hạn phải huỷ; cần Quản lý kho khác người lập duyệt
public class StockAdjustment {

    public static final String PENDING = "PENDING_APPROVAL";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final Map<String, String> REASONS = Map.of("DAMAGED", "Hàng hỏng", "LOST", "Hàng mất",
            "EXPIRED_DISPOSAL", "Huỷ hàng hết hạn");

    private static final Map<String, String> STATUS_LABELS = Map.of("DRAFT", "Nháp", PENDING, "Chờ duyệt",
            APPROVED, "Đã duyệt", REJECTED, "Từ chối");
    private static final Map<String, String> STATUS_TONES = Map.of("DRAFT", "info", PENDING, "warning",
            APPROVED, "success", REJECTED, "danger");

    private final long id;
    private final String adjustmentNo;
    private final String warehouseName;
    private final String reasonType;
    private final String status;
    private final BigDecimal totalLoss;
    private final String note;
    private final String createdByName;
    private final long createdBy;
    private final LocalDateTime createdAt;
    private final String approvedByName;
    private final LocalDateTime approvedAt;
    private final List<Item> items;

    public StockAdjustment(long id, String adjustmentNo, String warehouseName, String reasonType, String status,
                           BigDecimal totalLoss, String note, String createdByName, long createdBy,
                           LocalDateTime createdAt, String approvedByName, LocalDateTime approvedAt,
                           List<Item> items) {
        this.id = id;
        this.adjustmentNo = adjustmentNo;
        this.warehouseName = warehouseName;
        this.reasonType = reasonType;
        this.status = status;
        this.totalLoss = totalLoss;
        this.note = note;
        this.createdByName = createdByName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.approvedByName = approvedByName;
        this.approvedAt = approvedAt;
        this.items = items;
    }

    public StockAdjustment withItems(List<Item> newItems) {
        return new StockAdjustment(id, adjustmentNo, warehouseName, reasonType, status, totalLoss, note,
                createdByName, createdBy, createdAt, approvedByName, approvedAt, newItems);
    }

    public static boolean isReason(String code) {
        return code != null && REASONS.containsKey(code);
    }

    public long getId() {
        return id;
    }

    public String getAdjustmentNo() {
        return adjustmentNo;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getReasonLabel() {
        return REASONS.getOrDefault(reasonType, reasonType);
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return STATUS_LABELS.getOrDefault(status, status);
    }

    public String getStatusTone() {
        return STATUS_TONES.getOrDefault(status, "info");
    }

    public boolean isPending() {
        return PENDING.equals(status);
    }

    public BigDecimal getTotalLoss() {
        return totalLoss;
    }

    public String getNote() {
        return note;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public String getApprovedAtText() {
        return DateTimeUtil.formatDateTime(approvedAt);
    }

    public List<Item> getItems() {
        return items;
    }

    // Một dòng điều chỉnh: số lượng bị trừ (đơn vị cơ sở) và giá trị tổn thất
    public static class Item {
        private final String sku;
        private final String productName;
        private final String lotNo;
        private final BigDecimal qtyBase;
        private final BigDecimal lossValue;

        public Item(String sku, String productName, String lotNo, BigDecimal qtyBase, BigDecimal lossValue) {
            this.sku = sku;
            this.productName = productName;
            this.lotNo = lotNo;
            this.qtyBase = qtyBase;
            this.lossValue = lossValue;
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

        public BigDecimal getQtyBase() {
            return qtyBase;
        }

        public BigDecimal getLossValue() {
            return lossValue;
        }
    }
}
