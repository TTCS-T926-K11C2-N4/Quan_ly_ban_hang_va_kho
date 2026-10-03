package com.oms.model;

import java.math.BigDecimal;

// Một đơn vị quy đổi của SKU (product_units, is_base = false): 1 đơn vị này = factor đơn vị cơ sở (S2-07)
public class ProductUnitConversion {

    private final long unitId;
    private final String unitCode;
    private final String unitName;
    private final BigDecimal factor;

    public ProductUnitConversion(long unitId, String unitCode, String unitName, BigDecimal factor) {
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.factor = factor;
    }

    public long getUnitId() {
        return unitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    // Hệ số hiện gọn: 24, 0.5 (bỏ số 0 thừa của decimal(18,4))
    public String getFactorText() {
        return factor.stripTrailingZeros().toPlainString();
    }
}
