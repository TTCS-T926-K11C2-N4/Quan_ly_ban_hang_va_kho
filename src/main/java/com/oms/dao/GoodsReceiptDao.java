package com.oms.dao;

import com.oms.model.GoodsReceiptDetail;
import com.oms.model.GoodsReceiptFilter;
import com.oms.model.GoodsReceiptRow;
import com.oms.model.ReceiptProduct;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Phiếu nhập kho (goods_receipts, goods_receipt_items, S5-04) và các bước ghi sổ: cộng sổ tồn (stock_balances),
// tồn theo lô (stock_lot_balances) và ghi thẻ kho (stock_movements). Các hàm nhận Connection chạy trong
// transaction do GoodsReceiptService mở.
public class GoodsReceiptDao {

    private static final String DOC_TYPE = "RECEIPT";
    private static final String NO_PREFIX = "PN-";
    // SKU không quản lý lô dùng đúng một lô mặc định để mọi phát sinh kho đều có lot_id (xem COMMENT bảng lots)
    private static final String DEFAULT_LOT_NO = "MAC-DINH";
    private static final String REF_TYPE = "GOODS_RECEIPT";

    private static final String LIST_FROM = " FROM goods_receipts r JOIN suppliers s ON s.id = r.supplier_id"
            + " JOIN warehouses w ON w.id = r.warehouse_id LEFT JOIN goods_receipts a ON a.id = r.adjusts_receipt_id";
    private static final String LIST_WHERE = " WHERE r.status <> 'CANCELLED'"
            + " AND (? = '' OR r.receipt_no LIKE ? OR s.name LIKE ? OR s.code LIKE ? OR r.document_ref LIKE ?)"
            + " AND (? IS NULL OR r.status = ?) AND (? IS NULL OR r.warehouse_id = ?) AND (? IS NULL OR r.receipt_date = ?)";
    private static final String TOTAL_SQL =
            "COALESCE((SELECT SUM(i.qty * i.unit_price) FROM goods_receipt_items i WHERE i.receipt_id = r.id), 0)";

    // Đầu phiếu khi lưu
    public record Header(long supplierId, long warehouseId, String documentRef, LocalDate receiptDate, String note,
                         Long adjustsReceiptId) {
    }

    // Dòng hàng khi lưu: số lượng theo đơn vị nhập và quy về đơn vị cơ sở
    public record ItemRow(long productId, long lotId, long unitId, BigDecimal unitFactor, BigDecimal qty,
                          BigDecimal qtyBase, BigDecimal unitPrice, Long locationId) {
    }

    // Dòng hàng khi ghi sổ
    public record PostingItem(long productId, String sku, long lotId, String lotNo, BigDecimal qtyBase, Long locationId) {
    }

    // Lô đã có của một SKU (để báo khi nhập cùng số lô mà hạn sử dụng khác)
    public record ExistingLot(long id, LocalDate expiryDate) {
    }

