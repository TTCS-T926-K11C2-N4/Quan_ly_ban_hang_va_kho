package com.oms.dao;

import com.oms.model.PriceList;
import com.oms.model.PriceListItem;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Bảng giá theo nhóm khách hàng (price_lists, price_list_items, price_history) - S2-10
public class PriceListDao {

    // Đã khoá khi có cờ is_locked (module đơn hàng bật) hoặc có dòng đơn hàng dùng giá của bảng này
    private static final String LOCKED_SQL = "(pl.is_locked OR EXISTS (SELECT 1 FROM sales_order_items soi"
            + " JOIN price_list_items i ON i.id = soi.price_list_item_id WHERE i.price_list_id = pl.id))";

    private static final String SELECT_LIST = "SELECT pl.id, pl.code, pl.name, pl.customer_group_id, g.name AS group_name,"
            + " pl.version_no, pl.previous_version_id, pl.valid_from, pl.valid_to, " + LOCKED_SQL + " AS locked,"
            + " (SELECT COUNT(*) FROM price_list_items c WHERE c.price_list_id = pl.id) AS item_count"
            + " FROM price_lists pl JOIN customer_groups g ON g.id = pl.customer_group_id";

    public List<SelectOption> findActiveGroups() throws SQLException {
        List<SelectOption> groups = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, code, name FROM customer_groups WHERE is_active ORDER BY id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                groups.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("code"),
                        resultSet.getString("name")));
            }
        }
        return groups;
    }

    public boolean isActiveGroup(long groupId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM customer_groups WHERE id = ? AND is_active")) {
            statement.setLong(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public long count(Long groupId, LocalDate from, LocalDate to) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM price_lists pl" + where(groupId, from, to, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // Bảng mới bắt đầu trước, cùng ngày thì theo nhóm khách hàng
    public List<PriceList> findPage(Long groupId, LocalDate from, LocalDate to, int offset, int limit)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = SELECT_LIST + where(groupId, from, to, params)
                + " ORDER BY pl.valid_from DESC, pl.customer_group_id, pl.id DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        List<PriceList> lists = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lists.add(map(resultSet));
                }
            }
        }
        return lists;
    }

    public PriceList findById(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LIST + " WHERE pl.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    // includeCost = false thì không đọc giá vốn ra khỏi CSDL (người xem không có quyền COST_PRICE_VIEW)
    public List<PriceListItem> findItems(long priceListId, boolean includeCost) throws SQLException {
        String sql = "SELECT p.id, p.sku, p.name, i.unit_id, u.name AS unit_name,"
                + (includeCost ? " p.cost_price," : " NULL AS cost_price,") + " i.price, i.floor_price"
                + " FROM price_list_items i JOIN products p ON p.id = i.product_id JOIN units u ON u.id = i.unit_id"
                + " WHERE i.price_list_id = ? ORDER BY p.sku";
        List<PriceListItem> items = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, priceListId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new PriceListItem(resultSet.getLong("id"), resultSet.getString("sku"),
                            resultSet.getString("name"), resultSet.getLong("unit_id"),
                            resultSet.getString("unit_name"), resultSet.getBigDecimal("cost_price"),
                            resultSet.getBigDecimal("price"), resultSet.getBigDecimal("floor_price")));
                }
            }
        }
        return items;
    }

    // Sản phẩm đang kinh doanh để chọn khi nhập giá; giá tính theo đơn vị cơ sở (đơn vị quy đổi: S2-07)
    public List<PriceListItem> findPricingProducts(boolean includeCost) throws SQLException {
        String sql = "SELECT p.id, p.sku, p.name, p.base_unit_id, u.name AS unit_name,"
                + (includeCost ? " p.cost_price" : " NULL AS cost_price")
                + " FROM products p JOIN units u ON u.id = p.base_unit_id WHERE p.status = 'ACTIVE' ORDER BY p.sku";
        List<PriceListItem> products = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(new PriceListItem(resultSet.getLong("id"), resultSet.getString("sku"),
                        resultSet.getString("name"), resultSet.getLong("base_unit_id"),
                        resultSet.getString("unit_name"), resultSet.getBigDecimal("cost_price"), null, null));
            }
        }
        return products;
    }

    // Có bảng giá khác cùng nhóm trùng khoảng ngày không (mỗi ngày mỗi nhóm chỉ một bảng giá)
    public PriceList findOverlap(long groupId, LocalDate from, LocalDate to, List<Long> excludeIds)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_LIST
                + " WHERE pl.customer_group_id = ? AND pl.valid_from <= ? AND (pl.valid_to IS NULL OR pl.valid_to >= ?)");
        for (int i = 0; i < excludeIds.size(); i++) {
            sql.append(" AND pl.id <> ?");
        }
        sql.append(" ORDER BY pl.valid_from LIMIT 1");
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setLong(1, groupId);
            statement.setDate(2, Date.valueOf(to));
            statement.setDate(3, Date.valueOf(from));
            for (int i = 0; i < excludeIds.size(); i++) {
                statement.setLong(4 + i, excludeIds.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    // Mã bảng giá gốc (không tính phiên bản -Vn) lớn nhất dạng BGnnn; 0 nếu chưa có
    public int findMaxCodeNumber() throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(code, 3) AS UNSIGNED)), 0) FROM price_lists"
                + " WHERE code REGEXP '^BG[0-9]+$'";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public boolean existsCode(String code) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM price_lists WHERE code = ?")) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public long insert(Connection connection, String code, String name, long groupId, int versionNo,
                       Long previousVersionId, LocalDate from, LocalDate to, long actorUserId) throws SQLException {
        String sql = "INSERT INTO price_lists (code, name, customer_group_id, version_no, previous_version_id,"
                + " valid_from, valid_to, status, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, code);
            statement.setString(2, name);
            statement.setLong(3, groupId);
            statement.setInt(4, versionNo);
            if (previousVersionId == null) {
                statement.setNull(5, java.sql.Types.BIGINT);
            } else {
                statement.setLong(5, previousVersionId);
            }
            statement.setDate(6, Date.valueOf(from));
            statement.setDate(7, Date.valueOf(to));
            statement.setLong(8, actorUserId);
            statement.setLong(9, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // Không đổi bảng đã khoá (kiểm lại ngay trong câu UPDATE); trả về false nếu không cập nhật được
    public boolean update(Connection connection, long id, String name, long groupId, LocalDate from, LocalDate to,
                          long actorUserId) throws SQLException {
        String sql = "UPDATE price_lists pl SET name = ?, customer_group_id = ?, valid_from = ?, valid_to = ?,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE pl.id = ? AND NOT " + LOCKED_SQL;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setLong(2, groupId);
            statement.setDate(3, Date.valueOf(from));
            statement.setDate(4, Date.valueOf(to));
            statement.setLong(5, actorUserId);
            statement.setLong(6, id);
            return statement.executeUpdate() == 1;
        }
    }

    // Rút ngắn hiệu lực bản cũ khi tạo phiên bản mới (đơn đã có giữ giá đã chụp lại nên không đổi)
    public void updateValidTo(Connection connection, long id, LocalDate to, long actorUserId) throws SQLException {
        String sql = "UPDATE price_lists SET valid_to = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(to));
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    public void deleteItems(Connection connection, long priceListId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM price_list_items WHERE price_list_id = ?")) {
            statement.setLong(1, priceListId);
            statement.executeUpdate();
        }
    }

    public void insertItem(Connection connection, long priceListId, long productId, long unitId, BigDecimal price,
                           BigDecimal floorPrice, long actorUserId) throws SQLException {
        String sql = "INSERT INTO price_list_items (price_list_id, product_id, unit_id, price, floor_price,"
                + " created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, priceListId);
            statement.setLong(2, productId);
            statement.setLong(3, unitId);
            statement.setBigDecimal(4, price);
            statement.setBigDecimal(5, floorPrice);
            statement.setLong(6, actorUserId);
            statement.setLong(7, actorUserId);
            statement.executeUpdate();
        }
    }

    // Lịch sử giá theo SKU (để xem sau ở S3-02); old = null khi SKU mới có giá trong bảng này
    public void insertHistory(Connection connection, long priceListId, long productId, long unitId,
                              BigDecimal oldPrice, BigDecimal newPrice, BigDecimal oldFloor, BigDecimal newFloor,
                              LocalDate effectiveFrom, long actorUserId) throws SQLException {
        String sql = "INSERT INTO price_history (price_list_id, product_id, unit_id, old_price, new_price,"
                + " old_floor_price, new_floor_price, effective_from, created_at, created_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, priceListId);
            statement.setLong(2, productId);
            statement.setLong(3, unitId);
            statement.setBigDecimal(4, oldPrice);
            statement.setBigDecimal(5, newPrice);
            statement.setBigDecimal(6, oldFloor);
            statement.setBigDecimal(7, newFloor);
            statement.setDate(8, Date.valueOf(effectiveFrom));
            statement.setLong(9, actorUserId);
            statement.executeUpdate();
        }
    }

    public boolean hasHistory(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM price_history WHERE price_list_id = ? LIMIT 1")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Chỉ xoá được bảng chưa khoá, chưa có phiên bản sau và chưa có lịch sử giá (price_history không cho xoá);
    // trả về false nếu không xoá được
    public boolean delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT 1 FROM price_lists pl WHERE pl.id = ? AND NOT " + LOCKED_SQL
                        + " AND NOT EXISTS (SELECT 1 FROM price_lists n WHERE n.previous_version_id = pl.id)"
                        + " AND NOT EXISTS (SELECT 1 FROM price_history h WHERE h.price_list_id = pl.id) FOR UPDATE")) {
            check.setLong(1, id);
            try (ResultSet resultSet = check.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
            }
        }
        for (String sql : List.of("DELETE FROM price_list_items WHERE price_list_id = ?",
                "DELETE FROM price_lists WHERE id = ?")) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                statement.executeUpdate();
            }
        }
        return true;
    }

    public boolean hasNextVersion(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM price_lists WHERE previous_version_id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Khoảng ngày lọc: bảng giá có hiệu lực giao với [from, to]
    private static String where(Long groupId, LocalDate from, LocalDate to, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (groupId != null) {
            conditions.add("pl.customer_group_id = ?");
            params.add(groupId);
        }
        if (from != null) {
            conditions.add("(pl.valid_to IS NULL OR pl.valid_to >= ?)");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            conditions.add("pl.valid_from <= ?");
            params.add(Date.valueOf(to));
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private static PriceList map(ResultSet resultSet) throws SQLException {
        return new PriceList(resultSet.getLong("id"), resultSet.getString("code"), resultSet.getString("name"),
                resultSet.getLong("customer_group_id"), resultSet.getString("group_name"),
                resultSet.getInt("version_no"), resultSet.getObject("previous_version_id", Long.class),
                resultSet.getObject("valid_from", LocalDate.class), resultSet.getObject("valid_to", LocalDate.class),
                resultSet.getBoolean("locked"), resultSet.getInt("item_count"));
    }

    private static void bind(PreparedStatement statement, List<?> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }
}
