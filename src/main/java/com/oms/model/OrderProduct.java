package com.oms.model;

import java.math.BigDecimal;
import java.util.List;

// Sản phẩm đang kinh doanh để thêm vào đơn hàng (S3-09), kèm các đơn vị đã khai báo quy đổi (S2-07).
// Đơn vị cơ sở đứng đầu danh sách units.
public class OrderProduct {

    private final long id;
    private final String sku;
    private final String name;
    private final long categoryId;
    private final List<Unit> units;

    public OrderProduct(long id, String sku, String name, long categoryId, List<Unit> units) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.categoryId = categoryId;
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

    public long getCategoryId() {
        return categoryId;
    }

    public List<Unit> getUnits() {
        return units;
    }

    // Chữ hiện trong ô "Mã hàng / Tên hàng", vd "SP001 - Nước uống đóng chai"
    public String getLabel() {
        return sku + " - " + name;
    }

    public Unit findUnit(long unitId) {
        return units.stream().filter(unit -> unit.getId() == unitId).findFirst().orElse(null);
    }

    public Unit getBaseUnit() {
        return units.stream().filter(Unit::isBase).findFirst().orElse(null);
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

        public BigDecimal getFactor() {
            return factor;
        }

        public boolean isBase() {
            return base;
        }
    }
}