    public long count(GoodsReceiptFilter filter) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*)" + LIST_FROM + LIST_WHERE)) {
            bindFilter(statement, filter);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    // Mới nhất trước; limit <= 0: lấy hết (xuất Excel)
    public List<GoodsReceiptRow> findPage(GoodsReceiptFilter filter, int offset, int limit) throws SQLException {
        String sql = "SELECT r.id, r.receipt_no, s.name AS supplier_name, w.code AS warehouse_code,"
                + " w.name AS warehouse_name, r.receipt_date, r.status, a.receipt_no AS adjusts_no, " + TOTAL_SQL
                + " AS total" + LIST_FROM + LIST_WHERE + " ORDER BY r.receipt_date DESC, r.id DESC"
                + (limit > 0 ? " LIMIT ? OFFSET ?" : "");
        List<GoodsReceiptRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindFilter(statement, filter);
            if (limit > 0) {
                statement.setInt(next++, limit);
                statement.setInt(next, offset);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new GoodsReceiptRow(resultSet.getLong("id"), resultSet.getString("receipt_no"),
                            resultSet.getString("supplier_name"), resultSet.getString("warehouse_code"),
                            resultSet.getString("warehouse_name"), resultSet.getObject("receipt_date", LocalDate.class),
                            resultSet.getBigDecimal("total"), resultSet.getString("status"),
                            resultSet.getString("adjusts_no")));
                }
            }
        }
        return rows;
    }

    // Ba thẻ số liệu: tổng số phiếu (trừ phiếu huỷ), phiếu nháp, phiếu đã ghi sổ
    public long[] countByStatus() throws SQLException {
        String sql = "SELECT COALESCE(SUM(status <> 'CANCELLED'), 0), COALESCE(SUM(status = 'DRAFT'), 0),"
                + " COALESCE(SUM(status = 'CONFIRMED'), 0) FROM goods_receipts";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return new long[] {resultSet.getLong(1), resultSet.getLong(2), resultSet.getLong(3)};
        }
    }

    public GoodsReceiptDetail findDetail(long id) throws SQLException {
        String headerSql = "SELECT r.id, r.receipt_no, r.supplier_id, s.name AS supplier_name, r.warehouse_id,"
                + " w.code AS warehouse_code, w.name AS warehouse_name, r.document_ref, r.receipt_date, r.status, r.note,"
                + " r.adjusts_receipt_id, a.receipt_no AS adjusts_no, cu.full_name AS created_by_name, r.created_at,"
                + " fu.full_name AS confirmed_by_name, r.confirmed_at"
                + " FROM goods_receipts r JOIN suppliers s ON s.id = r.supplier_id JOIN warehouses w ON w.id = r.warehouse_id"
                + " LEFT JOIN goods_receipts a ON a.id = r.adjusts_receipt_id LEFT JOIN users cu ON cu.id = r.created_by"
                + " LEFT JOIN users fu ON fu.id = r.confirmed_by WHERE r.id = ?";
        String itemSql = "SELECT i.product_id, p.sku, p.name AS product_name, i.unit_id, u.name AS unit_name,"
                + " bu.name AS base_unit_name, i.unit_factor, i.qty, i.qty_base, i.unit_price, i.location_id,"
                + " l.code AS location_code, lo.lot_no, lo.is_default, lo.expiry_date"
                + " FROM goods_receipt_items i JOIN products p ON p.id = i.product_id JOIN units u ON u.id = i.unit_id"
                + " JOIN units bu ON bu.id = p.base_unit_id JOIN lots lo ON lo.id = i.lot_id"
                + " LEFT JOIN warehouse_locations l ON l.id = i.location_id WHERE i.receipt_id = ? ORDER BY i.id";
        try (Connection connection = DbConnection.getConnection()) {
            List<GoodsReceiptDetail.Item> items = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        items.add(new GoodsReceiptDetail.Item(resultSet.getLong("product_id"), resultSet.getString("sku"),
                                resultSet.getString("product_name"), resultSet.getLong("unit_id"),
                                resultSet.getString("unit_name"), resultSet.getString("base_unit_name"),
                                resultSet.getBigDecimal("unit_factor"), resultSet.getBigDecimal("qty"),
                                resultSet.getBigDecimal("qty_base"), resultSet.getBigDecimal("unit_price"),
                                resultSet.getObject("location_id", Long.class), resultSet.getString("location_code"),
                                resultSet.getString("lot_no"), resultSet.getBoolean("is_default"),
                                resultSet.getObject("expiry_date", LocalDate.class)));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return new GoodsReceiptDetail(resultSet.getLong("id"), resultSet.getString("receipt_no"),
                            resultSet.getLong("supplier_id"), resultSet.getString("supplier_name"),
                            resultSet.getLong("warehouse_id"), resultSet.getString("warehouse_code"),
                            resultSet.getString("warehouse_name"), resultSet.getString("document_ref"),
                            resultSet.getObject("receipt_date", LocalDate.class), resultSet.getString("status"),
                            resultSet.getString("note"), resultSet.getObject("adjusts_receipt_id", Long.class),
                            resultSet.getString("adjusts_no"), resultSet.getString("created_by_name"),
                            resultSet.getObject("created_at", LocalDateTime.class),
                            resultSet.getString("confirmed_by_name"),
                            resultSet.getObject("confirmed_at", LocalDateTime.class), items);
                }
            }
        }
    }

    // Sản phẩm đang kinh doanh và các đơn vị quy đổi; SKU chưa khai báo dòng đơn vị cơ sở trong product_units
    // vẫn có đơn vị cơ sở (products.base_unit_id, hệ số 1)
    public List<ReceiptProduct> findProducts() throws SQLException {
        String sql = "SELECT p.id, p.sku, p.name, p.is_lot_tracked, u.id AS unit_id, u.name AS unit_name,"
                + " pu.factor_to_base AS factor, pu.is_base"
                + " FROM products p JOIN product_units pu ON pu.product_id = p.id JOIN units u ON u.id = pu.unit_id"
                + " WHERE p.status = 'ACTIVE'"
                + " UNION ALL SELECT p.id, p.sku, p.name, p.is_lot_tracked, u.id, u.name, 1, true"
                + " FROM products p JOIN units u ON u.id = p.base_unit_id WHERE p.status = 'ACTIVE'"
                + " AND NOT EXISTS (SELECT 1 FROM product_units pu WHERE pu.product_id = p.id AND pu.is_base)"
                + " ORDER BY sku, is_base DESC, factor";
        Map<Long, Object[]> heads = new LinkedHashMap<>();
        Map<Long, List<ReceiptProduct.Unit>> units = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                heads.putIfAbsent(id, new Object[] {resultSet.getString("sku"), resultSet.getString("name"),
                    resultSet.getBoolean("is_lot_tracked")});
                units.computeIfAbsent(id, key -> new ArrayList<>()).add(new ReceiptProduct.Unit(
                        resultSet.getLong("unit_id"), resultSet.getString("unit_name"),
                        resultSet.getBigDecimal("factor"), resultSet.getBoolean("is_base")));
            }
        }
        List<ReceiptProduct> products = new ArrayList<>();
        heads.forEach((id, head) -> products.add(new ReceiptProduct(id, (String) head[0], (String) head[1],
                (Boolean) head[2], units.get(id))));
        return products;
    }

    public List<SelectOption> findActiveSuppliers() throws SQLException {
        List<SelectOption> suppliers = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, code, name FROM suppliers WHERE status = 'ACTIVE' ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                suppliers.add(new SelectOption(resultSet.getLong("id"), resultSet.getString("code"),
                        resultSet.getString("name")));
            }
        }
        return suppliers;
    }

    // Vị trí đang dùng của từng kho (khoá: id kho); name là "mã · tên" để hiện trong ô chọn
    public Map<Long, List<SelectOption>> findActiveLocations() throws SQLException {
        String sql = "SELECT id, warehouse_id, code, name FROM warehouse_locations WHERE is_active"
                + " ORDER BY warehouse_id, pick_sequence, code";
        Map<Long, List<SelectOption>> locations = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String code = resultSet.getString("code");
                String name = resultSet.getString("name");
                locations.computeIfAbsent(resultSet.getLong("warehouse_id"), key -> new ArrayList<>())
                        .add(new SelectOption(resultSet.getLong("id"), code,
                                name == null || name.isBlank() ? code : code + " · " + name));
            }
        }
        return locations;
    }

    // Kho người dùng được gán phụ trách (S1-09); rỗng là không giới hạn kho
    public Set<Long> findUserWarehouseIds(long userId) throws SQLException {
        Set<Long> ids = new HashSet<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT warehouse_id FROM user_warehouses WHERE user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
            }
        }
        return ids;
    }

    // null nếu SKU chưa có lô số này
    public ExistingLot findLot(long productId, String lotNo) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, expiry_date FROM lots WHERE product_id = ? AND lot_no = ?")) {
            statement.setLong(1, productId);
            statement.setString(2, lotNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new ExistingLot(resultSet.getLong("id"),
                        resultSet.getObject("expiry_date", LocalDate.class)) : null;
            }
        }
    }

    // Mã phiếu dự kiến cho phiếu mới (chỉ để hiện); số thật cấp khi lưu bằng nextReceiptNo
    public String previewNextReceiptNo() throws SQLException {
        String sql = "SELECT COALESCE((SELECT last_number FROM document_sequences WHERE doc_type = ? AND period_key = ''),"
                + " (SELECT COALESCE(MAX(CAST(SUBSTRING(receipt_no, 4) AS UNSIGNED)), 0) FROM goods_receipts"
                + " WHERE receipt_no REGEXP '^PN-[0-9]+$'))";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DOC_TYPE);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return format(resultSet.getLong(1) + 1);
            }
        }
    }

    // Cấp mã phiếu liên tục PN-000001, PN-000002... bằng khoá dòng document_sequences trong transaction lưu phiếu.
    // Lần đầu khởi tạo bộ đếm từ mã phiếu lớn nhất đã có.
    public String nextReceiptNo(Connection connection) throws SQLException {
        String initSql = "INSERT IGNORE INTO document_sequences (doc_type, period_key, prefix, last_number)"
                + " SELECT ?, '', ?, COALESCE(MAX(CAST(SUBSTRING(receipt_no, 4) AS UNSIGNED)), 0) FROM goods_receipts"
                + " WHERE receipt_no REGEXP '^PN-[0-9]+$'";
        try (PreparedStatement statement = connection.prepareStatement(initSql)) {
            statement.setString(1, DOC_TYPE);
            statement.setString(2, NO_PREFIX);
            statement.executeUpdate();
        }
        long next;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT last_number FROM document_sequences WHERE doc_type = ? AND period_key = '' FOR UPDATE")) {
            statement.setString(1, DOC_TYPE);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                next = resultSet.getLong(1) + 1;
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE document_sequences SET last_number = ? WHERE doc_type = ? AND period_key = ''")) {
            statement.setLong(1, next);
            statement.setString(2, DOC_TYPE);
            statement.executeUpdate();
        }
        return format(next);
    }

    private static String format(long number) {
        return NO_PREFIX + String.format("%06d", number);
    }

    public long insertHeader(Connection connection, String receiptNo, Header header, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO goods_receipts (receipt_no, supplier_id, warehouse_id, document_ref, receipt_date,"
                + " status, adjusts_receipt_id, note, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, receiptNo);
            statement.setLong(2, header.supplierId());
            statement.setLong(3, header.warehouseId());
            statement.setString(4, header.documentRef());
            statement.setDate(5, Date.valueOf(header.receiptDate()));
            setNullableLong(statement, 6, header.adjustsReceiptId());
            statement.setString(7, header.note());
            statement.setLong(8, actorUserId);
            statement.setLong(9, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // false nếu phiếu không còn là nháp (vừa được ghi sổ ở nơi khác)
    public boolean updateDraftHeader(Connection connection, long id, Header header, long actorUserId)
            throws SQLException {
        String sql = "UPDATE goods_receipts SET supplier_id = ?, warehouse_id = ?, document_ref = ?, receipt_date = ?,"
                + " note = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ? AND status = 'DRAFT'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, header.supplierId());
            statement.setLong(2, header.warehouseId());
            statement.setString(3, header.documentRef());
            statement.setDate(4, Date.valueOf(header.receiptDate()));
            statement.setString(5, header.note());
            statement.setLong(6, actorUserId);
            statement.setLong(7, id);
            return statement.executeUpdate() == 1;
        }
    }

    public void deleteItems(Connection connection, long receiptId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM goods_receipt_items WHERE receipt_id = ?")) {
            statement.setLong(1, receiptId);
            statement.executeUpdate();
        }
    }

    public void insertItem(Connection connection, long receiptId, ItemRow item) throws SQLException {
        String sql = "INSERT INTO goods_receipt_items (receipt_id, product_id, lot_id, unit_id, unit_factor, qty, qty_base,"
                + " unit_price, location_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, receiptId);
            statement.setLong(2, item.productId());
            statement.setLong(3, item.lotId());
            statement.setLong(4, item.unitId());
            statement.setBigDecimal(5, item.unitFactor());
            statement.setBigDecimal(6, item.qty());
            statement.setBigDecimal(7, item.qtyBase());
            statement.setBigDecimal(8, item.unitPrice());
            setNullableLong(statement, 9, item.locationId());
            statement.executeUpdate();
        }
    }

    // lotNo = null: lô mặc định của SKU không quản lý lô. Chưa có thì tạo mới; hai người cùng tạo thì lấy lô vừa tạo.
    public long findOrCreateLot(Connection connection, long productId, String lotNo, LocalDate expiryDate,
                                long supplierId, long actorUserId) throws SQLException {
        Long existing = findLotId(connection, productId, lotNo);
        if (existing != null) {
            return existing;
        }
        String sql = "INSERT INTO lots (product_id, lot_no, expiry_date, supplier_id, is_default, created_at, created_by)"
                + " VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, productId);
            statement.setString(2, lotNo == null ? DEFAULT_LOT_NO : lotNo);
            if (expiryDate == null) {
                statement.setNull(3, Types.DATE);
            } else {
                statement.setDate(3, Date.valueOf(expiryDate));
            }
            if (lotNo == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, supplierId);
            }
            statement.setBoolean(5, lotNo == null);
            statement.setLong(6, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            Long created = findLotId(connection, productId, lotNo);
            if (created == null) {
                throw e;
            }
            return created;
        }
    }

    private static Long findLotId(Connection connection, long productId, String lotNo) throws SQLException {
        String sql = lotNo == null ? "SELECT id FROM lots WHERE product_id = ? AND is_default"
                : "SELECT id FROM lots WHERE product_id = ? AND lot_no = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            if (lotNo != null) {
                statement.setString(2, lotNo);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    // Khoá phiếu để ghi sổ; null nếu không có phiếu
    public String lockStatus(Connection connection, long receiptId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT status FROM goods_receipts WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, receiptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    public List<PostingItem> findPostingItems(Connection connection, long receiptId) throws SQLException {
        List<PostingItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT i.product_id, p.sku, i.lot_id, l.lot_no, i.qty_base, i.location_id FROM goods_receipt_items i"
                        + " JOIN products p ON p.id = i.product_id JOIN lots l ON l.id = i.lot_id"
                        + " WHERE i.receipt_id = ? ORDER BY i.id")) {
            statement.setLong(1, receiptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new PostingItem(resultSet.getLong("product_id"), resultSet.getString("sku"),
                            resultSet.getLong("lot_id"), resultSet.getString("lot_no"), resultSet.getBigDecimal("qty_base"),
                            resultSet.getObject("location_id", Long.class)));
                }
            }
        }
        return items;
    }

    // Khoá dòng sổ tồn (kho, SKU), tạo dòng 0 nếu SKU chưa từng có ở kho này. Trả về {tồn thực tế, đang giữ chỗ}.
    public BigDecimal[] lockBalance(Connection connection, long warehouseId, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO stock_balances (warehouse_id, product_id, updated_at) VALUES (?, ?, UTC_TIMESTAMP())"
                        + " ON DUPLICATE KEY UPDATE warehouse_id = warehouse_id")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT qty_on_hand_base, qty_reserved_base"
                + " FROM stock_balances WHERE warehouse_id = ? AND product_id = ? FOR UPDATE")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new BigDecimal[] {resultSet.getBigDecimal(1), resultSet.getBigDecimal(2)};
            }
        }
    }

    // Đặt tồn thực tế mới; vị trí lưu chính chỉ điền khi SKU chưa có vị trí ở kho này
    public void updateBalance(Connection connection, long warehouseId, long productId, BigDecimal onHand,
                              Long locationId) throws SQLException {
        String sql = "UPDATE stock_balances SET qty_on_hand_base = ?, location_id = COALESCE(location_id, ?),"
                + " version = version + 1, updated_at = UTC_TIMESTAMP() WHERE warehouse_id = ? AND product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, onHand);
            setNullableLong(statement, 2, locationId);
            statement.setLong(3, warehouseId);
            statement.setLong(4, productId);
            statement.executeUpdate();
        }
    }

    // Khoá dòng tồn theo lô (kho, lô), tạo dòng 0 nếu chưa có; trả về tồn của lô
    public BigDecimal lockLotBalance(Connection connection, long warehouseId, long lotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO stock_lot_balances (warehouse_id, lot_id, updated_at) VALUES (?, ?, UTC_TIMESTAMP())"
                        + " ON DUPLICATE KEY UPDATE warehouse_id = warehouse_id")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, lotId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT qty_on_hand_base FROM stock_lot_balances WHERE warehouse_id = ? AND lot_id = ? FOR UPDATE")) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, lotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    public void updateLotBalance(Connection connection, long warehouseId, long lotId, BigDecimal onHand)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE stock_lot_balances SET qty_on_hand_base = ?,"
                + " updated_at = UTC_TIMESTAMP() WHERE warehouse_id = ? AND lot_id = ?")) {
            statement.setBigDecimal(1, onHand);
            statement.setLong(2, warehouseId);
            statement.setLong(3, lotId);
            statement.executeUpdate();
        }
    }

    // Thẻ kho: một dòng cho mỗi dòng hàng của phiếu (S6-02 truy được phiếu nào làm đổi tồn)
    public void insertMovement(Connection connection, long warehouseId, PostingItem item, String movementType,
                               BigDecimal balanceAfter, long receiptId, String note, long actorUserId) throws SQLException {
        String sql = "INSERT INTO stock_movements (warehouse_id, product_id, lot_id, movement_type, qty_change_base,"
                + " balance_after_base, ref_type, ref_id, occurred_at, note, created_at, created_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, item.productId());
            statement.setLong(3, item.lotId());
            statement.setString(4, movementType);
            statement.setBigDecimal(5, item.qtyBase());
            statement.setBigDecimal(6, balanceAfter);
            statement.setString(7, REF_TYPE);
            statement.setLong(8, receiptId);
            statement.setString(9, note);
            statement.setLong(10, actorUserId);
            statement.executeUpdate();
        }
    }

    // false nếu phiếu không còn là nháp
    public boolean confirm(Connection connection, long receiptId, long actorUserId) throws SQLException {
        String sql = "UPDATE goods_receipts SET status = 'CONFIRMED', confirmed_by = ?, confirmed_at = UTC_TIMESTAMP(),"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ? AND status = 'DRAFT'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, actorUserId);
            statement.setLong(3, receiptId);
            return statement.executeUpdate() == 1;
        }
    }

    private static int bindFilter(PreparedStatement statement, GoodsReceiptFilter filter) throws SQLException {
        String keyword = filter.getKeyword();
        String like = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        statement.setString(1, keyword);
        for (int i = 2; i <= 5; i++) {
            statement.setString(i, like);
        }
        statement.setString(6, filter.getStatus());
        statement.setString(7, filter.getStatus());
        setNullableLong(statement, 8, filter.getWarehouseId());
        setNullableLong(statement, 9, filter.getWarehouseId());
        Date date = filter.getReceiptDate() == null ? null : Date.valueOf(filter.getReceiptDate());
        statement.setDate(10, date);
        statement.setDate(11, date);
        return 12;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
