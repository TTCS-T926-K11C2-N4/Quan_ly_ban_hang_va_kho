package com.oms.dao;

import com.oms.model.Supplier;
import com.oms.model.SupplierForm;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Danh mục nhà cung cấp (suppliers, S2-09)
public class SupplierDao {

    // Phiếu nhập và lô hàng trỏ tới nhà cung cấp (khoá ngoại): có thì không xoá được, chỉ ngừng giao dịch
    private static final String HAS_RECEIPTS_SQL = "(EXISTS (SELECT 1 FROM goods_receipts r WHERE r.supplier_id = s.id)"
            + " OR EXISTS (SELECT 1 FROM lots l WHERE l.supplier_id = s.id))";
    private static final String COLUMNS = "s.id, s.code, s.name, s.tax_code, s.contact_name, s.payment_terms, s.status, "
            + HAS_RECEIPTS_SQL + " AS has_receipts";
    private static final String KEYWORD_WHERE = " WHERE (? = '' OR s.code LIKE ? OR s.name LIKE ? OR s.tax_code LIKE ?"
            + " OR s.contact_name LIKE ?)";

    public long count(String keyword) throws SQLException {
        String sql = "SELECT COUNT(*) FROM suppliers s" + KEYWORD_WHERE;
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindKeyword(statement, keyword);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // Đang hoạt động trước, rồi theo mã
    public List<Supplier> findPage(String keyword, int offset, int limit) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM suppliers s" + KEYWORD_WHERE
                + " ORDER BY s.status = 'ACTIVE' DESC, s.code LIMIT ? OFFSET ?";
        List<Supplier> suppliers = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindKeyword(statement, keyword);
            statement.setInt(next++, limit);
            statement.setInt(next, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    suppliers.add(map(resultSet));
                }
            }
        }
        return suppliers;
    }

    public Supplier findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM suppliers s WHERE s.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    // Tên nhà cung cấp đang dùng giá trị này ở cột column (code hoặc tax_code); null nếu chưa ai dùng.
    // So khớp không phân biệt hoa thường (collation của bảng).
    public String findNameUsing(String column, String value, Long excludeId) throws SQLException {
        if (!"code".equals(column) && !"tax_code".equals(column)) {
            throw new IllegalArgumentException(column);
        }
        String sql = "SELECT name FROM suppliers WHERE " + column + " = ?" + (excludeId == null ? "" : " AND id <> ?")
                + " LIMIT 1";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    public long insert(Connection connection, SupplierForm form, long actorUserId) throws SQLException {
        String sql = "INSERT INTO suppliers (code, name, tax_code, contact_name, payment_terms, status, created_at,"
                + " created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindForm(statement, form);
            statement.setLong(7, actorUserId);
            statement.setLong(8, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public boolean update(Connection connection, long id, SupplierForm form, long actorUserId) throws SQLException {
        String sql = "UPDATE suppliers SET code = ?, name = ?, tax_code = ?, contact_name = ?, payment_terms = ?,"
                + " status = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindForm(statement, form);
            statement.setLong(7, actorUserId);
            statement.setLong(8, id);
            return statement.executeUpdate() == 1;
        }
    }

    // false nếu đã ở trạng thái đó (vd bấm hai lần)
    public boolean updateStatus(Connection connection, long id, String status, long actorUserId) throws SQLException {
        String sql = "UPDATE suppliers SET status = ?, updated_at = UTC_TIMESTAMP(), updated_by = ?"
                + " WHERE id = ? AND status <> ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.setString(4, status);
            return statement.executeUpdate() == 1;
        }
    }

    // Ném SQLIntegrityConstraintViolationException nếu vừa có phiếu nhập/lô gắn với nhà cung cấp
    public boolean delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM suppliers WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    // Tìm theo mã, tên, mã số thuế, người liên hệ
    private static int bindKeyword(PreparedStatement statement, String keyword) throws SQLException {
        String like = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        statement.setString(1, keyword);
        for (int i = 2; i <= 5; i++) {
            statement.setString(i, like);
        }
        return 6;
    }

    private static void bindForm(PreparedStatement statement, SupplierForm form) throws SQLException {
        statement.setString(1, form.getCode());
        statement.setString(2, form.getName());
        statement.setString(3, form.getTaxCode());
        statement.setString(4, form.getContactName());
        statement.setString(5, form.getPaymentTerms());
        statement.setString(6, form.getStatus());
    }

    private static Supplier map(ResultSet resultSet) throws SQLException {
        return new Supplier(resultSet.getLong("id"), resultSet.getString("code"), resultSet.getString("name"),
                resultSet.getString("tax_code"), resultSet.getString("contact_name"),
                resultSet.getString("payment_terms"), resultSet.getString("status"),
                resultSet.getBoolean("has_receipts"));
    }
}
