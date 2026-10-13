package com.oms.dao;

import com.oms.model.SelectOption;
import com.oms.model.StockLedgerFilter;
import com.oms.model.StockLedgerRow;
import com.oms.model.StockLevel;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// Sổ tồn kho (S5-05): đọc stock_balances theo (kho, SKU). Tồn khả dụng là cột sinh qty_available_base
// (= thực tế - giữ chỗ) nên ba con số luôn khớp nhau.
public class StockLedgerDao {

    private static final String FROM = " FROM stock_balances b JOIN products p ON p.id = b.product_id"
            + " JOIN warehouses w ON w.id = b.warehouse_id JOIN units u ON u.id = p.base_unit_id"
            + " JOIN product_categories c ON c.id = p.category_id";
    // Nhóm hàng gồm cả nhóm con: so đường dẫn cây (path) của nhóm đang chọn
    private static final String WHERE = " WHERE (? = '' OR p.sku LIKE ? OR p.name LIKE ?)"
            + " AND (? IS NULL OR b.warehouse_id = ?)"
            + " AND (? IS NULL OR c.path LIKE CONCAT((SELECT pc.path FROM product_categories pc WHERE pc.id = ?), '%'))";
    private static final String OUT_SQL = "b.qty_available_base <= 0";
    private static final String LOW_SQL = "(b.qty_available_base > 0 AND b.min_stock_base > 0"
            + " AND b.qty_available_base <= b.min_stock_base)";

    public long count(StockLedgerFilter filter) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*)" + FROM + WHERE
                     + levelSql(filter.getLevel()))) {
            bindFilter(statement, filter);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // Hết hàng trước, rồi sắp hết, rồi còn hàng; trong cùng trạng thái theo SKU và mã kho.
    // limit <= 0: lấy hết (xuất Excel)
    public List<StockLedgerRow> findPage(StockLedgerFilter filter, int offset, int limit) throws SQLException {
        String sql = "SELECT p.id AS product_id, p.sku, p.name AS product_name, w.id AS warehouse_id,"
                + " w.code AS warehouse_code, w.name AS warehouse_name, b.qty_on_hand_base, b.qty_reserved_base,"
                + " b.qty_available_base, b.min_stock_base, u.name AS unit_name, c.name AS category_name" + FROM + WHERE
                + levelSql(filter.getLevel()) + " ORDER BY " + OUT_SQL + " DESC, " + LOW_SQL + " DESC, p.sku, w.code"
                + (limit > 0 ? " LIMIT ? OFFSET ?" : "");
        List<StockLedgerRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindFilter(statement, filter);
            if (limit > 0) {
                statement.setInt(next++, limit);
                statement.setInt(next, offset);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new StockLedgerRow(resultSet.getLong("product_id"), resultSet.getString("sku"),
                            resultSet.getString("product_name"), resultSet.getLong("warehouse_id"),
                            resultSet.getString("warehouse_code"), resultSet.getString("warehouse_name"),
                            resultSet.getBigDecimal("qty_on_hand_base"), resultSet.getBigDecimal("qty_reserved_base"),
                            resultSet.getBigDecimal("qty_available_base"), resultSet.getBigDecimal("min_stock_base"),
                            resultSet.getString("unit_name"), resultSet.getString("category_name")));
                }
            }
        }
        return rows;
    }

    // {tổng số dòng SKU-kho, số dòng hết hàng, số dòng sắp hết} theo bộ lọc (không tính lọc trạng thái)
    public long[] countSummary(StockLedgerFilter filter) throws SQLException {
        String sql = "SELECT COUNT(*), COALESCE(SUM(" + OUT_SQL + "), 0), COALESCE(SUM(" + LOW_SQL + "), 0)" + FROM + WHERE;
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindFilter(statement, filter.withoutLevel());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new long[] {resultSet.getLong(1), resultSet.getLong(2), resultSet.getLong(3)};
            }
        }
    }

    // Nhóm hàng đang dùng theo thứ tự cây; name thụt đầu dòng theo cấp để hiện trong ô chọn
    public List<SelectOption> findCategories() throws SQLException {
        String sql = "SELECT id, code, name, level FROM product_categories WHERE is_active ORDER BY path, sort_order";
        List<SelectOption> categories = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String indent = "— ".repeat(Math.max(0, resultSet.getInt("level") - 1));
                categories.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("code"),
                        indent + resultSet.getString("name")));
            }
        }
        return categories;
    }

    private static String levelSql(StockLevel level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case OUT -> " AND " + OUT_SQL;
            case LOW -> " AND " + LOW_SQL;
            case IN -> " AND NOT " + OUT_SQL + " AND NOT " + LOW_SQL;
        };
    }

    private static int bindFilter(PreparedStatement statement, StockLedgerFilter filter) throws SQLException {
        String keyword = filter.getKeyword();
        String like = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        statement.setString(1, keyword);
        statement.setString(2, like);
        statement.setString(3, like);
        setNullableLong(statement, 4, filter.getWarehouseId());
        setNullableLong(statement, 5, filter.getWarehouseId());
        setNullableLong(statement, 6, filter.getCategoryId());
        setNullableLong(statement, 7, filter.getCategoryId());
        return 8;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
