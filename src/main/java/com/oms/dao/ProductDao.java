package com.oms.dao;

import com.oms.model.Product;
import com.oms.model.ProductFilter;
import com.oms.model.ProductForm;
import com.oms.model.ProductListItem;
import com.oms.model.ProductSummary;
import com.oms.model.ProductUnitConversion;
import com.oms.model.StockStatus;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Truy vấn sản phẩm: danh mục sản phẩm (S2-05) và danh sách sản phẩm của một nhóm hàng (S2-06)
public class ProductDao {

    // Bảng chứng từ/sổ kho: sản phẩm có dòng ở đây là đã phát sinh giao dịch, không được xoá (S2-05)
    private static final List<String> TRANSACTION_TABLES = List.of(
            "sales_order_items", "lots", "stock_movements", "stock_reservations", "goods_receipt_items",
            "stock_transfer_items", "stocktake_items", "pick_list_items", "delivery_note_items", "invoice_items",
            "sales_return_items", "stock_adjustment_items");

    private static final Map<String, String> ORDER_BY = Map.of(
            "name", "p.name, p.id",
            "cost_asc", "p.cost_price, p.name, p.id",
            "cost_desc", "p.cost_price DESC, p.name, p.id",
            "stock_asc", "stock, p.name, p.id",
            "stock_desc", "stock DESC, p.name, p.id");

    private static final String STOCK_STATUS_SQL =
            "CASE WHEN p.status = 'DISCONTINUED' THEN 'DISCONTINUED'"
            + " WHEN COALESCE(s.qty, 0) <= 0 THEN 'OUT_OF_STOCK'"
            + " WHEN COALESCE(s.qty, 0) <= COALESCE(s.min_qty, 0) THEN 'LOW_STOCK'"
            + " ELSE 'IN_STOCK' END";

    // Đơn vị quy đổi của SKU gói vào một chuỗi (hệ số lớn trước) để danh sách không phải truy vấn từng dòng (S2-07).
    // Ký tự phân cách là ký tự điều khiển, không gõ được vào tên đơn vị.
    private static final char LIST_SEPARATOR = (char) 30;
    private static final char FIELD_SEPARATOR = (char) 31;
    private static final String CONVERSIONS_SQL = "(SELECT GROUP_CONCAT(CONCAT(cu.name, CHAR(31), pu.factor_to_base)"
            + " ORDER BY pu.factor_to_base DESC SEPARATOR 0x1E) FROM product_units pu JOIN units cu ON cu.id = pu.unit_id"
            + " WHERE pu.product_id = p.id AND NOT pu.is_base)";

