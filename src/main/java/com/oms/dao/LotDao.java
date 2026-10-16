package com.oms.dao;

import com.oms.model.LotRow;
import com.oms.model.LotStatus;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Lô và hạn sử dụng (S6-01): đọc lots + stock_lot_balances, bật / tắt quản lý lô theo SKU, cho phép xuất lô quá hạn
// và ngưỡng cảnh báo sắp hết hạn (system_settings). Các hàm nhận Connection chạy trong transaction của service.
public class LotDao {

    public static final String NEAR_EXPIRY_KEY = "inventory.near_expiry_days";

    private static final String FROM = " FROM stock_lot_balances b JOIN lots l ON l.id = b.lot_id"
            + " JOIN products p ON p.id = l.product_id JOIN units u ON u.id = p.base_unit_id"
            + " JOIN warehouses w ON w.id = b.warehouse_id LEFT JOIN users ov ON ov.id = l.expired_issue_approved_by";
    // Chỉ lô còn hàng của SKU quản lý lô, hoặc lô có hạn dùng (lô mặc định của SKU không quản lý lô không có hạn)
    private static final String BASE_WHERE = " WHERE b.qty_on_hand_base > 0 AND (p.is_lot_tracked OR l.expiry_date IS NOT NULL)"
            + " AND (? = '' OR p.sku LIKE ? OR p.name LIKE ? OR l.lot_no LIKE ?)"
            + " AND (? IS NULL OR b.warehouse_id = ?)";

