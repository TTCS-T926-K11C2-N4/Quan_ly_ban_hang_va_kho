package com.oms.model;

import java.math.BigDecimal;

// Một dòng sổ tồn kho (S5-05): một SKU tại một kho, ba cột tách bạch tồn thực tế, đang giữ chỗ, khả dụng,
// tính theo đơn vị cơ sở
public class StockLedgerRow {

    private final long productId;
    private final String sku;
    private final String productName;
    private final long warehouseId;
    private final String warehouseCode;
    private final String warehouseName;
    private final BigDecimal onHand;
    private final BigDecimal reserved;
    private final BigDecimal available;
    private final BigDecimal minStock;
    private final String baseUnitName;
    private final String categoryName;

    public StockLedgerRow(long productId, String sku, String productName, long warehouseId, String warehouseCode,
                          String warehouseName, BigDecimal onHand, BigDecimal reserved, BigDecimal available,
                          BigDecimal minStock, String baseUnitName, String categoryName) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.onHand = onHand;
        this.reserved = reserved;
        this.available = available;
        this.minStock = minStock;
        this.baseUnitName = baseUnitName;
        this.categoryName = categoryName;
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

    public long getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public BigDecimal getOnHand() {
        return onHand;
    }

    public BigDecimal getReserved() {
        return reserved;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getMinStock() {
        return minStock;
    }

    public String getBaseUnitName() {
        return baseUnitName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public StockLevel getLevel() {
        return StockLevel.of(available, minStock);
    }
}