    public List<ProductSummary> findByCategory(long categoryId) throws SQLException {
        List<ProductSummary> products = new ArrayList<>();
        String sql = "SELECT id, sku, name, status FROM products WHERE category_id = ? ORDER BY name";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(new ProductSummary(resultSet.getLong("id"), resultSet.getString("sku"),
                            resultSet.getString("name"), resultSet.getString("status")));
                }
            }
        }
        return products;
    }

    public long count(ProductFilter filter) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*)" + fromWhere(filter, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // includeCost = false thì không đọc giá vốn ra khỏi CSDL (người xem không có quyền COST_PRICE_VIEW)
    public List<ProductListItem> findPage(ProductFilter filter, boolean includeCost, int offset, int limit)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT p.id, p.sku, p.name, c.name AS category_name, u.name AS unit_name,"
                + (includeCost ? " p.cost_price," : " NULL AS cost_price,")
                + " p.image_file_id, COALESCE(s.qty, 0) AS stock, " + STOCK_STATUS_SQL + " AS stock_status,"
                + CONVERSIONS_SQL + " AS conversions"
                + fromWhere(filter, params)
                + " ORDER BY " + ORDER_BY.getOrDefault(filter.getSort(), ORDER_BY.get("name")) + " LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);

        List<ProductListItem> products = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(new ProductListItem(resultSet.getLong("id"), resultSet.getString("sku"),
                            resultSet.getString("name"), resultSet.getString("category_name"),
                            resultSet.getString("unit_name"), resultSet.getBigDecimal("cost_price"),
                            resultSet.getObject("image_file_id", Long.class), resultSet.getBigDecimal("stock"),
                            StockStatus.fromCode(resultSet.getString("stock_status")),
                            conversionTexts(resultSet.getString("conversions"), resultSet.getString("unit_name"))));
                }
            }
        }
        return products;
    }

    // "1 Lốc = 6 Lon", "1 Thùng = 24 Lon"; hệ số bỏ số 0 thừa của decimal(18,4)
    static List<String> conversionTexts(String packed, String baseUnitName) {
        List<String> texts = new ArrayList<>();
        if (packed == null) {
            return texts;
        }
        for (String item : packed.split(String.valueOf(LIST_SEPARATOR))) {
            String[] parts = item.split(String.valueOf(FIELD_SEPARATOR), 2);
            texts.add("1 " + parts[0] + " = " + new BigDecimal(parts[1]).stripTrailingZeros().toPlainString() + " "
                    + baseUnitName);
        }
        return texts;
    }

    public Product findById(long id, boolean includeCost) throws SQLException {
        String sql = "SELECT id, sku, name, category_id, base_unit_id, packaging_spec, cost_price, image_file_id,"
                + " status, description, version FROM products WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new Product(resultSet.getLong("id"), resultSet.getString("sku"), resultSet.getString("name"),
                        resultSet.getLong("category_id"), resultSet.getLong("base_unit_id"),
                        resultSet.getString("packaging_spec"),
                        includeCost ? resultSet.getBigDecimal("cost_price") : null,
                        resultSet.getObject("image_file_id", Long.class), resultSet.getString("status"),
                        resultSet.getString("description"), resultSet.getLong("version"));
            }
        }
    }

    // So khớp không phân biệt hoa thường (collation của bảng) để "sp001" cũng tính là trùng "SP001"
    public boolean existsBySku(String sku, Long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM products WHERE sku = ?" + (excludeId == null ? "" : " AND id <> ?");
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sku);
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean hasTransactions(long productId) throws SQLException {
        String sql = "SELECT " + TRANSACTION_TABLES.stream()
                .map(table -> "EXISTS (SELECT 1 FROM " + table + " WHERE product_id = ?)")
                .collect(Collectors.joining(" OR "));
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= TRANSACTION_TABLES.size(); i++) {
                statement.setLong(i, productId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    // costPrice = null khi thêm bởi người không có quyền giá vốn: dùng mặc định 0 của cột
    public long insert(Connection connection, ProductForm form, BigDecimal costPrice, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO products (sku, name, category_id, base_unit_id, packaging_spec, cost_price, status,"
                + " description, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, form.getSku());
            statement.setString(2, form.getName());
            statement.setLong(3, form.getCategoryId());
            statement.setLong(4, form.getBaseUnitId());
            statement.setString(5, form.getPackagingSpec());
            statement.setBigDecimal(6, costPrice == null ? BigDecimal.ZERO : costPrice);
            statement.setString(7, form.getStatus());
            statement.setString(8, form.getDescription());
            statement.setLong(9, actorUserId);
            statement.setLong(10, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // costPrice = null thì giữ nguyên giá vốn. Trả về false nếu version đã đổi (người khác vừa sửa).
    public boolean update(Connection connection, long id, long version, ProductForm form, BigDecimal costPrice,
                          long actorUserId) throws SQLException {
        String sql = "UPDATE products SET sku = ?, name = ?, category_id = ?, base_unit_id = ?, packaging_spec = ?,"
                + (costPrice == null ? "" : " cost_price = ?,")
                + " status = ?, description = ?, version = version + 1, updated_at = UTC_TIMESTAMP(), updated_by = ?"
                + " WHERE id = ? AND version = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            statement.setString(i++, form.getSku());
            statement.setString(i++, form.getName());
            statement.setLong(i++, form.getCategoryId());
            statement.setLong(i++, form.getBaseUnitId());
            statement.setString(i++, form.getPackagingSpec());
            if (costPrice != null) {
                statement.setBigDecimal(i++, costPrice);
            }
            statement.setString(i++, form.getStatus());
            statement.setString(i++, form.getDescription());
            statement.setLong(i++, actorUserId);
            statement.setLong(i++, id);
            statement.setLong(i, version);
            return statement.executeUpdate() == 1;
        }
    }

    // Đơn vị cơ sở cũng là một dòng is_base của product_units (bảng quy đổi S2-07, hệ số 1)
    public void replaceBaseUnit(Connection connection, long productId, long unitId, long actorUserId)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM product_units WHERE product_id = ? AND is_base")) {
            delete.setLong(1, productId);
            delete.executeUpdate();
        }
        String sql = "INSERT INTO product_units (product_id, unit_id, factor_to_base, is_base, created_at, created_by,"
                + " updated_at, updated_by) VALUES (?, ?, 1, true, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            insert.setLong(1, productId);
            insert.setLong(2, unitId);
            insert.setLong(3, actorUserId);
            insert.setLong(4, actorUserId);
            insert.executeUpdate();
        }
    }

    // Đơn vị quy đổi (không gồm đơn vị cơ sở), hệ số lớn trước: thùng, lốc...
    public List<ProductUnitConversion> findConversions(long productId) throws SQLException {
        String sql = "SELECT pu.unit_id, u.code, u.name, pu.factor_to_base FROM product_units pu"
                + " JOIN units u ON u.id = pu.unit_id WHERE pu.product_id = ? AND NOT pu.is_base"
                + " ORDER BY pu.factor_to_base DESC, u.name";
        List<ProductUnitConversion> conversions = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    conversions.add(new ProductUnitConversion(resultSet.getLong("unit_id"), resultSet.getString("code"),
                            resultSet.getString("name"), resultSet.getBigDecimal("factor_to_base")));
                }
            }
        }
        return conversions;
    }

    // Thay toàn bộ đơn vị quy đổi (giữ dòng đơn vị cơ sở). Giao dịch đã ghi chụp lại hệ số lúc ghi (unit_factor)
    // nên đổi ở đây không làm sai số đã ghi (S2-07).
    public void replaceConversions(Connection connection, long productId, Map<Long, BigDecimal> factors,
                                   long actorUserId) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM product_units WHERE product_id = ? AND NOT is_base")) {
            delete.setLong(1, productId);
            delete.executeUpdate();
        }
        String sql = "INSERT INTO product_units (product_id, unit_id, factor_to_base, is_base, created_at, created_by,"
                + " updated_at, updated_by) VALUES (?, ?, ?, false, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            for (Map.Entry<Long, BigDecimal> entry : factors.entrySet()) {
                insert.setLong(1, productId);
                insert.setLong(2, entry.getKey());
                insert.setBigDecimal(3, entry.getValue());
                insert.setLong(4, actorUserId);
                insert.setLong(5, actorUserId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    // Hệ số của một đơn vị với SKU (đơn vị cơ sở = 1); null nếu SKU không dùng đơn vị này
    public BigDecimal findFactor(long productId, long unitId) throws SQLException {
        String sql = "SELECT CASE WHEN p.base_unit_id = ? THEN 1 ELSE pu.factor_to_base END FROM products p"
                + " LEFT JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = ? WHERE p.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, unitId);
            statement.setLong(2, unitId);
            statement.setLong(3, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal(1) : null;
            }
        }
    }

    public boolean updateStatus(Connection connection, long id, String status, long actorUserId) throws SQLException {
        String sql = "UPDATE products SET status = ?, version = version + 1, updated_at = UTC_TIMESTAMP(),"
                + " updated_by = ? WHERE id = ? AND status <> ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.setString(4, status);
            return statement.executeUpdate() == 1;
        }
    }

    public void updateImage(Connection connection, long id, long fileId, long actorUserId) throws SQLException {
        String sql = "UPDATE products SET image_file_id = ?, version = version + 1, updated_at = UTC_TIMESTAMP(),"
                + " updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, fileId);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    // Gọi khi chưa phát sinh giao dịch: product_units và stock_balances (toàn 0) xoá theo sản phẩm.
    // Còn bảng giá/chính sách chiết khấu tham chiếu thì khoá ngoại chặn (SQLIntegrityConstraintViolationException).
    public boolean delete(Connection connection, long id) throws SQLException {
        for (String table : List.of("product_units", "stock_balances")) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + table + " WHERE product_id = ?")) {
                statement.setLong(1, id);
                statement.executeUpdate();
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM products WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    // storage_key ảnh hiện tại của sản phẩm; null nếu chưa có ảnh
    public String findImageKey(long productId) throws SQLException {
        String sql = "SELECT f.storage_key FROM products p JOIN file_objects f ON f.id = p.image_file_id WHERE p.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    // Chỉ chuyển các sản phẩm còn đang ở nhóm nguồn; trả về số sản phẩm đã chuyển.
    // Tăng version vì products dùng khoá lạc quan (form sửa sản phẩm đang mở sẽ phải tải lại).
    public int moveToCategory(Connection connection, List<Long> productIds, long fromCategoryId, long toCategoryId,
                              long actorUserId) throws SQLException {
        if (productIds.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", Collections.nCopies(productIds.size(), "?"));
        String sql = "UPDATE products SET category_id = ?, version = version + 1, updated_at = UTC_TIMESTAMP(),"
                + " updated_by = ? WHERE category_id = ? AND id IN (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, toCategoryId);
            statement.setLong(2, actorUserId);
            statement.setLong(3, fromCategoryId);
            for (int i = 0; i < productIds.size(); i++) {
                statement.setLong(4 + i, productIds.get(i));
            }
            return statement.executeUpdate();
        }
    }

    // Tồn tính theo kho đang lọc (hoặc mọi kho); sản phẩm chưa có dòng stock_balances có tồn 0
    private static String fromWhere(ProductFilter filter, List<Object> params) {
        StringBuilder sql = new StringBuilder(" FROM products p JOIN product_categories c ON c.id = p.category_id"
                + " JOIN units u ON u.id = p.base_unit_id"
                + " LEFT JOIN (SELECT product_id, SUM(qty_available_base) AS qty, SUM(min_stock_base) AS min_qty"
                + " FROM stock_balances");
        if (filter.getWarehouseId() != null) {
            sql.append(" WHERE warehouse_id = ?");
            params.add(filter.getWarehouseId());
        }
        sql.append(" GROUP BY product_id) s ON s.product_id = p.id");

        List<String> conditions = new ArrayList<>();
        if (filter.getKeyword() != null) {
            String pattern = "%" + escapeLike(filter.getKeyword()) + "%";
            conditions.add("(p.name LIKE ? OR p.sku LIKE ?)");
            params.add(pattern);
            params.add(pattern);
        }
        if (filter.getCategoryPath() != null) {
            conditions.add("c.path LIKE ?");
            params.add(escapeLike(filter.getCategoryPath()) + "%");
        }
        if (filter.getStatus() != null) {
            conditions.add(STOCK_STATUS_SQL + " = ?");
            params.add(filter.getStatus().name());
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        return sql.toString();
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
