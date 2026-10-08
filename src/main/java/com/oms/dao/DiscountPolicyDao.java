package com.oms.dao;

import com.oms.model.DiscountPolicyRow;
import com.oms.model.DiscountPolicyStatus;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Chính sách chiết khấu theo sản lượng (discount_policies + discount_tiers, S3-01). Màn tạo đơn (S3-09) đọc chính
// sách đang áp dụng qua SalesOrderDao.findPricingRules; dòng đơn ghi discount_policy_id nên chính sách đã dùng
// không xoá được (khoá ngoại), chỉ ngừng áp dụng.
public class DiscountPolicyDao {

    private static final String FROM = " FROM discount_policies d LEFT JOIN customer_groups g ON g.id = d.customer_group_id"
            + " LEFT JOIN products p ON p.id = d.product_id LEFT JOIN product_categories c ON c.id = d.category_id";
    private static final String COLUMNS = "d.id, d.code, d.name, d.customer_group_id, g.name AS group_name, d.scope_type,"
            + " d.category_id, COALESCE(p.sku, c.code) AS target_code, COALESCE(p.name, c.name) AS target_name,"
            + " d.discount_type, d.valid_from, d.valid_to, d.is_active,"
            + " EXISTS (SELECT 1 FROM sales_order_items i WHERE i.discount_policy_id = d.id) AS used";

