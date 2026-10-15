package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// Một dòng danh sách lô (S6-01): lô của một SKU và số còn lại ở một kho
public class LotRow {

    private final long lotId;
    private final long productId;
    private final String sku;
    private final String productName;
    private final String lotNo;
    private final LocalDate manufactureDate;
    private final LocalDate expiryDate;
    private final String warehouseCode;
    private final BigDecimal qty;
    private final String baseUnitName;
    private final LocalDateTime overrideAt;
    private final String overrideBy;
    private final String overrideReason;
    private final LotStatus status;
    private final LocalDate today;

    public LotRow(long lotId, long productId, String sku, String productName, String lotNo, LocalDate manufactureDate,
                  LocalDate expiryDate, String warehouseCode, BigDecimal qty, String baseUnitName,
                  LocalDateTime overrideAt, String overrideBy, String overrideReason, LocalDate today, int nearDays) {
        this.lotId = lotId;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.lotNo = lotNo;
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
        this.warehouseCode = warehouseCode;
        this.qty = qty;
        this.baseUnitName = baseUnitName;
        this.overrideAt = overrideAt;
        this.overrideBy = overrideBy;
        this.overrideReason = overrideReason;
        this.today = today;
        this.status = LotStatus.of(expiryDate, today, nearDays);
    }

    public long getLotId() {
        return lotId;
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

    public String getManufactureDateText() {
        return DateTimeUtil.formatDay(manufactureDate);
    }

    public String getExpiryDateText() {
        return DateTimeUtil.formatDay(expiryDate);
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public String getBaseUnitName() {
        return baseUnitName;
    }

    public LotStatus getStatus() {
        return status;
    }

    // Số ngày còn lại tới hạn (âm nếu đã quá hạn); null nếu lô không có hạn
    public Long getDaysLeft() {
        return expiryDate == null ? null : ChronoUnit.DAYS.between(today, expiryDate);
    }

    // Quản lý kho đã cho phép xuất lô quá hạn này
    public boolean isOverridden() {
        return overrideAt != null;
    }

    public String getOverrideAtText() {
        return DateTimeUtil.formatDateTime(overrideAt);
    }

    public String getOverrideBy() {
        return overrideBy;
    }

    public String getOverrideReason() {
        return overrideReason;
    }
}
