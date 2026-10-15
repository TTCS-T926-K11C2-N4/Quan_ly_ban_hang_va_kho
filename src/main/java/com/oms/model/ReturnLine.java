package com.oms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Một dòng hoá đơn có thể trả (S8-01): số lượng đã giao, đã trả trước đó và còn trả được, theo đơn vị của hoá đơn
public class ReturnLine {

    private final long invoiceItemId;
    private final String sku;
    private final String productName;
    private final String unitName;
    private final String lotNo;
    private final BigDecimal unitFactor;
    private final BigDecimal qty;
    private final BigDecimal qtyBase;
    private final BigDecimal returnedBase;
    private final BigDecimal unitPrice;
    private final long productId;
    private final long lotId;
    private final long warehouseId;

    public ReturnLine(long invoiceItemId, long productId, String sku, String productName, String unitName,
                      String lotNo, long lotId, long warehouseId, BigDecimal unitFactor, BigDecimal qty,
                      BigDecimal qtyBase, BigDecimal returnedBase, BigDecimal unitPrice) {
        this.invoiceItemId = invoiceItemId;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitName = unitName;
        this.lotNo = lotNo;
        this.lotId = lotId;
        this.warehouseId = warehouseId;
        this.unitFactor = unitFactor;
        this.qty = qty;
        this.qtyBase = qtyBase;
        this.returnedBase = returnedBase;
        this.unitPrice = unitPrice;
    }

    // Còn trả được theo đơn vị cơ sở (đã giao trừ phần đã trả, kể cả phiếu đang chờ nhận)
    public BigDecimal getReturnableBase() {
        return qtyBase.subtract(returnedBase).max(BigDecimal.ZERO);
    }

    // Còn trả được theo đơn vị của hoá đơn, làm tròn xuống 3 số lẻ để không vượt
    public BigDecimal getReturnable() {
        return getReturnableBase().divide(unitFactor, 3, RoundingMode.DOWN);
    }

    public BigDecimal getReturned() {
        return returnedBase.divide(unitFactor, 3, RoundingMode.HALF_UP);
    }

    public long getInvoiceItemId() {
        return invoiceItemId;
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

    public String getUnitName() {
        return unitName;
    }

    public String getLotNo() {
        return lotNo;
    }

    public long getLotId() {
        return lotId;
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public BigDecimal getUnitFactor() {
        return unitFactor;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public BigDecimal getQtyBase() {
        return qtyBase;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
