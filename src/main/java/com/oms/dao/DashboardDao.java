package com.oms.dao;

import com.oms.model.OrderStatus;
import com.oms.model.RecentOrder;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Số liệu trang Tổng quan. salesRepId null: mọi đại lý; khác null: chỉ đại lý người đó phụ trách (phạm vi ASSIGNED).
public class DashboardDao {

    // Đơn đã gửi (không tính nháp) trong khoảng [fromUtc, toUtc)
    public int countSubmittedOrders(LocalDateTime fromUtc, LocalDateTime toUtc, Long salesRepId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales_orders o JOIN customers c ON c.id = o.customer_id"
                + " WHERE o.submitted_at >= ? AND o.submitted_at < ? AND (? IS NULL OR c.sales_rep_id = ?)";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(fromUtc));
            statement.setTimestamp(2, Timestamp.valueOf(toUtc));
            setNullableLong(statement, 3, salesRepId);
            setNullableLong(statement, 4, salesRepId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    // Doanh thu theo hoá đơn phát hành trong ngày, cùng cách tính với dashboard điều hành (S8-05)
    public BigDecimal sumInvoiceRevenue(LocalDate day, Long salesRepId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(i.line_total), 0) FROM invoice_items i"
                + " JOIN invoices v ON v.id = i.invoice_id AND v.status <> 'CANCELLED'"
                + " JOIN customers c ON c.id = v.customer_id"
                + " WHERE v.issue_date = ? AND (? IS NULL OR c.sales_rep_id = ?)";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(day));
            setNullableLong(statement, 2, salesRepId);
            setNullableLong(statement, 3, salesRepId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    // Đơn đã gửi mới nhất trước
    public List<RecentOrder> findRecentOrders(Long salesRepId, int limit) throws SQLException {
        String sql = "SELECT o.id, o.order_no, c.name, o.status, o.total_amount FROM sales_orders o"
                + " JOIN customers c ON c.id = o.customer_id"
                + " WHERE o.status <> ? AND (? IS NULL OR c.sales_rep_id = ?)"
                + " ORDER BY o.submitted_at DESC, o.id DESC LIMIT ?";
        List<RecentOrder> orders = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, OrderStatus.DRAFT);
            setNullableLong(statement, 2, salesRepId);
            setNullableLong(statement, 3, salesRepId);
            statement.setInt(4, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(new RecentOrder(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
                            OrderStatus.label(resultSet.getString(4)), resultSet.getBigDecimal(5).longValue()));
                }
            }
        }
        return orders;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
