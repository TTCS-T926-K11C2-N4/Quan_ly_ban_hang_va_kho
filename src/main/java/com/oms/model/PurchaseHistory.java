package com.oms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Lịch sử mua hàng của một đại lý trong 3 tháng gần nhất (S4-04): từng mặt hàng kèm số lượng bình quân,
// và các dòng của lần mua gần nhất để thêm nhanh vào đơn mới.
public class PurchaseHistory {

    private final List<Item> items;
    private final String lastOrderNo;
    private final LocalDate lastOrderDate;
    private final List<Line> lastOrderLines;

    public PurchaseHistory(List<Item> items, String lastOrderNo, LocalDate lastOrderDate, List<Line> lastOrderLines) {
        this.items = List.copyOf(items);
        this.lastOrderNo = lastOrderNo;
        this.lastOrderDate = lastOrderDate;
        this.lastOrderLines = List.copyOf(lastOrderLines);
    }

    public List<Item> getItems() {
        return items;
    }

    public String getLastOrderNo() {
        return lastOrderNo;
    }

    public LocalDate getLastOrderDate() {
        return lastOrderDate;
    }

    public List<Line> getLastOrderLines() {
        return lastOrderLines;
    }

    // Một mặt hàng đã mua: bình quân tính theo đơn vị mua nhiều nhất (không cộng lẫn đơn vị khác nhau)
    public static class Item {
        private final long productId;
        private final String sku;
        private final String name;
        private final long unitId;
        private final String unitName;
        private final int orderCount;
        private final BigDecimal avgQty;
        private final LocalDate lastDate;

        public Item(long productId, String sku, String name, long unitId, String unitName, int orderCount,
                    BigDecimal avgQty, LocalDate lastDate) {
            this.productId = productId;
            this.sku = sku;
            this.name = name;
            this.unitId = unitId;
            this.unitName = unitName;
            this.orderCount = orderCount;
            this.avgQty = avgQty;
            this.lastDate = lastDate;
        }

        public long getProductId() {
            return productId;
        }

        public String getSku() {
            return sku;
        }

        public String getName() {
            return name;
        }

        public long getUnitId() {
            return unitId;
        }

        public String getUnitName() {
            return unitName;
        }

        public int getOrderCount() {
            return orderCount;
        }

        public BigDecimal getAvgQty() {
            return avgQty;
        }

        public LocalDate getLastDate() {
            return lastDate;
        }
    }

    // Dòng hàng của lần mua gần nhất
    public static class Line {
        private final long productId;
        private final String sku;
        private final String name;
        private final long unitId;
        private final String unitName;
        private final BigDecimal qty;

        public Line(long productId, String sku, String name, long unitId, String unitName, BigDecimal qty) {
            this.productId = productId;
            this.sku = sku;
            this.name = name;
            this.unitId = unitId;
            this.unitName = unitName;
            this.qty = qty;
        }

        public long getProductId() {
            return productId;
        }

        public String getSku() {
            return sku;
        }

        public String getName() {
            return name;
        }

        public long getUnitId() {
            return unitId;
        }

        public String getUnitName() {
            return unitName;
        }

        public BigDecimal getQty() {
            return qty;
        }
    }
}
