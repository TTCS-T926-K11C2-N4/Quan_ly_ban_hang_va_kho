package com.oms.dao;

import com.oms.model.Supplier;
import com.oms.model.SupplierFilter;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SupplierDao {

    // Lô hàng cũng tham chiếu nhà cung cấp (khoá ngoại) nên tính như đã có phiếu nhập
    private static final String HAS_RECEIPTS_SQL =
            "(EXISTS (SELECT 1 FROM goods_receipts gr WHERE gr.supplier_id = s.id)"
            + " OR EXISTS (SELECT 1 FROM lots l WHERE l.supplier_id = s.id))";

    private static final String SELECT_SQL = "SELECT s.id, s.code, s.name, s.tax_code, s.contact_name, s.phone,"
            + " s.email, s.address, s.payment_terms, s.status, " + HAS_RECEIPTS_SQL + " AS has_receipts"
            + " FROM suppliers s";

    public long count(SupplierFilter filter) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM suppliers s" + buildWhere(filter, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public List<Supplier> findPage(SupplierFilter filter, int offset, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = SELECT_SQL + buildWhere(filter, params) + " ORDER BY s.code LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        List<Supplier> suppliers = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    suppliers.add(map(resultSet));
                }
            }
        }
        return suppliers;
    }

    public Supplier findById(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SQL + " WHERE s.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    // excludeId: khi sửa, mã của chính nhà cung cấp đó không tính là trùng
    public boolean existsByCode(String code, Long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM suppliers WHERE code = ? AND id <> ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.setLong(2, excludeId == null ? 0 : excludeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void insert(Supplier supplier, long actorUserId) throws SQLException {
        String sql = "INSERT INTO suppliers (code, name, tax_code, contact_name, phone, email, address, payment_terms,"
                + " status, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindFields(statement, supplier);
            statement.setLong(9, actorUserId);
            statement.setLong(10, actorUserId);
            statement.executeUpdate();
        }
    }

    // Trạng thái không đổi ở đây: ngừng/mở lại giao dịch dùng updateStatus
    public void update(Supplier supplier, long actorUserId) throws SQLException {
        String sql = "UPDATE suppliers SET code = ?, name = ?, tax_code = ?, contact_name = ?, phone = ?, email = ?,"
                + " address = ?, payment_terms = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindFields(statement, supplier);
            statement.setLong(9, actorUserId);
            statement.setLong(10, supplier.getId());
            statement.executeUpdate();
        }
    }

    // Trả về false nếu nhà cung cấp đã ở trạng thái đó (vd người khác vừa đổi)
    public boolean updateStatus(long id, String status, long actorUserId) throws SQLException {
        String sql = "UPDATE suppliers SET status = ?, updated_at = UTC_TIMESTAMP(), updated_by = ?"
                + " WHERE id = ? AND status <> ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.setString(4, status);
            return statement.executeUpdate() > 0;
        }
    }

    // Chỉ xoá khi chưa có phiếu nhập; kiểm tra ngay trong câu DELETE để không lọt khi vừa có phiếu nhập mới.
    // Trả về false nếu không xoá (đã có phiếu nhập hoặc không còn tồn tại).
    public boolean deleteIfNoReceipts(long id) throws SQLException {
        String sql = "DELETE s FROM suppliers s WHERE s.id = ? AND NOT " + HAS_RECEIPTS_SQL;
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private static void bindFields(PreparedStatement statement, Supplier supplier) throws SQLException {
        statement.setString(1, supplier.getCode());
        statement.setString(2, supplier.getName());
        statement.setString(3, supplier.getTaxCode());
        statement.setString(4, supplier.getContactName());
        statement.setString(5, supplier.getPhone());
        statement.setString(6, supplier.getEmail());
        statement.setString(7, supplier.getAddress());
        statement.setString(8, supplier.getPaymentTerms());
    }

    private static Supplier map(ResultSet resultSet) throws SQLException {
        return new Supplier(resultSet.getLong("id"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("tax_code"),
                resultSet.getString("contact_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getString("address"),
                resultSet.getString("payment_terms"),
                resultSet.getString("status"),
                resultSet.getBoolean("has_receipts"));
    }

    private String buildWhere(SupplierFilter filter, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (filter.getKeyword() != null) {
            String pattern = "%" + escapeLike(filter.getKeyword()) + "%";
            conditions.add("(s.code LIKE ? OR s.name LIKE ? OR s.tax_code LIKE ? OR s.contact_name LIKE ?"
                    + " OR s.phone LIKE ?)");
            for (int i = 0; i < 5; i++) {
                params.add(pattern);
            }
        }
        if (filter.getStatus() != null) {
            conditions.add("s.status = ?");
            params.add(filter.getStatus());
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
