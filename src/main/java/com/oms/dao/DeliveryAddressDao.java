package com.oms.dao;

import com.oms.model.DeliveryAddress;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Điểm giao hàng của đại lý (customer_delivery_addresses). Màn khai báo điểm giao thuộc S3-04;
// màn tạo đơn (S3-09) chỉ đọc danh sách điểm giao đang hoạt động.
public class DeliveryAddressDao {

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
