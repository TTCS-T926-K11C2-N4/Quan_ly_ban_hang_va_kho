package com.oms.dao;

import com.oms.model.CustomerCredit;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

// Công nợ của đại lý khi tạo đơn (S4-02): hạn mức trên customers, nợ trên sổ (customer_balances), các đơn đã gửi
// chưa xuất hoá đơn và hoá đơn quá hạn (invoices). Chỉ đọc; sổ công nợ do phân hệ thu tiền ghi (S7).
public class CustomerCreditDao {

    // Đơn đã gửi mà chưa thành công nợ trên sổ: từ Chờ duyệt đến Đã giao, chưa có dòng nào được xuất hoá đơn
    private static final String OPEN_ORDER_SQL = "SELECT COALESCE(SUM(o.total_amount), 0) FROM sales_orders o"
            + " WHERE o.customer_id = ? AND o.status IN ('PENDING_APPROVAL', 'APPROVED', 'PICKING', 'SHIPPED', 'DELIVERED')"
            + " AND NOT EXISTS (SELECT 1 FROM sales_order_items i JOIN invoice_items v ON v.order_item_id = i.id"
            + " WHERE i.order_id = o.id)";
    private static final String OVERDUE_SQL = "SELECT COALESCE(SUM(v.outstanding_amount), 0), MIN(v.due_date)"
            + " FROM invoices v WHERE v.customer_id = ? AND v.status IN ('ISSUED', 'PARTIALLY_PAID')"
            + " AND v.outstanding_amount > 0 AND v.due_date < ?";

    public CustomerCredit find(long customerId, LocalDate today) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            return find(connection, customerId, today, false);
        }
    }

    // Đọc trong transaction của người gọi (đánh giá lại cờ sau thu tiền, S7-04); null nếu không còn đại lý
    public CustomerCredit find(Connection connection, long customerId, LocalDate today) throws SQLException {
        return find(connection, customerId, today, false);
    }

    // Gọi đầu transaction lưu đơn: khoá dòng đại lý trước rồi mới cộng nợ, để hai đơn gửi cùng lúc
    // phải chờ nhau và đơn sau thấy được đơn trước (null nếu không còn đại lý)
    public CustomerCredit lock(Connection connection, long customerId, LocalDate today) throws SQLException {
        return find(connection, customerId, today, true);
    }

    private CustomerCredit find(Connection connection, long customerId, LocalDate today, boolean lock)
            throws SQLException {
        BigDecimal creditLimit;
        int maxDebtDays;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT credit_limit, max_debt_days FROM customers WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                creditLimit = resultSet.getBigDecimal("credit_limit");
                maxDebtDays = resultSet.getInt("max_debt_days");
            }
        }
        BigDecimal ledgerBalance = BigDecimal.ZERO;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT outstanding_amount FROM customer_balances WHERE customer_id = ?")) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    ledgerBalance = resultSet.getBigDecimal(1);
                }
            }
        }
        BigDecimal openOrders;
        try (PreparedStatement statement = connection.prepareStatement(OPEN_ORDER_SQL)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                openOrders = resultSet.getBigDecimal(1);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(OVERDUE_SQL)) {
            statement.setLong(1, customerId);
            statement.setDate(2, Date.valueOf(today));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new CustomerCredit(creditLimit, maxDebtDays, ledgerBalance, openOrders,
                        resultSet.getBigDecimal(1), resultSet.getObject(2, LocalDate.class));
            }
        }
    }
}
