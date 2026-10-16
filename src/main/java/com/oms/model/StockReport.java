package com.oms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Báo cáo tồn kho và vòng quay hàng (S8-07): giá trị tồn theo kho và nhóm hàng, số ngày tồn bình quân của từng SKU
public class StockReport {

    private final int windowDays;
    private final int slowDays;
    private final boolean showValue;
    private final List<Sku> skus;
    private final List<Group> byWarehouse;
    private final List<Group> byCategory;
    private final BigDecimal totalValue;

    private StockReport(int windowDays, int slowDays, boolean showValue, List<Sku> skus, List<Group> byWarehouse,
                        List<Group> byCategory, BigDecimal totalValue) {
        this.windowDays = windowDays;
        this.slowDays = slowDays;
        this.showValue = showValue;
        this.skus = skus;
        this.byWarehouse = byWarehouse;
        this.byCategory = byCategory;
        this.totalValue = totalValue;
    }

    // Số ngày tồn = tồn hiện tại / bán bình quân mỗi ngày trong cửa sổ; null khi không có hàng bán ra trong cửa sổ
    public static BigDecimal daysOfStock(BigDecimal onHand, BigDecimal soldInWindow, int windowDays) {
        if (soldInWindow == null || soldInWindow.signum() <= 0) {
            return null;
        }
        BigDecimal perDay = soldInWindow.divide(BigDecimal.valueOf(windowDays), 6, RoundingMode.HALF_UP);
        return onHand.divide(perDay, 1, RoundingMode.HALF_UP);
    }

    // Chậm luân chuyển: còn tồn mà không bán ra trong cửa sổ, hoặc số ngày tồn vượt ngưỡng
    public static boolean isSlow(BigDecimal onHand, BigDecimal daysOfStock, int slowDays) {
        if (onHand.signum() <= 0) {
            return false;
        }
        return daysOfStock == null || daysOfStock.compareTo(BigDecimal.valueOf(slowDays)) > 0;
    }

    public static StockReport build(int windowDays, int slowDays, boolean showValue, List<Sku> skus,
                                    Map<String, BigDecimal> warehouseValues,
                                    Map<String, BigDecimal> categoryValues) {
        List<Group> warehouses = new ArrayList<>();
        warehouseValues.forEach((name, value) -> warehouses.add(new Group(name, value)));
        List<Group> categories = new ArrayList<>();
        categoryValues.forEach((name, value) -> categories.add(new Group(name, value)));
        warehouses.sort((x, y) -> y.value.compareTo(x.value));
        categories.sort((x, y) -> y.value.compareTo(x.value));
        BigDecimal total = BigDecimal.ZERO;
        for (Group group : warehouses) {
            total = total.add(group.value);
        }
        return new StockReport(windowDays, slowDays, showValue, skus, warehouses, categories, total);
    }

    public static Map<String, BigDecimal> newTotals() {
        return new LinkedHashMap<>();
    }

    public int getWindowDays() {
        return windowDays;
    }

    public int getSlowDays() {
        return slowDays;
    }

    public boolean isShowValue() {
        return showValue;
    }

    public List<Sku> getSkus() {
        return skus;
    }

    public List<Group> getByWarehouse() {
        return byWarehouse;
    }

    public List<Group> getByCategory() {
        return byCategory;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public long getSlowCount() {
        return skus.stream().filter(Sku::isSlow).count();
    }

    // Tổng giá trị tồn của một kho hoặc một nhóm hàng
    public static class Group {
        private final String name;
        private final BigDecimal value;

        public Group(String name, BigDecimal value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getValue() {
            return value;
        }
    }

    // Một SKU: tồn (đơn vị cơ sở), bán ra trong cửa sổ, số ngày tồn và cờ chậm luân chuyển
    public static class Sku {
        private final String sku;
        private final String name;
        private final String category;
        private final BigDecimal onHand;
        private final BigDecimal sold;
        private final BigDecimal daysOfStock;
        private final boolean slow;
        private final BigDecimal value;

        public Sku(String sku, String name, String category, BigDecimal onHand, BigDecimal sold,
                   BigDecimal daysOfStock, boolean slow, BigDecimal value) {
            this.sku = sku;
            this.name = name;
            this.category = category;
            this.onHand = onHand;
            this.sold = sold;
            this.daysOfStock = daysOfStock;
            this.slow = slow;
            this.value = value;
        }

        public String getSku() {
            return sku;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public BigDecimal getOnHand() {
            return onHand;
        }

        public BigDecimal getSold() {
            return sold;
        }

        public BigDecimal getDaysOfStock() {
            return daysOfStock;
        }

        public boolean isSlow() {
            return slow;
        }

        public BigDecimal getValue() {
            return value;
        }
    }
}
