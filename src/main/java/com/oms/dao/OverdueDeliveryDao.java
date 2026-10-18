package com.oms.dao;

import com.oms.model.OrderStatus;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Đơn quá ngày giao mong muốn mà chưa giao xong, chưa báo nhân viên kinh doanh phụ trách đại lý
public class OverdueDeliveryDao {

    public static final String NOTIFICATION_TYPE = "ORDER_DELIVERY_OVERDUE";

    public record OverdueOrder(long orderId, String orderNo, String customerName, long salesRepId,
                               LocalDate requestedDate) {
    }

    // Mỗi đơn chỉ báo một lần: đã có thông báo loại này cho đơn thì bỏ qua. Đại lý chưa có người phụ trách không báo.
    public List<OverdueOrder> findUnnotified(LocalDate today) throws SQLException {
        String sql = "SELECT o.id, o.order_no, c.name, c.sales_rep_id, o.requested_delivery_date FROM sales_orders o"
                + " JOIN customers c ON c.id = o.customer_id"
                + " WHERE o.status IN (?, ?, ?) AND o.requested_delivery_date < ? AND c.sales_rep_id IS NOT NULL"
                + " AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.type = ? AND n.ref_type = 'ORDER'"
                + " AND n.ref_id = o.id) ORDER BY o.requested_delivery_date, o.id";
        List<OverdueOrder> orders = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, OrderStatus.APPROVED);
            statement.setString(2, OrderStatus.PICKING);
            statement.setString(3, OrderStatus.SHIPPED);
            statement.setDate(4, Date.valueOf(today));
            statement.setString(5, NOTIFICATION_TYPE);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(new OverdueOrder(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(3), resultSet.getLong(4), resultSet.getObject(5, LocalDate.class)));
                }
            }
        }
        return orders;
    }
}
