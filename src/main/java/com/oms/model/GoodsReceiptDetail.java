package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Một phiếu nhập kho đầy đủ (S5-04): đầu phiếu và các dòng hàng, để xem phiếu đã ghi sổ hoặc mở lại phiếu nháp
public class GoodsReceiptDetail {

    private final long id;
    private final String receiptNo;
    private final long supplierId;
    private final String supplierName;
    private final long warehouseId;
    private final String warehouseCode;
    private final String warehouseName;
    private final String documentRef;
    private final LocalDate receiptDate;
    private final String status;
    private final String note;
    private final Long adjustsReceiptId;
    private final String adjustsReceiptNo;
    private final String createdByName;
    private final LocalDateTime createdAt;
    private final String confirmedByName;
    private final LocalDateTime confirmedAt;
    private final List<Item> items;

    public GoodsReceiptDetail(long id, String receiptNo, long supplierId, String supplierName, long warehouseId,
                              String warehouseCode, String warehouseName, String documentRef, LocalDate receiptDate,
                              String status, String note, Long adjustsReceiptId, String adjustsReceiptNo,
                              String createdByName, LocalDateTime createdAt, String confirmedByName,
                              LocalDateTime confirmedAt, List<Item> items) {
        this.id = id;
        this.receiptNo = receiptNo;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.documentRef = documentRef;
        this.receiptDate = receiptDate;
        this.status = status;
        this.note = note;
        this.adjustsReceiptId = adjustsReceiptId;
        this.adjustsReceiptNo = adjustsReceiptNo;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.confirmedByName = confirmedByName;
        this.confirmedAt = confirmedAt;
        this.items = List.copyOf(items);
    }

    public long getId() {
        return id;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public long getSupplierId() {
        return supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public String getReceiptDateText() {
        return DateTimeUtil.formatDay(receiptDate);
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return GoodsReceiptRow.statusLabel(status);
    }

    public boolean isDraft() {
        return GoodsReceiptRow.DRAFT.equals(status);
    }

    public boolean isConfirmed() {
        return GoodsReceiptRow.CONFIRMED.equals(status);
    }

    public String getNote() {
        return note;
    }

    public Long getAdjustsReceiptId() {
        return adjustsReceiptId;
    }

    public String getAdjustsReceiptNo() {
        return adjustsReceiptNo;
    }

    public boolean isAdjustment() {
        return adjustsReceiptId != null;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public String getConfirmedByName() {
        return confirmedByName;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public String getConfirmedAtText() {
        return DateTimeUtil.formatDateTime(confirmedAt);
    }

    public List<Item> getItems() {
        return items;
    }

    public BigDecimal getTotalAmount() {
        return items.stream().map(Item::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalQtyBase() {
        return items.stream().map(Item::getQtyBase).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Một dòng hàng đã lưu: số lượng theo đơn vị nhập và quy về đơn vị cơ sở (hệ số chụp lại lúc lưu)
    public static class Item {
        private final long productId;
        private final String sku;
        private final String productName;
        private final long unitId;
        private final String unitName;
        private final String baseUnitName;
        private final BigDecimal unitFactor;
        private final BigDecimal qty;
        private final BigDecimal qtyBase;
        private final BigDecimal unitPrice;
        private final Long locationId;
        private final String locationCode;
        private final String lotNo;
        private final boolean defaultLot;
        private final LocalDate expiryDate;

        public Item(long productId, String sku, String productName, long unitId, String unitName, String baseUnitName,
                    BigDecimal unitFactor, BigDecimal qty, BigDecimal qtyBase, BigDecimal unitPrice, Long locationId,
                    String locationCode, String lotNo, boolean defaultLot, LocalDate expiryDate) {
            this.productId = productId;
            this.sku = sku;
            this.productName = productName;
            this.unitId = unitId;
            this.unitName = unitName;
            this.baseUnitName = baseUnitName;
            this.unitFactor = unitFactor;
            this.qty = qty;
            this.qtyBase = qtyBase;
            this.unitPrice = unitPrice;
            this.locationId = locationId;
            this.locationCode = locationCode;
            this.lotNo = lotNo;
            this.defaultLot = defaultLot;
            this.expiryDate = expiryDate;
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

        public long getUnitId() {
            return unitId;
        }

        public String getUnitName() {
            return unitName;
        }

        public String getBaseUnitName() {
            return baseUnitName;
        }

        public BigDecimal getUnitFactor() {
            return unitFactor;
        }

        public String getUnitFactorText() {
            return unitFactor.stripTrailingZeros().toPlainString();
        }

        public BigDecimal getQty() {
            return qty;
        }

        public String getQtyText() {
            return qty.stripTrailingZeros().toPlainString();
        }

        public BigDecimal getQtyBase() {
            return qtyBase;
        }

        public String getQtyBaseText() {
            return qtyBase.stripTrailingZeros().toPlainString();
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getAmount() {
            return qty.multiply(unitPrice);
        }

        public Long getLocationId() {
            return locationId;
        }

        public String getLocationCode() {
            return locationCode;
        }

        // Số lô thật; null với lô mặc định của SKU không quản lý lô
        public String getLotNo() {
            return defaultLot ? null : lotNo;
        }

        public LocalDate getExpiryDate() {
            return expiryDate;
        }

        public String getExpiryDateText() {
            return DateTimeUtil.formatDay(expiryDate);
        }
    }
}
