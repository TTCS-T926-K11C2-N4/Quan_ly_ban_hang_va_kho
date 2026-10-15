package com.oms.dao;

import com.oms.model.OrderStatus;
import com.oms.model.PortalOrder;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Đơn hàng của một đại lý cho cổng đại lý (S5-01). Luôn lọc theo customer_id của tài khoản đăng nhập;
// đơn nháp do nhân viên lập chưa gửi nên không hiện.
public class PortalOrderDao {

    // Đã xuất kho trở đi thì có số thực giao
    private static final Set<String> SHIPPED_STATUSES = Set.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED,
            OrderStatus.CLOSED);

    // Đại lý của tài khoản (users.customer_id); null nếu tài khoản chưa gắn đại lý
    public Long findCustomerId(long userId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT customer_id FROM users WHERE id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject(1, Long.class) : null;
            }
        }
    }

    // Mới nhất trước; chưa kèm dòng hàng
    public List<PortalOrder> findOrders(long customerId, int limit) throws SQLException {
        String sql = "SELECT o.id, o.order_no, o.status, o.requested_delivery_date, o.created_at, o.subtotal_amount,"
                + " o.discount_amount, o.total_amount, o.note, a.address FROM sales_orders o"
                + " JOIN customer_delivery_addresses a ON a.id = o.delivery_address_id"
                + " WHERE o.customer_id = ? AND o.status <> 'DRAFT' ORDER BY o.created_at DESC, o.id DESC LIMIT ?";
        List<PortalOrder> orders = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(readOrder(resultSet, List.of()));
                }
            }
        }
        return orders;
    }

    // null nếu không có đơn này hoặc đơn không thuộc đại lý (404 như nhau)
    public PortalOrder findOrder(long customerId, long orderId) throws SQLException {
        String headerSql = "SELECT o.id, o.order_no, o.status, o.requested_delivery_date, o.created_at,"
                + " o.subtotal_amount, o.discount_amount, o.total_amount, o.note, a.address FROM sales_orders o"
                + " JOIN customer_delivery_addresses a ON a.id = o.delivery_address_id"
                + " WHERE o.id = ? AND o.customer_id = ? AND o.status <> 'DRAFT'";
        String itemSql = "SELECT i.line_no, p.sku, p.name, u.name AS unit_name, i.qty, i.qty_shipped_base,"
                + " i.unit_factor, i.unit_price, i.discount_amount, i.line_total FROM sales_order_items i"
                + " JOIN products p ON p.id = i.product_id JOIN units u ON u.id = i.unit_id"
                + " WHERE i.order_id = ? ORDER BY i.line_no";
        try (Connection connection = DbConnection.getConnection()) {
            // Đọc dòng hàng sau khi biết đơn có thuộc đại lý (header) và trạng thái xuất kho của đơn
            try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
                statement.setLong(1, orderId);
                statement.setLong(2, customerId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    boolean shipped = SHIPPED_STATUSES.contains(resultSet.getString("status"));
                    return readOrder(resultSet, readItems(connection, itemSql, orderId, shipped));
                }
            }
        }
    }

    // Dòng của đơn cũ để đặt lại (S5-02): chỉ sản phẩm, đơn vị, số lượng; null nếu đơn không thuộc đại lý
    public Reorderable findReorderable(long customerId, long orderId) throws SQLException {
        String headerSql = "SELECT delivery_address_id FROM sales_orders WHERE id = ? AND customer_id = ?"
                + " AND status <> 'DRAFT'";
        String itemSql = "SELECT i.line_no, i.product_id, p.sku, p.name, i.unit_id, i.qty FROM sales_order_items i"
                + " JOIN products p ON p.id = i.product_id WHERE i.order_id = ? ORDER BY i.line_no";
        try (Connection connection = DbConnection.getConnection()) {
            long addressId;
            try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
                statement.setLong(1, orderId);
                statement.setLong(2, customerId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    addressId = resultSet.getLong(1);
                }
            }
            List<Reorderable.Line> lines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                statement.setLong(1, orderId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        lines.add(new Reorderable.Line(resultSet.getInt(1), resultSet.getLong(2),
                                resultSet.getString(3), resultSet.getString(4), resultSet.getLong(5),
                                resultSet.getBigDecimal(6)));
                    }
                }
            }
            return new Reorderable(addressId, lines);
        }
    }

    // Đơn cũ nhìn như nguồn để đặt lại
    public static class Reorderable {
        private final long deliveryAddressId;
        private final List<Line> lines;

        Reorderable(long deliveryAddressId, List<Line> lines) {
            this.deliveryAddressId = deliveryAddressId;
            this.lines = lines;
        }

        public long getDeliveryAddressId() {
            return deliveryAddressId;
        }

        public List<Line> getLines() {
            return lines;
        }

        public static class Line {
            private final int lineNo;
            private final long productId;
            private final String sku;
            private final String name;
            private final long unitId;
            private final BigDecimal qty;

            Line(int lineNo, long productId, String sku, String name, long unitId, BigDecimal qty) {
                this.lineNo = lineNo;
                this.productId = productId;
                this.sku = sku;
                this.name = name;
                this.unitId = unitId;
                this.qty = qty;
            }

            public int getLineNo() {
                return lineNo;
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

            public BigDecimal getQty() {
                return qty;
            }
        }
    }

    private static List<PortalOrder.Item> readItems(Connection connection, String sql, long orderId,
                                                    boolean shipped) throws SQLException {
        List<PortalOrder.Item> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    BigDecimal factor = resultSet.getBigDecimal("unit_factor");
                    BigDecimal shippedInUnit = resultSet.getBigDecimal("qty_shipped_base")
                            .divide(factor, 3, RoundingMode.HALF_UP);
                    items.add(new PortalOrder.Item(resultSet.getInt("line_no"), resultSet.getString("sku"),
                            resultSet.getString("name"), resultSet.getString("unit_name"),
                            resultSet.getBigDecimal("qty"), shippedInUnit, shipped,
                            resultSet.getBigDecimal("unit_price"), resultSet.getBigDecimal("discount_amount"),
                            resultSet.getBigDecimal("line_total")));
                }
            }
        }
        return items;
    }

    private static PortalOrder readOrder(ResultSet resultSet, List<PortalOrder.Item> items) throws SQLException {
        return new PortalOrder(resultSet.getLong("id"), resultSet.getString("order_no"),
                resultSet.getString("status"), resultSet.getObject("requested_delivery_date", LocalDate.class),
                resultSet.getObject("created_at", LocalDateTime.class), resultSet.getBigDecimal("subtotal_amount"),
                resultSet.getBigDecimal("discount_amount"), resultSet.getBigDecimal("total_amount"),
                resultSet.getString("note"), resultSet.getString("address"), items);
    }
}
