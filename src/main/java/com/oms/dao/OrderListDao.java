package com.oms.dao;

import com.oms.model.OrderListFilter;
import com.oms.model.OrderListRow;
import com.oms.model.PageResult;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Danh sách đơn hàng kèm bộ lọc (S4-07). scopeRepId != null: chỉ đơn của đại lý do người đó phụ trách (Nhân viên
// kinh doanh, phạm vi ASSIGNED); null: mọi đơn. Luôn dùng PreparedStatement; khoá sắp xếp lấy từ danh sách cố định.
public class OrderListDao {

    // Tổng tiền không cộng đơn đã huỷ và bị từ chối: hai loại này không phải doanh số
    private static final String NOT_COUNTED = "('CANCELLED', 'REJECTED')";
    private static final Map<String, String> SORT_SQL = Map.of(
            "newest", "o.created_at DESC, o.id DESC",
            "oldest", "o.created_at ASC, o.id ASC",
            "total_desc", "o.total_amount DESC, o.id DESC",
            "total_asc", "o.total_amount ASC, o.id ASC");
    private static final String FROM = " FROM sales_orders o JOIN customers c ON c.id = o.customer_id"
            + " JOIN regions r ON r.id = c.region_id LEFT JOIN users rep ON rep.id = o.sales_rep_id";

    // Điều kiện WHERE và tham số theo đúng thứ tự dấu ?
    private static final class Where {
        private final StringBuilder sql = new StringBuilder(" WHERE 1 = 1");
        private final List<Object> params = new ArrayList<>();

        Where(OrderListFilter filter, Long scopeRepId) {
            if (filter.getStatus() == null) {
                sql.append(" AND o.status <> 'DRAFT'");
            } else {
                sql.append(" AND o.status = ?");
                params.add(filter.getStatus());
            }
            if (scopeRepId != null) {
                sql.append(" AND c.sales_rep_id = ?");
                params.add(scopeRepId);
            }
            if (filter.getCustomerId() != null) {
                sql.append(" AND o.customer_id = ?");
                params.add(filter.getCustomerId());
            }
            if (filter.getSalesRepId() != null) {
                sql.append(" AND o.sales_rep_id = ?");
                params.add(filter.getSalesRepId());
            }
            if (filter.getRegionId() != null) {
                sql.append(" AND c.region_id = ?");
                params.add(filter.getRegionId());
            }
            LocalDate from = filter.getDateFrom();
            LocalDate to = filter.getDateTo();
            if (from != null) {
                sql.append(" AND o.created_at >= ?");
                params.add(DateTimeUtil.startOfDayUtc(from));
            }
            if (to != null) {
                sql.append(" AND o.created_at < ?");
                params.add(DateTimeUtil.startOfDayUtc(to.plusDays(1)));
            }
            if (filter.getKeyword() != null) {
                sql.append(" AND (o.order_no LIKE ? OR c.name LIKE ? OR c.code LIKE ?)");
                String like = "%" + filter.getKeyword() + "%";
                params.add(like);
                params.add(like);
                params.add(like);
            }
        }

        int bind(PreparedStatement statement) throws SQLException {
            int index = 1;
            for (Object param : params) {
                statement.setObject(index++, param);
            }
            return index;
        }
    }

    public PageResult<OrderListRow> search(OrderListFilter filter, Long scopeRepId, int requestedPage)
            throws SQLException {
        Where where = new Where(filter, scopeRepId);
        try (Connection connection = DbConnection.getConnection()) {
            long total;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*)" + FROM + where.sql)) {
                where.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    total = resultSet.getLong(1);
                }
            }
            int pageSize = filter.getPageSize();
            int lastPage = (int) Math.max(1, (total + pageSize - 1) / pageSize);
            int page = Math.min(Math.max(1, requestedPage), lastPage);
            String sql = "SELECT o.id, o.order_no, c.name, c.code, r.name AS region_name, rep.full_name,"
                    + " o.requested_delivery_date, o.created_at, o.total_amount, o.status, o.needs_approval"
                    + FROM + where.sql + " ORDER BY " + SORT_SQL.get(filter.getSort()) + " LIMIT ? OFFSET ?";
            List<OrderListRow> rows = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = where.bind(statement);
                statement.setInt(index++, pageSize);
                statement.setInt(index, (page - 1) * pageSize);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        rows.add(new OrderListRow(resultSet.getLong("id"), resultSet.getString("order_no"),
                                resultSet.getString("name"), resultSet.getString("code"),
                                resultSet.getString("region_name"), resultSet.getString("full_name"),
                                resultSet.getObject("requested_delivery_date", LocalDate.class),
                                resultSet.getObject("created_at", LocalDateTime.class),
                                resultSet.getBigDecimal("total_amount"), resultSet.getString("status"),
                                resultSet.getBoolean("needs_approval")));
                    }
                }
            }
            return new PageResult<>(rows, page, pageSize, total);
        }
    }

    // Tổng tiền của cả tập kết quả đang lọc (không chỉ trang hiện tại) và số đơn không được cộng
    public Totals totals(OrderListFilter filter, Long scopeRepId) throws SQLException {
        Where where = new Where(filter, scopeRepId);
        String sql = "SELECT COALESCE(SUM(CASE WHEN o.status NOT IN " + NOT_COUNTED + " THEN o.total_amount END), 0),"
                + " COALESCE(SUM(CASE WHEN o.status IN " + NOT_COUNTED + " THEN 1 ELSE 0 END), 0)"
                + FROM + where.sql;
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            where.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new Totals(resultSet.getBigDecimal(1), resultSet.getLong(2));
            }
        }
    }

    public static final class Totals {
        private final BigDecimal amount;
        private final long excludedOrders;

        public Totals(BigDecimal amount, long excludedOrders) {
            this.amount = amount;
            this.excludedOrders = excludedOrders;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        // Số đơn Đã huỷ / Bị từ chối nằm trong kết quả lọc nhưng không cộng vào tổng
        public long getExcludedOrders() {
            return excludedOrders;
        }
    }
}