    // groupId = 0: chỉ chính sách áp cho mọi nhóm khách hàng
    public long count(String keyword, Long groupId, DiscountPolicyStatus status, LocalDate today) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*)" + FROM + buildWhere(keyword, groupId, status, today, params);
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public List<DiscountPolicyRow> findPage(String keyword, Long groupId, DiscountPolicyStatus status, LocalDate today,
                                            int offset, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + FROM + buildWhere(keyword, groupId, status, today, params)
                + " ORDER BY d.is_active DESC, d.valid_from DESC, d.id DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);
        try (Connection connection = DbConnection.getConnection()) {
            List<Object[]> heads = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        heads.add(readHead(resultSet));
                    }
                }
            }
            Map<Long, List<DiscountPolicyRow.Tier>> tiers = findTiers(connection,
                    heads.stream().map(head -> (Long) head[0]).toList());
            List<DiscountPolicyRow> rows = new ArrayList<>();
            for (Object[] head : heads) {
                rows.add(toRow(head, tiers.getOrDefault((Long) head[0], List.of())));
            }
            return rows;
        }
    }

    public DiscountPolicyRow findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM + " WHERE d.id = ?";
        try (Connection connection = DbConnection.getConnection()) {
            Object[] head;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    head = readHead(resultSet);
                }
            }
            return toRow(head, findTiers(connection, List.of(id)).getOrDefault(id, List.of()));
        }
    }

    // SKU đang kinh doanh để gợi ý khi chọn "Áp cho 1 SKU"
    public List<SelectOption> findProductOptions() throws SQLException {
        List<SelectOption> products = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, sku, name FROM products WHERE status = 'ACTIVE' ORDER BY sku");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("sku"),
                        resultSet.getString("name")));
            }
        }
        return products;
    }

    // Số lớn nhất của mã dạng CK-0012 -> 12; 0 nếu chưa có
    public int findMaxCodeNumber() throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COALESCE(MAX(CAST(SUBSTRING(code, 4) AS UNSIGNED)), 0) FROM discount_policies"
                             + " WHERE code LIKE 'CK-%'");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public long insert(Connection connection, String code, Values values, long actorUserId) throws SQLException {
        String sql = "INSERT INTO discount_policies (code, name, scope_type, product_id, category_id, discount_type,"
                + " customer_group_id, valid_from, valid_to, is_active, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, code);
            int index = bindValues(statement, 2, values);
            statement.setLong(index++, actorUserId);
            statement.setLong(index, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, long id, Values values, long actorUserId) throws SQLException {
        String sql = "UPDATE discount_policies SET name = ?, scope_type = ?, product_id = ?, category_id = ?,"
                + " discount_type = ?, customer_group_id = ?, valid_from = ?, valid_to = ?, is_active = ?,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindValues(statement, 1, values);
            statement.setLong(index++, actorUserId);
            statement.setLong(index, id);
            statement.executeUpdate();
        }
    }

    public void replaceTiers(Connection connection, long policyId, List<DiscountPolicyRow.Tier> tiers)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM discount_tiers WHERE policy_id = ?")) {
            statement.setLong(1, policyId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO discount_tiers (policy_id, min_qty_base, discount_value) VALUES (?, ?, ?)")) {
            for (DiscountPolicyRow.Tier tier : tiers) {
                statement.setLong(1, policyId);
                statement.setBigDecimal(2, tier.minQtyBase());
                statement.setBigDecimal(3, tier.value());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public void setActive(Connection connection, long id, boolean active, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE discount_policies SET is_active = ?,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setBoolean(1, active);
            statement.setLong(2, actorUserId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    public void delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM discount_tiers WHERE policy_id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM discount_policies WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    // Giá trị đã kiểm tra của form, ghi vào discount_policies
    public record Values(String name, String scopeType, Long productId, Long categoryId, String discountType,
                         Long customerGroupId, LocalDate validFrom, LocalDate validTo, boolean active) {
    }

    private static int bindValues(PreparedStatement statement, int start, Values values) throws SQLException {
        int index = start;
        statement.setString(index++, values.name());
        statement.setString(index++, values.scopeType());
        setNullableLong(statement, index++, values.productId());
        setNullableLong(statement, index++, values.categoryId());
        statement.setString(index++, values.discountType());
        setNullableLong(statement, index++, values.customerGroupId());
        statement.setDate(index++, Date.valueOf(values.validFrom()));
        if (values.validTo() == null) {
            statement.setNull(index++, Types.DATE);
        } else {
            statement.setDate(index++, Date.valueOf(values.validTo()));
        }
        statement.setBoolean(index++, values.active());
        return index;
    }

    private static Map<Long, List<DiscountPolicyRow.Tier>> findTiers(Connection connection, List<Long> policyIds)
            throws SQLException {
        Map<Long, List<DiscountPolicyRow.Tier>> tiers = new HashMap<>();
        if (policyIds.isEmpty()) {
            return tiers;
        }
        String placeholders = String.join(", ", Collections.nCopies(policyIds.size(), "?"));
        try (PreparedStatement statement = connection.prepareStatement("SELECT policy_id, min_qty_base, discount_value"
                + " FROM discount_tiers WHERE policy_id IN (" + placeholders + ") ORDER BY policy_id, min_qty_base")) {
            bind(statement, policyIds);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tiers.computeIfAbsent(resultSet.getLong("policy_id"), key -> new ArrayList<>())
                            .add(new DiscountPolicyRow.Tier(resultSet.getBigDecimal("min_qty_base"),
                                    resultSet.getBigDecimal("discount_value")));
                }
            }
        }
        return tiers;
    }

    private static Object[] readHead(ResultSet resultSet) throws SQLException {
        return new Object[] {resultSet.getLong("id"), resultSet.getString("code"), resultSet.getString("name"),
                resultSet.getObject("customer_group_id", Long.class), resultSet.getString("group_name"),
                resultSet.getString("scope_type"), resultSet.getObject("category_id", Long.class),
                resultSet.getString("target_code"), resultSet.getString("target_name"),
                resultSet.getString("discount_type"), resultSet.getObject("valid_from", LocalDate.class),
                resultSet.getObject("valid_to", LocalDate.class), resultSet.getBoolean("is_active"),
                resultSet.getBoolean("used")};
    }

    private static DiscountPolicyRow toRow(Object[] head, List<DiscountPolicyRow.Tier> tiers) {
        return new DiscountPolicyRow((Long) head[0], (String) head[1], (String) head[2], (Long) head[3],
                (String) head[4], (String) head[5], (Long) head[6], (String) head[7], (String) head[8],
                (String) head[9], (LocalDate) head[10], (LocalDate) head[11], (Boolean) head[12], (Boolean) head[13],
                tiers);
    }

    private static String buildWhere(String keyword, Long groupId, DiscountPolicyStatus status, LocalDate today,
                                     List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (keyword != null) {
            String pattern = "%" + escapeLike(keyword) + "%";
            conditions.add("(d.code LIKE ? OR d.name LIKE ? OR p.sku LIKE ? OR c.name LIKE ?)");
            for (int i = 0; i < 4; i++) {
                params.add(pattern);
            }
        }
        if (groupId != null) {
            if (groupId == 0) {
                conditions.add("d.customer_group_id IS NULL");
            } else {
                conditions.add("d.customer_group_id = ?");
                params.add(groupId);
            }
        }
        if (status != null) {
            Date day = Date.valueOf(today);
            switch (status) {
                case INACTIVE -> conditions.add("NOT d.is_active");
                case UPCOMING -> {
                    conditions.add("d.is_active AND d.valid_from > ?");
                    params.add(day);
                }
                case ACTIVE -> {
                    conditions.add("d.is_active AND d.valid_from <= ? AND (d.valid_to IS NULL OR d.valid_to >= ?)");
                    params.add(day);
                    params.add(day);
                }
                case EXPIRED -> {
                    conditions.add("d.is_active AND d.valid_to < ?");
                    params.add(day);
                }
            }
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    // Người dùng gõ % hoặc _ thì tìm đúng ký tự đó, không để thành ký tự đại diện của LIKE
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void bind(PreparedStatement statement, List<?> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }
}