    // Đếm số dòng lô theo từng tình trạng: {quá hạn, sắp hết hạn, còn hạn, không có hạn}
    public long[] countByStatus(String keyword, Long warehouseId, LocalDate today, int nearDays) throws SQLException {
        String sql = "SELECT COALESCE(SUM(l.expiry_date < ?), 0),"
                + " COALESCE(SUM(l.expiry_date >= ? AND l.expiry_date <= ?), 0),"
                + " COALESCE(SUM(l.expiry_date > ?), 0), COALESCE(SUM(l.expiry_date IS NULL), 0)" + FROM + BASE_WHERE;
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            LocalDate limit = today.plusDays(nearDays);
            statement.setDate(1, Date.valueOf(today));
            statement.setDate(2, Date.valueOf(today));
            statement.setDate(3, Date.valueOf(limit));
            statement.setDate(4, Date.valueOf(limit));
            bindFilter(statement, 5, keyword, warehouseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new long[] {resultSet.getLong(1), resultSet.getLong(2), resultSet.getLong(3),
                    resultSet.getLong(4)};
            }
        }
    }

    // status null: mọi tình trạng. Hạn gần nhất trước, lô không hạn để cuối.
    public List<LotRow> findRows(String keyword, Long warehouseId, LotStatus status, LocalDate today, int nearDays,
                                 int offset, int limit) throws SQLException {
        String sql = "SELECT l.id, p.id, p.sku, p.name, l.lot_no, l.manufacture_date, l.expiry_date, w.code,"
                + " b.qty_on_hand_base, u.name, l.expired_issue_approved_at, ov.full_name, l.expired_issue_reason"
                + FROM + BASE_WHERE + statusSql(status) + " ORDER BY l.expiry_date IS NULL, l.expiry_date, p.sku,"
                + " l.lot_no, w.code LIMIT ? OFFSET ?";
        List<LotRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindFilter(statement, 1, keyword, warehouseId);
            next = bindStatus(statement, next, status, today, nearDays);
            statement.setInt(next++, limit);
            statement.setInt(next, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new LotRow(resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3),
                            resultSet.getString(4), resultSet.getString(5),
                            resultSet.getObject(6, LocalDate.class), resultSet.getObject(7, LocalDate.class),
                            resultSet.getString(8), resultSet.getBigDecimal(9), resultSet.getString(10),
                            resultSet.getObject(11, LocalDateTime.class), resultSet.getString(12),
                            resultSet.getString(13), today, nearDays));
                }
            }
        }
        return rows;
    }

    public long count(String keyword, Long warehouseId, LotStatus status, LocalDate today, int nearDays)
            throws SQLException {
        String sql = "SELECT COUNT(*)" + FROM + BASE_WHERE + statusSql(status);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindFilter(statement, 1, keyword, warehouseId);
            bindStatus(statement, next, status, today, nearDays);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // SKU để bật / tắt quản lý lô, kèm số lô còn hàng ngoài lô mặc định
    public List<ProductRow> findProducts(String keyword, int limit) throws SQLException {
        String sql = "SELECT p.id, p.sku, p.name, p.is_lot_tracked, (SELECT COUNT(DISTINCT l.id) FROM lots l"
                + " JOIN stock_lot_balances b ON b.lot_id = l.id WHERE l.product_id = p.id AND NOT l.is_default"
                + " AND b.qty_on_hand_base > 0) FROM products p WHERE p.status = 'ACTIVE'"
                + " AND (? = '' OR p.sku LIKE ? OR p.name LIKE ?) ORDER BY p.is_lot_tracked DESC, p.sku LIMIT ?";
        List<ProductRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = like(keyword);
            statement.setString(1, keyword);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setInt(4, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new ProductRow(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
                            resultSet.getBoolean(4), resultSet.getInt(5)));
                }
            }
        }
        return rows;
    }

    // Khoá SKU để đổi cờ quản lý lô; null nếu SKU không còn
    public ProductRow lockProduct(Connection connection, long productId) throws SQLException {
        String sql = "SELECT p.id, p.sku, p.name, p.is_lot_tracked, (SELECT COUNT(DISTINCT l.id) FROM lots l"
                + " JOIN stock_lot_balances b ON b.lot_id = l.id WHERE l.product_id = p.id AND NOT l.is_default"
                + " AND b.qty_on_hand_base > 0) FROM products p WHERE p.id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new ProductRow(resultSet.getLong(1), resultSet.getString(2),
                        resultSet.getString(3), resultSet.getBoolean(4), resultSet.getInt(5)) : null;
            }
        }
    }

    public void setTracked(Connection connection, long productId, boolean tracked, long actorUserId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET is_lot_tracked = ?, version = version + 1, updated_at = UTC_TIMESTAMP(),"
                        + " updated_by = ? WHERE id = ?")) {
            statement.setBoolean(1, tracked);
            statement.setLong(2, actorUserId);
            statement.setLong(3, productId);
            statement.executeUpdate();
        }
    }

    // Lô đang khoá để cho phép / thu hồi xuất quá hạn; null nếu không có lô
    public LotInfo lockLot(Connection connection, long lotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT l.lot_no, l.expiry_date, l.expired_issue_approved_at, p.sku FROM lots l"
                        + " JOIN products p ON p.id = l.product_id WHERE l.id = ? FOR UPDATE")) {
            statement.setLong(1, lotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new LotInfo(resultSet.getString(1),
                        resultSet.getObject(2, LocalDate.class), resultSet.getObject(3, LocalDateTime.class) != null,
                        resultSet.getString(4)) : null;
            }
        }
    }

    public void setExpiredIssue(Connection connection, long lotId, Long approvedBy, String reason)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE lots SET expired_issue_approved_by = ?, expired_issue_approved_at = "
                        + (approvedBy == null ? "NULL" : "UTC_TIMESTAMP()") + ", expired_issue_reason = ?"
                        + " WHERE id = ?")) {
            if (approvedBy == null) {
                statement.setNull(1, Types.BIGINT);
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setLong(1, approvedBy);
                statement.setString(2, reason);
            }
            statement.setLong(3, lotId);
            statement.executeUpdate();
        }
    }

    public void saveNearExpiryDays(Connection connection, int days, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO system_settings (`key`, `value`, description, updated_by) VALUES (?, ?, ?, ?)"
                        + " ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), updated_at = UTC_TIMESTAMP(),"
                        + " updated_by = VALUES(updated_by)")) {
            statement.setString(1, NEAR_EXPIRY_KEY);
            statement.setString(2, String.valueOf(days));
            statement.setString(3, "Ngưỡng cảnh báo lô sắp hết hạn (ngày)");
            statement.setLong(4, actorUserId);
            statement.executeUpdate();
        }
    }

    private static String statusSql(LotStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case EXPIRED -> " AND l.expiry_date < ?";
            case NEAR -> " AND l.expiry_date >= ? AND l.expiry_date <= ?";
            case OK -> " AND l.expiry_date > ?";
            case NONE -> " AND l.expiry_date IS NULL";
        };
    }

    private static int bindStatus(PreparedStatement statement, int next, LotStatus status, LocalDate today,
                                  int nearDays) throws SQLException {
        if (status == null) {
            return next;
        }
        LocalDate limit = today.plusDays(nearDays);
        switch (status) {
            case EXPIRED -> statement.setDate(next++, Date.valueOf(today));
            case NEAR -> {
                statement.setDate(next++, Date.valueOf(today));
                statement.setDate(next++, Date.valueOf(limit));
            }
            case OK -> statement.setDate(next++, Date.valueOf(limit));
            case NONE -> {
                // không có tham số
            }
        }
        return next;
    }

    private static int bindFilter(PreparedStatement statement, int start, String keyword, Long warehouseId)
            throws SQLException {
        String like = like(keyword);
        int index = start;
        statement.setString(index++, keyword);
        statement.setString(index++, like);
        statement.setString(index++, like);
        statement.setString(index++, like);
        if (warehouseId == null) {
            statement.setNull(index++, Types.BIGINT);
            statement.setNull(index++, Types.BIGINT);
        } else {
            statement.setLong(index++, warehouseId);
            statement.setLong(index++, warehouseId);
        }
        return index;
    }

    private static String like(String keyword) {
        return "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    public static class ProductRow {
        private final long id;
        private final String sku;
        private final String name;
        private final boolean tracked;
        private final int lotsInStock;

        public ProductRow(long id, String sku, String name, boolean tracked, int lotsInStock) {
            this.id = id;
            this.sku = sku;
            this.name = name;
            this.tracked = tracked;
            this.lotsInStock = lotsInStock;
        }

        public long getId() {
            return id;
        }

        public String getSku() {
            return sku;
        }

        public String getName() {
            return name;
        }

        public boolean isTracked() {
            return tracked;
        }

        // Số lô (không kể lô mặc định) còn hàng; > 0 thì chưa tắt được quản lý lô
        public int getLotsInStock() {
            return lotsInStock;
        }
    }

    public static class LotInfo {
        private final String lotNo;
        private final LocalDate expiryDate;
        private final boolean overridden;
        private final String sku;

        LotInfo(String lotNo, LocalDate expiryDate, boolean overridden, String sku) {
            this.lotNo = lotNo;
            this.expiryDate = expiryDate;
            this.overridden = overridden;
            this.sku = sku;
        }

        public String getLotNo() {
            return lotNo;
        }

        public LocalDate getExpiryDate() {
            return expiryDate;
        }

        public boolean isOverridden() {
            return overridden;
        }

        public String getSku() {
            return sku;
        }
    }
}
