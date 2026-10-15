package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Phiếu chuyển kho nội bộ (S5-07): kho đi xuất → hàng "đang trên đường" (đã trừ kho đi, chưa cộng kho đến)
// → kho đến xác nhận nhận thì tồn kho đến mới được cộng; nhận thiếu phải nhập lý do.
public class StockTransfer {

    public static final String DRAFT = "DRAFT";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String RECEIVED = "RECEIVED";
    public static final String CANCELLED = "CANCELLED";

    private static final Map<String, String> LABELS = Map.of(DRAFT, "Nháp", IN_TRANSIT, "Đang trên đường",
            RECEIVED, "Đã nhận", CANCELLED, "Đã huỷ");
    private static final Map<String, String> TONES = Map.of(DRAFT, "neutral", IN_TRANSIT, "warning",
            RECEIVED, "success", CANCELLED, "danger");

    private final long id;
    private final String transferNo;
    private final long fromWarehouseId;
    private final String fromWarehouseName;
    private final long toWarehouseId;
    private final String toWarehouseName;
    private final String status;
    private final LocalDateTime shippedAt;
    private final String shippedByName;
    private final LocalDateTime receivedAt;
    private final String receivedByName;
    private final String note;
    private final String createdByName;
    private final LocalDateTime createdAt;
    private final List<Item> items;

    public StockTransfer(long id, String transferNo, long fromWarehouseId, String fromWarehouseName,
                         long toWarehouseId, String toWarehouseName, String status, LocalDateTime shippedAt,
                         String shippedByName, LocalDateTime receivedAt, String receivedByName, String note,
                         String createdByName, LocalDateTime createdAt, List<Item> items) {
        this.id = id;
        this.transferNo = transferNo;
        this.fromWarehouseId = fromWarehouseId;
        this.fromWarehouseName = fromWarehouseName;
        this.toWarehouseId = toWarehouseId;
        this.toWarehouseName = toWarehouseName;
        this.status = status;
        this.shippedAt = shippedAt;
        this.shippedByName = shippedByName;
        this.receivedAt = receivedAt;
        this.receivedByName = receivedByName;
        this.note = note;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.items = List.copyOf(items);
    }

    public long getId() {
        return id;
    }

    public String getTransferNo() {
        return transferNo;
    }

    public long getFromWarehouseId() {
        return fromWarehouseId;
    }

    public String getFromWarehouseName() {
        return fromWarehouseName;
    }

    public long getToWarehouseId() {
        return toWarehouseId;
    }

    public String getToWarehouseName() {
        return toWarehouseName;
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

    public boolean isDraft() {
        return DRAFT.equals(status);
    }

    public boolean isInTransit() {
        return IN_TRANSIT.equals(status);
    }

    public String getShippedAtText() {
        return DateTimeUtil.formatDateTime(shippedAt);
    }

    public String getShippedByName() {
        return shippedByName;
    }

    public String getReceivedAtText() {
        return DateTimeUtil.formatDateTime(receivedAt);
    }

    public String getReceivedByName() {
        return receivedByName;
    }

    public String getNote() {
        return note;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public List<Item> getItems() {
        return items;
    }

    public int getItemCount() {
        return items.size();
    }

    // Một dòng chuyển: SKU + lô; qtyReceived null = chưa nhận
    public static class Item {
        private final long id;
        private final long productId;
        private final long lotId;
        private final String sku;
        private final String productName;
        private final String lotNo;
        private final String unitName;
        private final BigDecimal qtySent;
        private final BigDecimal qtyReceived;
        private final String varianceReason;

        public Item(long id, long productId, long lotId, String sku, String productName, String lotNo,
                    String unitName, BigDecimal qtySent, BigDecimal qtyReceived, String varianceReason) {
            this.id = id;
            this.productId = productId;
            this.lotId = lotId;
            this.sku = sku;
            this.productName = productName;
            this.lotNo = lotNo;
            this.unitName = unitName;
            this.qtySent = qtySent;
            this.qtyReceived = qtyReceived;
            this.varianceReason = varianceReason;
        }

        public long getId() {
            return id;
        }

        public long getProductId() {
            return productId;
        }

        public long getLotId() {
            return lotId;
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

        public BigDecimal getQtySent() {
            return qtySent;
        }

        public BigDecimal getQtyReceived() {
            return qtyReceived;
        }

        public String getVarianceReason() {
            return varianceReason;
        }

        public boolean isReceived() {
            return qtyReceived != null;
        }

        // Số thiếu so với số đã xuất (0 nếu nhận đủ hoặc chưa nhận)
        public BigDecimal getShortage() {
            return qtyReceived == null ? BigDecimal.ZERO : qtySent.subtract(qtyReceived);
        }

        public boolean isMissing() {
            return getShortage().signum() > 0;
        }
    }

    // Lô đang có hàng ở kho đi, để chọn khi thêm dòng chuyển
    public static class LotOption {
        private final long lotId;
        private final String sku;
        private final String productName;
        private final String lotNo;
        private final BigDecimal qtyOnHand;

        public LotOption(long lotId, String sku, String productName, String lotNo, BigDecimal qtyOnHand) {
            this.lotId = lotId;
            this.sku = sku;
            this.productName = productName;
            this.lotNo = lotNo;
            this.qtyOnHand = qtyOnHand;
        }

        public long getLotId() {
            return lotId;
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

        public BigDecimal getQtyOnHand() {
            return qtyOnHand;
        }
    }
}
