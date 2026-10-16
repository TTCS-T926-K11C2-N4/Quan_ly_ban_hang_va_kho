package com.oms.service;

import com.oms.model.ExecutiveDashboard;
import com.oms.model.OverdueReport;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Dashboard điều hành (S8-05): mọi số liệu lấy bằng vài truy vấn tổng hợp trên chỉ mục sẵn có để tải dưới 2 giây.
// Doanh số = dòng hoá đơn chưa huỷ theo ngày lập hoá đơn.
public class ExecutiveDashboardService {

    public static final int OVERDUE_LIMIT = 10;

    private final OverdueReportService overdueReportService = new OverdueReportService();

    public ExecutiveDashboard load() throws SQLException {
        LocalDate today = DateTimeUtil.today();
        LocalDate monthStart = today.withDayOfMonth(1);
        BigDecimal todayRevenue;
        BigDecimal monthRevenue;
        int pending;
        int waiting;
        int belowMin;
        Map<Integer, BigDecimal> perDay = new LinkedHashMap<>();
        Map<String, BigDecimal> perRegion = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COALESCE(SUM(IF(v.issue_date = ?, i.line_total, 0)), 0), COALESCE(SUM(i.line_total), 0)"
                            + " FROM invoice_items i JOIN invoices v ON v.id = i.invoice_id AND v.status <> 'CANCELLED'"
                            + " WHERE v.issue_date BETWEEN ? AND ?")) {
                statement.setDate(1, Date.valueOf(today));
                statement.setDate(2, Date.valueOf(monthStart));
                statement.setDate(3, Date.valueOf(today));
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    todayRevenue = resultSet.getBigDecimal(1);
                    monthRevenue = resultSet.getBigDecimal(2);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COALESCE(SUM(status = 'PENDING_APPROVAL'), 0), COALESCE(SUM(status IN ('APPROVED',"
                            + " 'PICKING')), 0) FROM sales_orders WHERE status IN ('PENDING_APPROVAL', 'APPROVED',"
                            + " 'PICKING')");
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                pending = resultSet.getInt(1);
                waiting = resultSet.getInt(2);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(DISTINCT sb.product_id) FROM stock_balances sb JOIN products p ON p.id = sb.product_id"
                            + " WHERE p.status = 'ACTIVE' AND sb.min_stock_base > 0"
                            + " AND sb.qty_on_hand_base < sb.min_stock_base");
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                belowMin = resultSet.getInt(1);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT DAY(v.issue_date), SUM(i.line_total) FROM invoice_items i JOIN invoices v"
                            + " ON v.id = i.invoice_id AND v.status <> 'CANCELLED' WHERE v.issue_date BETWEEN ? AND ?"
                            + " GROUP BY DAY(v.issue_date)")) {
                statement.setDate(1, Date.valueOf(monthStart));
                statement.setDate(2, Date.valueOf(today));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        perDay.put(resultSet.getInt(1), resultSet.getBigDecimal(2));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COALESCE(r.name, 'Chưa có khu vực'), SUM(i.line_total) FROM invoice_items i"
                            + " JOIN invoices v ON v.id = i.invoice_id AND v.status <> 'CANCELLED'"
                            + " JOIN customers c ON c.id = v.customer_id LEFT JOIN regions r ON r.id = c.region_id"
                            + " WHERE v.issue_date BETWEEN ? AND ? GROUP BY r.id, r.name ORDER BY SUM(i.line_total) DESC")) {
                statement.setDate(1, Date.valueOf(monthStart));
                statement.setDate(2, Date.valueOf(today));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        perRegion.put(resultSet.getString(1), resultSet.getBigDecimal(2));
                    }
                }
            }
        }
        List<ExecutiveDashboard.Bar> daily = new ArrayList<>();
        for (int day = 1; day <= today.getDayOfMonth(); day++) {
            daily.add(new ExecutiveDashboard.Bar(String.format("%02d/%02d", day, today.getMonthValue()),
                    perDay.getOrDefault(day, BigDecimal.ZERO), 0));
        }
        List<ExecutiveDashboard.Bar> regions = new ArrayList<>();
        perRegion.forEach((name, value) -> regions.add(new ExecutiveDashboard.Bar(name, value, 0)));
        OverdueReport overdue = overdueReportService.report();
        List<OverdueReport.Row> top = overdue.getRows().stream()
                .sorted((x, y) -> y.getOverdue().compareTo(x.getOverdue())).limit(OVERDUE_LIMIT).toList();
        return new ExecutiveDashboard(todayRevenue, monthRevenue, pending, waiting, overdue.getTotal(), belowMin,
                ExecutiveDashboard.scale(daily), ExecutiveDashboard.scale(regions), top);
    }
}
