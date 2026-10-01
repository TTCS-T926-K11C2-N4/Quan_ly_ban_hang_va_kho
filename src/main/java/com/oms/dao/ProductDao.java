package com.oms.dao;

import com.oms.model.ProductSummary;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Truy vấn sản phẩm. Hiện mới phục vụ màn hình nhóm hàng (S2-06); CRUD sản phẩm thêm ở S2-05.
public class ProductDao {

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
}
