package com.oms.dao;

import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Các dòng hàng đại lý đã mua (S4-04). "Đã mua" là đơn đã được duyệt trở đi, không tính nháp, chờ duyệt, từ chối, huỷ;
// chỉ lấy hàng còn kinh doanh để thêm vào đơn mới được.
public class PurchaseHistoryDao {

    // Một dòng hàng của một đơn đã mua
    public static class Row {
        private final long orderId;
        private final String orderNo;
        private final LocalDateTime createdAt;
        private final long productId;
        private final String sku;
        private final String name;
        private final long unitId;
        private final String unitName;
        private final BigDecimal qty;

        public Row(long orderId, String orderNo, LocalDateTime createdAt, long productId, String sku, String name,
                   long unitId, String unitName, BigDecimal qty) {
            this.orderId = orderId;
            this.orderNo = orderNo;
            this.createdAt = createdAt;
            this.productId = productId;
            this.sku = sku;
            this.name = name;
            this.unitId = unitId;
            this.unitName = unitName;
            this.qty = qty;
        }

        public long getOrderId() {
            return orderId;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
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

    // Mới nhất trước; since là mốc UTC bắt đầu của khoảng 3 tháng
    public List<Row> findRows(long customerId, LocalDateTime since) throws SQLException {
        String sql = "SELECT o.id, o.order_no, o.created_at, i.product_id, p.sku, p.name, i.unit_id,"
                + " u.name AS unit_name, i.qty FROM sales_orders o"
                + " JOIN sales_order_items i ON i.order_id = o.id JOIN products p ON p.id = i.product_id"
                + " JOIN units u ON u.id = i.unit_id WHERE o.customer_id = ? AND o.created_at >= ?"
                + " AND o.status IN ('APPROVED', 'PICKING', 'SHIPPED', 'DELIVERED', 'CLOSED')"
                + " AND p.status = 'ACTIVE' ORDER BY o.created_at DESC, o.id DESC, i.line_no";
        List<Row> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setObject(2, since);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new Row(resultSet.getLong("id"), resultSet.getString("order_no"),
                            resultSet.getObject("created_at", LocalDateTime.class), resultSet.getLong("product_id"),
                            resultSet.getString("sku"), resultSet.getString("name"), resultSet.getLong("unit_id"),
                            resultSet.getString("unit_name"), resultSet.getBigDecimal("qty")));
                }
            }
        }
        return rows;
    }
}
