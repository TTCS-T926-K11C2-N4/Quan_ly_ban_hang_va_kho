package com.oms.dao;

import com.oms.model.DraftOrder;
import com.oms.model.OrderForm;
import com.oms.model.OrderProduct;
import com.oms.model.OrderQuote;
import com.oms.model.PricingRules;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Đơn bán hàng (sales_orders, sales_order_items) cho màn tạo đơn S3-09, cùng dữ liệu để tính tiền:
// sản phẩm kèm đơn vị quy đổi, bảng giá đang hiệu lực, chính sách chiết khấu.
public class SalesOrderDao {

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";

    // Sản phẩm đang kinh doanh; đơn vị cơ sở trước rồi đến đơn vị lớn dần
    public List<OrderProduct> findOrderableProducts() throws SQLException {
        String sql = "SELECT p.id, p.sku, p.name, p.category_id, u.id AS unit_id, u.name AS unit_name,"
                + " pu.factor_to_base, pu.is_base FROM products p"
                + " JOIN product_units pu ON pu.product_id = p.id JOIN units u ON u.id = pu.unit_id"
                + " WHERE p.status = 'ACTIVE' ORDER BY p.sku, pu.is_base DESC, pu.factor_to_base, u.name";
        Map<Long, OrderProduct> products = new LinkedHashMap<>();
        Map<Long, List<OrderProduct.Unit>> units = new HashMap<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                units.computeIfAbsent(id, key -> new ArrayList<>()).add(new OrderProduct.Unit(
                        resultSet.getLong("unit_id"), resultSet.getString("unit_name"),
                        resultSet.getBigDecimal("factor_to_base"), resultSet.getBoolean("is_base")));
                products.putIfAbsent(id, new OrderProduct(id, resultSet.getString("sku"), resultSet.getString("name"),
                        resultSet.getLong("category_id"), List.of()));
            }
        }
        List<OrderProduct> result = new ArrayList<>();
        products.forEach((id, product) -> result.add(new OrderProduct(id, product.getSku(), product.getName(),
                product.getCategoryId(), units.get(id))));
        return result;
    }

    // Giá của bảng giá ACTIVE đang hiệu lực vào ngày đó cho nhóm khách hàng, và chiết khấu áp được cho nhóm
    public PricingRules findPricingRules(long customerGroupId, LocalDate date) throws SQLException {
        String priceSql = "SELECT i.id, i.product_id, i.unit_id, i.price, i.floor_price FROM price_lists pl"
                + " JOIN price_list_items i ON i.price_list_id = pl.id WHERE pl.customer_group_id = ?"
                + " AND pl.status = 'ACTIVE' AND pl.valid_from <= ? AND (pl.valid_to IS NULL OR pl.valid_to >= ?)"
                + " ORDER BY pl.valid_from DESC, pl.id DESC";
        String discountSql = "SELECT d.id, d.product_id, d.category_id, d.discount_type, t.min_qty_base,"
                + " t.discount_value FROM discount_policies d JOIN discount_tiers t ON t.policy_id = d.id"
                + " WHERE d.is_active AND d.valid_from <= ? AND (d.valid_to IS NULL OR d.valid_to >= ?)"
                + " AND (d.customer_group_id IS NULL OR d.customer_group_id = ?) ORDER BY d.id, t.min_qty_base";
        // Chính sách theo nhóm hàng áp cho cả nhóm con: path của nhóm con bắt đầu bằng path của nhóm cha (/20/ -> /20/25/)
        String categorySql = "SELECT id, path FROM product_categories";
        Map<String, PricingRules.PriceItem> prices = new HashMap<>();
        Map<Long, String> categoryPaths = new HashMap<>();
        Map<Long, Object[]> policyHeads = new LinkedHashMap<>();
        Map<Long, List<PricingRules.Tier>> tiers = new HashMap<>();
        try (Connection connection = DbConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(priceSql)) {
                statement.setLong(1, customerGroupId);
                statement.setDate(2, Date.valueOf(date));
                statement.setDate(3, Date.valueOf(date));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        // Hai bảng cùng hiệu lực không xảy ra (S2-10 chặn chồng ngày); nếu có thì lấy bảng mới nhất
                        prices.putIfAbsent(PricingRules.key(resultSet.getLong("product_id"),
                                        resultSet.getLong("unit_id")),
                                new PricingRules.PriceItem(resultSet.getLong("id"), resultSet.getBigDecimal("price"),
                                        resultSet.getBigDecimal("floor_price")));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(categorySql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categoryPaths.put(resultSet.getLong("id"), resultSet.getString("path"));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(discountSql)) {
                statement.setDate(1, Date.valueOf(date));
                statement.setDate(2, Date.valueOf(date));
                statement.setLong(3, customerGroupId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        long id = resultSet.getLong("id");
                        policyHeads.putIfAbsent(id, new Object[] {resultSet.getObject("product_id", Long.class),
                                resultSet.getObject("category_id", Long.class), resultSet.getString("discount_type")});
                        tiers.computeIfAbsent(id, key -> new ArrayList<>()).add(new PricingRules.Tier(
                                resultSet.getBigDecimal("min_qty_base"), resultSet.getBigDecimal("discount_value")));
                    }
                }
            }
        }
        List<PricingRules.DiscountPolicy> policies = new ArrayList<>();
        policyHeads.forEach((id, head) -> policies.add(new PricingRules.DiscountPolicy(id, (Long) head[0],
                (Long) head[1], subtree((Long) head[1], categoryPaths), (String) head[2], tiers.get(id))));
        return new PricingRules(prices, policies);
    }

    // Nhóm hàng categoryId cùng mọi nhóm con; rỗng nếu chính sách không theo nhóm hàng
    static Set<Long> subtree(Long categoryId, Map<Long, String> categoryPaths) {
        String path = categoryId == null ? null : categoryPaths.get(categoryId);
        if (path == null) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        categoryPaths.forEach((id, candidate) -> {
            if (candidate.startsWith(path)) {
                ids.add(id);
            }
        });
        return ids;
    }

    // Số thứ tự lớn nhất của các mã đơn bắt đầu bằng prefix (vd DH261007-0012 -> 12); 0 nếu chưa có
    public int findMaxOrderNumber(String prefix) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(order_no, ?) AS UNSIGNED)), 0) FROM sales_orders"
                + " WHERE order_no LIKE ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, prefix.length() + 1);
            statement.setString(2, prefix + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    // Ném SQLIntegrityConstraintViolationException nếu orderNo vừa bị đơn khác dùng
    public long insert(Connection connection, String orderNo, Header header, long actorUserId) throws SQLException {
        String sql = "INSERT INTO sales_orders (order_no, customer_id, delivery_address_id, warehouse_id, sales_rep_id,"
                + " source, status, requested_delivery_date, subtotal_amount, discount_amount, total_amount, note,"
                + " submitted_at, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, 'SALES_REP', ?, ?, ?, ?, ?, ?, " + submittedAt(header)
                + ", UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, orderNo);
            int next = bindHeader(statement, 2, header);
            statement.setLong(next++, actorUserId);
            statement.setLong(next, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // Chỉ sửa được đơn còn nháp và đúng version lúc mở; false nếu đơn vừa được sửa/gửi ở nơi khác
    public boolean updateDraft(Connection connection, long id, long version, Header header, long actorUserId)
            throws SQLException {
        String sql = "UPDATE sales_orders SET customer_id = ?, delivery_address_id = ?, warehouse_id = ?,"
                + " sales_rep_id = ?, status = ?, requested_delivery_date = ?, subtotal_amount = ?,"
                + " discount_amount = ?, total_amount = ?, note = ?, submitted_at = " + submittedAt(header) + ","
                + " version = version + 1, updated_at = UTC_TIMESTAMP(), updated_by = ?"
                + " WHERE id = ? AND version = ? AND status = 'DRAFT'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindHeader(statement, 1, header);
            statement.setLong(next++, actorUserId);
            statement.setLong(next++, id);
            statement.setLong(next, version);
            return statement.executeUpdate() == 1;
        }
    }

    public void deleteItems(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM sales_order_items WHERE order_id = ?")) {
            statement.setLong(1, orderId);
            statement.executeUpdate();
        }
    }

    // Chụp lại đơn vị, hệ số, giá, giá sàn, chiết khấu của dòng để đổi bảng giá/hệ số sau này không làm sai đơn
    public void insertItem(Connection connection, long orderId, int lineNo, OrderQuote.Line line, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO sales_order_items (order_id, line_no, product_id, unit_id, unit_factor, qty, qty_base,"
                + " unit_price, floor_price, price_list_item_id, discount_policy_id, discount_amount, line_total,"
                + " created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setInt(2, lineNo);
            statement.setLong(3, line.getProduct().getId());
            statement.setLong(4, line.getUnit().getId());
            statement.setBigDecimal(5, line.getUnit().getFactor());
            statement.setBigDecimal(6, line.getQty());
            statement.setBigDecimal(7, line.getQtyBase());
            statement.setBigDecimal(8, line.getUnitPrice());
            statement.setBigDecimal(9, line.getFloorPrice());
            setNullableLong(statement, 10, line.getPriceListItemId());
            setNullableLong(statement, 11, line.getDiscountPolicyId());
            statement.setBigDecimal(12, line.getDiscount());
            statement.setBigDecimal(13, line.getTotal());
            statement.setLong(14, actorUserId);
            statement.setLong(15, actorUserId);
            statement.executeUpdate();
        }
    }

    // Đơn nháp mới sửa gần nhất; salesRepId = null: mọi đơn nháp, ngược lại chỉ đơn của đại lý người đó phụ trách
    public List<DraftOrder> findDrafts(Long salesRepId, int limit) throws SQLException {
        String sql = "SELECT o.id, o.order_no, c.name, c.is_blocked, o.total_amount, o.updated_at FROM sales_orders o"
                + " JOIN customers c ON c.id = o.customer_id WHERE o.status = 'DRAFT'"
                + (salesRepId == null ? "" : " AND c.sales_rep_id = ?") + " ORDER BY o.updated_at DESC, o.id DESC"
                + " LIMIT ?";
        List<DraftOrder> drafts = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = 1;
            if (salesRepId != null) {
                statement.setLong(next++, salesRepId);
            }
            statement.setInt(next, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    drafts.add(new DraftOrder(resultSet.getLong("id"), resultSet.getString("order_no"),
                            resultSet.getString("name"), resultSet.getBoolean("is_blocked"),
                            resultSet.getBigDecimal("total_amount"),
                            resultSet.getObject("updated_at", LocalDateTime.class)));
                }
            }
        }
        return drafts;
    }

    // Đơn nháp dạng form để mở lại; null nếu không có đơn này hoặc đơn không còn là nháp
    public Draft findDraft(long id) throws SQLException {
        String headerSql = "SELECT o.customer_id, c.code AS customer_code, o.order_no, o.delivery_address_id,"
                + " o.requested_delivery_date, o.note, o.subtotal_amount, o.discount_amount, o.total_amount, o.version"
                + " FROM sales_orders o JOIN customers c ON c.id = o.customer_id WHERE o.id = ? AND o.status = 'DRAFT'";
        String itemSql = "SELECT i.product_id, p.sku, p.name, i.unit_id, i.qty FROM sales_order_items i"
                + " JOIN products p ON p.id = i.product_id WHERE i.order_id = ? ORDER BY i.line_no";
        try (Connection connection = DbConnection.getConnection()) {
            long customerId;
            String customerCode;
            String orderNo;
            String deliveryAddressId;
            String requestedDate;
            String note;
            BigDecimal subtotal;
            BigDecimal discount;
            BigDecimal total;
            long version;
            try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    customerId = resultSet.getLong("customer_id");
                    customerCode = resultSet.getString("customer_code");
                    orderNo = resultSet.getString("order_no");
                    deliveryAddressId = String.valueOf(resultSet.getLong("delivery_address_id"));
                    LocalDate date = resultSet.getObject("requested_delivery_date", LocalDate.class);
                    requestedDate = date == null ? null : date.toString();
                    note = resultSet.getString("note");
                    subtotal = resultSet.getBigDecimal("subtotal_amount");
                    discount = resultSet.getBigDecimal("discount_amount");
                    total = resultSet.getBigDecimal("total_amount");
                    version = resultSet.getLong("version");
                }
            }
            List<OrderForm.Line> lines = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        lines.add(new OrderForm.Line(String.valueOf(resultSet.getLong("product_id")),
                                resultSet.getString("sku") + " - " + resultSet.getString("name"),
                                String.valueOf(resultSet.getLong("unit_id")),
                                resultSet.getBigDecimal("qty").stripTrailingZeros().toPlainString()));
                    }
                }
            }
            return new Draft(customerId, customerCode, orderNo, new OrderForm(String.valueOf(customerId),
                    deliveryAddressId, requestedDate, note, id, String.valueOf(version), lines), subtotal, discount, total);
        }
    }

    private static String submittedAt(Header header) {
        return header.submitted ? "UTC_TIMESTAMP()" : "NULL";
    }

    private static int bindHeader(PreparedStatement statement, int start, Header header) throws SQLException {
        int index = start;
        statement.setLong(index++, header.customerId);
        statement.setLong(index++, header.deliveryAddressId);
        statement.setLong(index++, header.warehouseId);
        setNullableLong(statement, index++, header.salesRepId);
        statement.setString(index++, header.submitted ? PENDING_APPROVAL : DRAFT);
        if (header.requestedDate == null) {
            statement.setNull(index++, Types.DATE);
        } else {
            statement.setDate(index++, Date.valueOf(header.requestedDate));
        }
        statement.setBigDecimal(index++, header.subtotal);
        statement.setBigDecimal(index++, header.discount);
        statement.setBigDecimal(index++, header.total);
        statement.setString(index++, header.note);
        return index;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    // Phần đầu đơn; submitted = true: gửi đơn (chờ duyệt), false: lưu nháp
    public static class Header {
        final long customerId;
        final long deliveryAddressId;
        final long warehouseId;
        final Long salesRepId;
        final boolean submitted;
        final LocalDate requestedDate;
        final BigDecimal subtotal;
        final BigDecimal discount;
        final BigDecimal total;
        final String note;

        public Header(long customerId, long deliveryAddressId, long warehouseId, Long salesRepId, boolean submitted,
                      LocalDate requestedDate, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                      String note) {
            this.customerId = customerId;
            this.deliveryAddressId = deliveryAddressId;
            this.warehouseId = warehouseId;
            this.salesRepId = salesRepId;
            this.submitted = submitted;
            this.requestedDate = requestedDate;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
            this.note = note;
        }
    }

    // Tiền hàng, chiết khấu, tổng là số đã lưu của đơn nháp, để ghi giá trị trước khi sửa vào nhật ký
    public static class Draft {
        private final long customerId;
        private final String customerCode;
        private final String orderNo;
        private final OrderForm form;
        private final BigDecimal subtotal;
        private final BigDecimal discount;
        private final BigDecimal total;

        public Draft(long customerId, String customerCode, String orderNo, OrderForm form, BigDecimal subtotal,
                     BigDecimal discount, BigDecimal total) {
            this.customerId = customerId;
            this.customerCode = customerCode;
            this.orderNo = orderNo;
            this.form = form;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }

        public long getCustomerId() {
            return customerId;
        }

        public String getCustomerCode() {
            return customerCode;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public OrderForm getForm() {
            return form;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public BigDecimal getDiscount() {
            return discount;
        }

        public BigDecimal getTotal() {
            return total;
        }
    }
}
