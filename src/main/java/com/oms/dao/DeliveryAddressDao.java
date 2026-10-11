package com.oms.dao;

import com.oms.model.DeliveryAddress;
import com.oms.model.DeliveryAddressEntry;
import com.oms.model.DeliveryAddressForm;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Điểm giao hàng của đại lý (customer_delivery_addresses). Màn khai báo điểm giao thuộc S3-04;
// màn tạo đơn (S3-09) chỉ đọc danh sách điểm giao đang hoạt động.
// Mỗi đại lý chỉ một điểm mặc định đang dùng (ux_delivery_addr_default): bỏ mặc định cũ trước khi đặt điểm mới.
public class DeliveryAddressDao {

    private static final String ENTRY_COLUMNS = "a.id, a.customer_id, a.label, a.address, a.receiver_name,"
            + " a.receiver_phone, a.route_note, a.is_default, a.is_active,"
            + " EXISTS (SELECT 1 FROM sales_orders o WHERE o.delivery_address_id = a.id) AS used";

    // Mọi điểm giao của đại lý cho màn S3-04: đang dùng trước (mặc định đầu tiên), đã ngừng dùng ở cuối
    public List<DeliveryAddressEntry> findEntries(long customerId) throws SQLException {
        String sql = "SELECT " + ENTRY_COLUMNS + " FROM customer_delivery_addresses a WHERE a.customer_id = ?"
                + " ORDER BY a.is_active DESC, a.is_default DESC, a.id";
        List<DeliveryAddressEntry> entries = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapEntry(resultSet));
                }
            }
        }
        return entries;
    }

    public DeliveryAddressEntry findEntry(long id) throws SQLException {
        String sql = "SELECT " + ENTRY_COLUMNS + " FROM customer_delivery_addresses a WHERE a.id = ?";
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapEntry(resultSet) : null;
            }
        }
    }

    public boolean hasActiveDefault(Connection connection, long customerId) throws SQLException {
        String sql = "SELECT 1 FROM customer_delivery_addresses WHERE customer_id = ? AND is_default AND is_active";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Điểm đang dùng đầu tiên còn lại để nhận mặc định khi điểm mặc định bị xoá / ngừng dùng; null nếu không còn
    public Long findFirstActiveId(Connection connection, long customerId, long excludeId) throws SQLException {
        String sql = "SELECT id FROM customer_delivery_addresses WHERE customer_id = ? AND is_active AND id <> ?"
                + " ORDER BY id LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setLong(2, excludeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    public long insert(Connection connection, long customerId, DeliveryAddressForm form, boolean isDefault,
                       long actorUserId) throws SQLException {
        String sql = "INSERT INTO customer_delivery_addresses (customer_id, label, address, receiver_name,"
                + " receiver_phone, route_note, is_default, is_active, created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, true, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, customerId);
            statement.setString(2, form.getLabel());
            statement.setString(3, form.getAddress());
            statement.setString(4, form.getReceiverName());
            statement.setString(5, form.getReceiverPhone());
            statement.setString(6, form.getRouteNote());
            statement.setBoolean(7, isDefault);
            statement.setLong(8, actorUserId);
            statement.setLong(9, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, long id, DeliveryAddressForm form, long actorUserId)
            throws SQLException {
        String sql = "UPDATE customer_delivery_addresses SET label = ?, address = ?, receiver_name = ?,"
                + " receiver_phone = ?, route_note = ?, updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, form.getLabel());
            statement.setString(2, form.getAddress());
            statement.setString(3, form.getReceiverName());
            statement.setString(4, form.getReceiverPhone());
            statement.setString(5, form.getRouteNote());
            statement.setLong(6, actorUserId);
            statement.setLong(7, id);
            statement.executeUpdate();
        }
    }

    public void clearDefault(Connection connection, long customerId, long actorUserId) throws SQLException {
        String sql = "UPDATE customer_delivery_addresses SET is_default = false, updated_at = UTC_TIMESTAMP(),"
                + " updated_by = ? WHERE customer_id = ? AND is_default";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, customerId);
            statement.executeUpdate();
        }
    }

    public void setDefault(Connection connection, long id, long actorUserId) throws SQLException {
        String sql = "UPDATE customer_delivery_addresses SET is_default = true, updated_at = UTC_TIMESTAMP(),"
                + " updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    // Ngừng dùng thì bỏ luôn cờ mặc định
    public void setActive(Connection connection, long id, boolean active, long actorUserId) throws SQLException {
        String sql = "UPDATE customer_delivery_addresses SET is_active = ?, is_default = is_default AND ?,"
                + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setBoolean(2, active);
            statement.setLong(3, actorUserId);
            statement.setLong(4, id);
            statement.executeUpdate();
        }
    }

    public void delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM customer_delivery_addresses WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private static DeliveryAddressEntry mapEntry(ResultSet resultSet) throws SQLException {
        return new DeliveryAddressEntry(resultSet.getLong("id"), resultSet.getLong("customer_id"),
                resultSet.getString("label"), resultSet.getString("address"), resultSet.getString("receiver_name"),
                resultSet.getString("receiver_phone"), resultSet.getString("route_note"),
                resultSet.getBoolean("is_default"), resultSet.getBoolean("is_active"), resultSet.getBoolean("used"));
    }

    // Điểm giao đang hoạt động của đại lý, điểm mặc định đứng đầu (S3-09 chọn điểm giao khi tạo đơn)
    public List<DeliveryAddress> findDeliveryAddresses(long customerId) throws SQLException {
        String sql = "SELECT id, label, address, receiver_name, is_default FROM customer_delivery_addresses"
                + " WHERE customer_id = ? AND is_active ORDER BY is_default DESC, id";
        List<DeliveryAddress> addresses = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    addresses.add(new DeliveryAddress(resultSet.getLong("id"), resultSet.getString("label"),
                            resultSet.getString("address"), resultSet.getString("receiver_name"),
                            resultSet.getBoolean("is_default")));
                }
            }
        }
        return addresses;
    }
}
