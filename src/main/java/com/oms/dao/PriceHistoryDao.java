package com.oms.dao;

import com.oms.model.PriceHistoryFilter;
import com.oms.model.PriceHistoryRow;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Đọc lịch sử thay đổi giá (price_history, S3-02). Bảng chỉ cho INSERT (trigger chặn UPDATE/DELETE),
// dòng mới do PriceListService ghi mỗi lần giá bán / giá sàn trong bảng giá thay đổi.
public class PriceHistoryDao {

    private static final String FROM = " FROM price_history h JOIN products p ON p.id = h.product_id"
            + " JOIN product_categories c ON c.id = p.category_id JOIN units u ON u.id = h.unit_id"
            + " JOIN price_lists pl ON pl.id = h.price_list_id JOIN customer_groups g ON g.id = pl.customer_group_id"
            + " LEFT JOIN users a ON a.id = h.created_by";

    public long count(PriceHistoryFilter filter, LocalDate today) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*)" + FROM + buildWhere(filter, today, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // Mới nhất trước: theo ngày áp dụng rồi lúc sửa
    public List<PriceHistoryRow> findPage(PriceHistoryFilter filter, LocalDate today, int offset, int limit)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT h.id, h.effective_from, h.created_at, p.sku, p.name AS product_name, c.name AS category_name,"
                + " u.name AS unit_name, pl.code AS price_list_code, pl.version_no, g.name AS group_name,"
                + " h.old_price, h.new_price, h.old_floor_price, h.new_floor_price, a.full_name AS actor_name,"
                + " pl.valid_from, pl.valid_to" + FROM + buildWhere(filter, today, params)
                + " ORDER BY h.effective_from DESC, h.created_at DESC, h.id DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        List<PriceHistoryRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new PriceHistoryRow(resultSet.getLong("id"),
                            resultSet.getObject("effective_from", LocalDate.class),
                            resultSet.getObject("created_at", LocalDateTime.class), resultSet.getString("sku"),
                            resultSet.getString("product_name"), resultSet.getString("category_name"),
                            resultSet.getString("unit_name"), resultSet.getString("price_list_code"),
                            resultSet.getInt("version_no"), resultSet.getString("group_name"),
                            resultSet.getBigDecimal("old_price"), resultSet.getBigDecimal("new_price"),
                            resultSet.getBigDecimal("old_floor_price"), resultSet.getBigDecimal("new_floor_price"),
                            resultSet.getString("actor_name"), resultSet.getObject("valid_from", LocalDate.class),
                            resultSet.getObject("valid_to", LocalDate.class)));
                }
            }
        }
        return rows;
    }

    private static String buildWhere(PriceHistoryFilter filter, LocalDate today, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (filter.getKeyword() != null) {
            String pattern = "%" + escapeLike(filter.getKeyword()) + "%";
            conditions.add("(p.sku LIKE ? OR p.name LIKE ?)");
            params.add(pattern);
            params.add(pattern);
        }
        if (filter.getCategoryPath() != null) {
            // Chọn nhóm cha thì gồm cả sản phẩm trong các nhóm con (giống danh sách sản phẩm)
            conditions.add("c.path LIKE ?");
            params.add(escapeLike(filter.getCategoryPath()) + "%");
        }
        if (filter.getCustomerGroupId() != null) {
            conditions.add("pl.customer_group_id = ?");
            params.add(filter.getCustomerGroupId());
        }
        if (filter.getPriceListId() != null) {
            conditions.add("h.price_list_id = ?");
            params.add(filter.getPriceListId());
        }
        if (filter.getStatus() != null) {
            Date day = Date.valueOf(today);
            switch (filter.getStatus()) {
                case UPCOMING -> {
                    conditions.add("pl.valid_from > ?");
                    params.add(day);
                }
                case ACTIVE -> {
                    conditions.add("pl.valid_from <= ? AND (pl.valid_to IS NULL OR pl.valid_to >= ?)");
                    params.add(day);
                    params.add(day);
                }
                case EXPIRED -> {
                    conditions.add("pl.valid_to < ?");
                    params.add(day);
                }
            }
        }
        if (filter.getFrom() != null) {
            conditions.add("h.effective_from >= ?");
            params.add(Date.valueOf(filter.getFrom()));
        }
        if (filter.getTo() != null) {
            conditions.add("h.effective_from <= ?");
            params.add(Date.valueOf(filter.getTo()));
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    // Người dùng gõ % hoặc _ thì tìm đúng ký tự đó, không để thành ký tự đại diện của LIKE
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void bind(PreparedStatement statement, List<?> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }
}
