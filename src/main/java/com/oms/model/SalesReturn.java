package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Phiếu trả hàng từ đại lý (S8-01): lập từ hoá đơn gốc, Quản lý kho xác nhận nhận hàng thì mới cộng tồn
public class SalesReturn {

    public static final String SUBMITTED = "SUBMITTED";
    public static final String RECEIVED = "RECEIVED";
    public static final String CANCELLED = "CANCELLED";
    public static final String GOOD = "GOOD";
    public static final String DAMAGED = "DAMAGED";

    private static final Map<String, String> STATUS_LABELS = Map.of("DRAFT", "Nháp", SUBMITTED,
            "Chờ nhận hàng", RECEIVED, "Đã nhận hàng", CANCELLED, "Đã huỷ");
    private static final Map<String, String> STATUS_TONES = Map.of("DRAFT", "info", SUBMITTED, "warning",
            RECEIVED, "success", CANCELLED, "danger");
    public static final Map<String, String> REASONS = Map.of("DAMAGED", "Hàng hỏng, lỗi",
            "WRONG_ITEM", "Giao sai hàng", "EXPIRED", "Hàng hết hạn", "OTHER", "Lý do khác");

    private final long id;
    private final String returnNo;
    private final long invoiceId;
    private final String invoiceNo;
    private final long customerId;
    private final String customerName;
    private final String warehouseName;
    private final String reasonCode;
    private final String reasonNote;
    private final String status;
    private final String createdByName;
    private final LocalDateTime createdAt;
    private final String confirmedByName;
    private final LocalDateTime confirmedAt;
    private final List<Item> items;

    public SalesReturn(long id, String returnNo, long invoiceId, String invoiceNo, long customerId,
                       String customerName, String warehouseName, String reasonCode, String reasonNote, String status,
                       String createdByName, LocalDateTime createdAt, String confirmedByName,
                       LocalDateTime confirmedAt, List<Item> items) {
        this.id = id;
        this.returnNo = returnNo;
        this.invoiceId = invoiceId;
        this.invoiceNo = invoiceNo;
        this.customerId = customerId;
        this.customerName = customerName;
        this.warehouseName = warehouseName;
        this.reasonCode = reasonCode;
        this.reasonNote = reasonNote;
        this.status = status;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.confirmedByName = confirmedByName;
        this.confirmedAt = confirmedAt;
        this.items = items;
    }

    // Cùng phiếu kèm các dòng trả
    public SalesReturn withItems(List<Item> newItems) {
        return new SalesReturn(id, returnNo, invoiceId, invoiceNo, customerId, customerName, warehouseName,
                reasonCode, reasonNote, status, createdByName, createdAt, confirmedByName, confirmedAt, newItems);
    }

    public static boolean isReason(String code) {
        return code != null && REASONS.containsKey(code);
    }

    public long getId() {
        return id;
    }

    public String getReturnNo() {
        return returnNo;
    }

    public long getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getReasonLabel() {
        return REASONS.getOrDefault(reasonCode, reasonCode);
    }

    public String getReasonNote() {
        return reasonNote;
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
        return SUBMITTED.equals(status);
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public String getConfirmedByName() {
        return confirmedByName;
    }

    public String getConfirmedAtText() {
        return DateTimeUtil.formatDateTime(confirmedAt);
    }

    public List<Item> getItems() {
        return items;
    }

    // Một dòng trả: số lượng theo đơn vị của hoá đơn, tình trạng và kho nhận
    public static class Item {
        private final String sku;
        private final String productName;
        private final String unitName;
        private final String lotNo;
        private final BigDecimal qty;
        private final String condition;
        private final String toWarehouseName;

        public Item(String sku, String productName, String unitName, String lotNo, BigDecimal qty, String condition,
                    String toWarehouseName) {
            this.sku = sku;
            this.productName = productName;
            this.unitName = unitName;
            this.lotNo = lotNo;
            this.qty = qty;
            this.condition = condition;
            this.toWarehouseName = toWarehouseName;
        }

        public String getSku() {
            return sku;
        }

        public String getProductName() {
            return productName;
        }

        public String getUnitName() {
            return unitName;
        }

        public String getLotNo() {
            return lotNo;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public String getConditionLabel() {
            return DAMAGED.equals(condition) ? "Hàng hỏng" : "Hàng tốt";
        }

        public String getToWarehouseName() {
            return toWarehouseName;
        }
    }
}
