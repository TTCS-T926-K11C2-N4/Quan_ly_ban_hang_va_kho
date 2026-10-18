package com.oms.dao;

import com.oms.model.OrderStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Đơn đã giao đủ, hoá đơn đã lập và đã thanh toán đủ (đủ điều kiện sang "Đóng")
public class OrderClosureDao {

    public record SettledOrder(long id, String orderNo) {
    }

    // Đơn Đã giao của đại lý thoả cả ba điều kiện: có hoá đơn; mọi dòng đã giao có trên hoá đơn; mọi hoá đơn chứa hàng
    // của đơn đều đã thanh toán đủ. Khoá đơn để hai giao dịch thu tiền song song không đóng trùng.
    public List<SettledOrder> lockSettled(Connection connection, long customerId) throws SQLException {
        String sql = "SELECT o.id, o.order_no FROM sales_orders o WHERE o.customer_id = ? AND o.status = ?"
                + " AND EXISTS (SELECT 1 FROM invoice_items ii JOIN sales_order_items i ON i.id = ii.order_item_id"
                + " WHERE i.order_id = o.id)"
                + " AND NOT EXISTS (SELECT 1 FROM invoice_items ii JOIN sales_order_items i ON i.id = ii.order_item_id"
                + " JOIN invoices v ON v.id = ii.invoice_id WHERE i.order_id = o.id AND v.status NOT IN ('PAID', 'CANCELLED'))"
                + " AND NOT EXISTS (SELECT 1 FROM delivery_notes d JOIN delivery_note_items di ON di.delivery_note_id = d.id"
                + " WHERE d.order_id = o.id AND d.status = 'DELIVERED' AND di.qty_delivered_base > 0"
                + " AND NOT EXISTS (SELECT 1 FROM invoice_items ii WHERE ii.delivery_note_item_id = di.id))"
                + " ORDER BY o.id FOR UPDATE";
        List<SettledOrder> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setString(2, OrderStatus.DELIVERED);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(new SettledOrder(resultSet.getLong(1), resultSet.getString(2)));
                }
            }
        }
        return orders;
    }
}
