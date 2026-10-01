package com.oms.dao;

import com.oms.model.ProductCategory;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// Nhóm hàng dạng cây (S2-06). path lưu chuỗi id từ gốc (vd /1/5/12/) để lấy cả nhánh bằng LIKE 'path%'.
public class ProductCategoryDao {

    // Đếm sẵn sản phẩm trực tiếp, số nhóm con và sản phẩm của cả nhánh cho màn hình cây
    private static final String SELECT_WITH_COUNTS = "SELECT c.id, c.parent_id, c.code, c.name, c.description, c.level, c.path,"
            + " c.sort_order, c.is_active,"
            + " (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id) AS product_count,"
            + " (SELECT COUNT(*) FROM product_categories ch WHERE ch.parent_id = c.id) AS child_count,"
            + " (SELECT COUNT(*) FROM products p JOIN product_categories d ON d.id = p.category_id"
            + "  WHERE d.path LIKE CONCAT(c.path, '%')) AS branch_product_count"
            + " FROM product_categories c";

    public List<ProductCategory> findAll() throws SQLException {
        List<ProductCategory> categories = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WITH_COUNTS);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                categories.add(map(resultSet));
            }
        }
        return categories;
    }

    public ProductCategory findById(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WITH_COUNTS + " WHERE c.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    // excludeId: khi sửa, mã của chính nhóm đó không tính là trùng
    public boolean existsByCode(String code, Long excludeId) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM product_categories WHERE code = ? AND id <> ?")) {
            statement.setString(1, code);
            statement.setLong(2, excludeIdOrZero(excludeId));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Hai nhóm cùng cha không được trùng tên. Collation mặc định (ai_ci) coi "Bia" = "Bìa" nên so theo as_ci:
    // không phân biệt hoa thường nhưng phân biệt dấu tiếng Việt.
    public boolean existsNameInParent(String name, Long parentId, Long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM product_categories WHERE name COLLATE utf8mb4_0900_as_ci = ?"
                + " AND id <> ? AND parent_id <=> ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setLong(2, excludeIdOrZero(excludeId));
            setNullableLong(statement, 3, parentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Cấp sâu nhất trong nhánh có path cho trước, để kiểm tra chuyển cả nhánh không vượt quá số cấp cho phép
    public int findMaxLevelInBranch(String path) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT MAX(level) FROM product_categories WHERE path LIKE CONCAT(?, '%')")) {
            statement.setString(1, path);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    // path cần id mới sinh nên ghi tạm rồi cập nhật lại bằng updatePath trong cùng transaction
    public long insert(Connection connection, Long parentId, String code, String name, String description, int level,
                       int sortOrder, long actorUserId) throws SQLException {
        String sql = "INSERT INTO product_categories (parent_id, code, name, description, level, path, sort_order,"
                + " is_active, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, '', ?, true, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableLong(statement, 1, parentId);
            statement.setString(2, code);
            statement.setString(3, name);
            statement.setString(4, description);
            statement.setInt(5, level);
            statement.setInt(6, sortOrder);
            statement.setLong(7, actorUserId);
            statement.setLong(8, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updatePath(Connection connection, long id, String path) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE product_categories SET path = ? WHERE id = ?")) {
            statement.setString(1, path);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void update(Connection connection, long id, Long parentId, String code, String name, String description,
                       int sortOrder, long actorUserId) throws SQLException {
        String sql = "UPDATE product_categories SET parent_id = ?, code = ?, name = ?, description = ?, sort_order = ?,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableLong(statement, 1, parentId);
            statement.setString(2, code);
            statement.setString(3, name);
            statement.setString(4, description);
            statement.setInt(5, sortOrder);
            statement.setLong(6, actorUserId);
            statement.setLong(7, id);
            statement.executeUpdate();
        }
    }

    // Chuyển cả nhánh: thay tiền tố path cũ bằng path mới và cộng chênh lệch cấp cho mọi nhóm trong nhánh
    public void moveBranch(Connection connection, String oldPath, String newPath, int levelDelta) throws SQLException {
        String sql = "UPDATE product_categories SET path = CONCAT(?, SUBSTRING(path, ?)), level = level + ?"
                + " WHERE path LIKE CONCAT(?, '%')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPath);
            statement.setInt(2, oldPath.length() + 1);
            statement.setInt(3, levelDelta);
            statement.setString(4, oldPath);
            statement.executeUpdate();
        }
    }

    // Ngừng sử dụng áp dụng cho cả nhánh (nhóm con của nhóm đã ngừng cũng không được chọn nữa)
    public void deactivateBranch(Connection connection, String path, long actorUserId) throws SQLException {
        String sql = "UPDATE product_categories SET is_active = false, updated_at = UTC_TIMESTAMP(), updated_by = ?"
                + " WHERE path LIKE CONCAT(?, '%')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actorUserId);
            statement.setString(2, path);
            statement.executeUpdate();
        }
    }

    // Dùng lại chỉ bật đúng nhóm này; nhóm con muốn dùng lại thì bật từng nhóm
    public void activate(Connection connection, long id, long actorUserId) throws SQLException {
        String sql = "UPDATE product_categories SET is_active = true, updated_at = UTC_TIMESTAMP(), updated_by = ?"
                + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    // Trả về false nếu nhóm còn sản phẩm hoặc nhóm con (kiểm tra lại ngay trong câu DELETE để tránh tranh chấp)
    public boolean deleteIfEmpty(Connection connection, long id) throws SQLException {
        String sql = "DELETE FROM product_categories WHERE id = ?"
                + " AND NOT EXISTS (SELECT 1 FROM products WHERE category_id = ?)"
                + " AND NOT EXISTS (SELECT 1 FROM (SELECT parent_id FROM product_categories) ch WHERE ch.parent_id = ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, id);
            statement.setLong(3, id);
            return statement.executeUpdate() == 1;
        }
    }

    // id luôn >= 1 nên 0 nghĩa là không loại trừ nhóm nào
    private static long excludeIdOrZero(Long excludeId) {
        return excludeId == null ? 0 : excludeId;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static ProductCategory map(ResultSet resultSet) throws SQLException {
        // wasNull() chỉ đúng ngay sau lần đọc cột parent_id nên phải tính trước khi đọc cột khác
        long parentValue = resultSet.getLong("parent_id");
        Long parentId = resultSet.wasNull() ? null : parentValue;
        return new ProductCategory(resultSet.getLong("id"), parentId,
                resultSet.getString("code"), resultSet.getString("name"), resultSet.getString("description"),
                resultSet.getInt("level"),
                resultSet.getString("path"), resultSet.getInt("sort_order"), resultSet.getBoolean("is_active"),
                resultSet.getInt("product_count"), resultSet.getInt("child_count"),
                resultSet.getInt("branch_product_count"));
    }
}
