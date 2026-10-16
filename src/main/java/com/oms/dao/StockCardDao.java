package com.oms.dao;

import com.oms.model.SelectOption;
import com.oms.model.StockCardEntry;
import com.oms.model.StockCardFilter;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Thẻ kho (S6-02): đọc stock_movements, nguồn sự thật duy nhất của mọi biến động tồn (chỉ thêm, không sửa / xoá).
// Tồn sau phát sinh là tồn thực tế của SKU tại kho ngay sau bút toán.
public class StockCardDao {

    private static final String FROM = " FROM stock_movements m JOIN warehouses w ON w.id = m.warehouse_id"
            + " JOIN lots lo ON lo.id = m.lot_id LEFT JOIN users u ON u.id = m.created_by"
            + " LEFT JOIN goods_receipts gr ON m.ref_type = 'GOODS_RECEIPT' AND gr.id = m.ref_id"
            + " LEFT JOIN delivery_notes dn ON m.ref_type = 'DELIVERY_NOTE' AND dn.id = m.ref_id"
            + " LEFT JOIN stock_transfers st ON m.ref_type = 'STOCK_TRANSFER' AND st.id = m.ref_id"
            + " LEFT JOIN stocktakes sk ON m.ref_type = 'STOCKTAKE' AND sk.id = m.ref_id";
    private static final String WHERE = " WHERE m.product_id = ? AND (? IS NULL OR m.warehouse_id = ?)"
            + " AND (? IS NULL OR m.occurred_at >= ?) AND (? IS NULL OR m.occurred_at < ?)";

    // Sản phẩm đã có phát sinh kho, để chọn trên màn thẻ kho
    public List<SelectOption> findProducts() throws SQLException {
        String sql = "SELECT p.id, p.sku, p.name FROM products p WHERE EXISTS"
                + " (SELECT 1 FROM stock_movements m WHERE m.product_id = p.id) ORDER BY p.sku";
        List<SelectOption> products = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(new SelectOption(resultSet.getLong(1), resultSet.getString(2),
                        resultSet.getString(2) + " - " + resultSet.getString(3)));
            }
        }
        return products;
    }

    public long count(StockCardFilter filter) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*)" + FROM + WHERE)) {
            bind(statement, filter);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // Theo thứ tự thời gian; cùng thời điểm thì theo thứ tự ghi (id)
    public List<StockCardEntry> findPage(StockCardFilter filter, int offset, int limit) throws SQLException {
        String sql = "SELECT m.occurred_at, w.code AS warehouse_code, lo.lot_no, m.movement_type, m.qty_change_base,"
                + " m.balance_after_base, m.ref_type, m.ref_id,"
                + " COALESCE(gr.receipt_no, dn.delivery_no, st.transfer_no, sk.stocktake_no) AS document_no,"
                + " m.note, u.full_name" + FROM + WHERE + " ORDER BY m.occurred_at, m.id LIMIT ? OFFSET ?";
        List<StockCardEntry> entries = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bind(statement, filter);
            statement.setInt(next++, limit);
            statement.setInt(next, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new StockCardEntry(resultSet.getObject(1, LocalDateTime.class),
                            resultSet.getString(2), resultSet.getString(3), resultSet.getString(4),
                            resultSet.getBigDecimal(5), resultSet.getBigDecimal(6), resultSet.getString(7),
                            resultSet.getLong(8), resultSet.getString(9), resultSet.getString(10),
                            resultSet.getString(11)));
                }
            }
        }
        return entries;
    }

    // Tồn thực tế hiện tại của SKU (tổng các kho đang lọc), để so với dòng cuối thẻ kho
    public BigDecimal findCurrentOnHand(long productId, Long warehouseId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(qty_on_hand_base), 0) FROM stock_balances WHERE product_id = ?"
                + " AND (? IS NULL OR warehouse_id = ?)";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            setNullableLong(statement, 2, warehouseId);
            setNullableLong(statement, 3, warehouseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    // Tên đơn vị cơ sở của SKU; null nếu không có SKU
    public String findBaseUnitName(long productId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT u.name FROM products p JOIN units u ON u.id = p.base_unit_id WHERE p.id = ?")) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private static int bind(PreparedStatement statement, StockCardFilter filter) throws SQLException {
        statement.setLong(1, filter.getProductId());
        setNullableLong(statement, 2, filter.getWarehouseId());
        setNullableLong(statement, 3, filter.getWarehouseId());
        Timestamp from = filter.getDateFrom() == null ? null
                : Timestamp.valueOf(DateTimeUtil.startOfDayUtc(filter.getDateFrom()));
        // Hết ngày "đến" = 00:00 ngày kế tiếp
        Timestamp to = filter.getDateTo() == null ? null
                : Timestamp.valueOf(DateTimeUtil.startOfDayUtc(filter.getDateTo().plusDays(1)));
        setNullableTimestamp(statement, 4, from);
        setNullableTimestamp(statement, 5, from);
        setNullableTimestamp(statement, 6, to);
        setNullableTimestamp(statement, 7, to);
        return 8;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setNullableTimestamp(PreparedStatement statement, int index, Timestamp value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, value);
        }
    }
}
