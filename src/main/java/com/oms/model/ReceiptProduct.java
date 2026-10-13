package com.oms.model;

import java.math.BigDecimal;
import java.util.List;

// Sản phẩm đang kinh doanh chọn được trên phiếu nhập kho (S5-04), kèm các đơn vị đã khai báo quy đổi (S2-07).
// Đơn vị cơ sở đứng đầu danh sách units. lotTracked: SKU quản lý lô thì mỗi dòng nhập phải ghi số lô.
public class ReceiptProduct {

    private final long id;
    private final String sku;
    private final String name;
    private final boolean lotTracked;
    private final List<Unit> units;

    public ReceiptProduct(long id, String sku, String name, boolean lotTracked, List<Unit> units) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.lotTracked = lotTracked;
        this.units = List.copyOf(units);
    }

    public long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public boolean isLotTracked() {
        return lotTracked;
    }

    public List<Unit> getUnits() {
        return units;
    }

    // Chữ hiện trong ô chọn sản phẩm, vd "SP011 - Nước suối Lavie chai 500ml"
    public String getLabel() {
        return sku + " - " + name;
    }

    public Unit findUnit(long unitId) {
        return units.stream().filter(unit -> unit.getId() == unitId).findFirst().orElse(null);
    }

    public Unit getBaseUnit() {
        return units.stream().filter(Unit::isBase).findFirst().orElse(units.isEmpty() ? null : units.get(0));
    }

    public static class Unit {
        private final long id;
        private final String name;
        private final BigDecimal factor;
        private final boolean base;

        public Unit(long id, String name, BigDecimal factor, boolean base) {
            this.id = id;
            this.name = name;
            this.factor = factor;
            this.base = base;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        // Số đơn vị cơ sở trong 1 đơn vị này, bỏ số 0 thừa (24, 0.5)
        public BigDecimal getFactor() {
            return factor;
        }

        public String getFactorText() {
            return factor.stripTrailingZeros().toPlainString();
        }

        public boolean isBase() {
            return base;
        }
    }
}
