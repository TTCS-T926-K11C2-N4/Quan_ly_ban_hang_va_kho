package com.oms.dao;

import com.oms.model.DeliveryTrip;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// Chuyến giao hàng (delivery_trips) và việc gom phiếu xuất vào chuyến qua delivery_notes.trip_id (S6-06, S6-07).
// Các hàm nhận Connection chạy trong transaction của DeliveryTripService.
public class DeliveryTripDao {

    private static final String HEADER = "SELECT t.id, t.trip_no, t.warehouse_id, w.name AS warehouse_name,"
            + " r.name AS region_name, t.driver_name, t.driver_phone, t.vehicle_plate, t.status, t.planned_departure,"
            + " t.expected_return_at, t.departed_at, t.completed_at, t.note,"
            + " (SELECT COUNT(*) FROM delivery_notes d WHERE d.trip_id = t.id) AS stop_count,"
            + " (SELECT COUNT(*) FROM delivery_notes d WHERE d.trip_id = t.id AND d.status IN ('DELIVERED', 'FAILED'))"
            + " AS done_count FROM delivery_trips t JOIN warehouses w ON w.id = t.warehouse_id"
            + " LEFT JOIN regions r ON r.id = t.region_id";

    private static final String STOP_SELECT = "SELECT d.id, d.delivery_no, d.status, o.id AS order_id, o.order_no,"
            + " c.name AS customer_name, a.label, a.address, a.receiver_name, a.receiver_phone, a.route_note,"
            + " (SELECT ROUND(SUM(i.qty_issued_base * oi.line_total / oi.qty_base), 0) FROM delivery_note_items i"
            + " JOIN sales_order_items oi ON oi.id = i.order_item_id WHERE i.delivery_note_id = d.id) AS note_value"
            + " FROM delivery_notes d JOIN sales_orders o ON o.id = d.order_id JOIN customers c ON c.id = o.customer_id"
            + " JOIN customer_delivery_addresses a ON a.id = o.delivery_address_id";

    public List<SelectOption> findRegions() throws SQLException {
        List<SelectOption> regions = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, code, name FROM regions WHERE is_active ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                regions.add(new SelectOption(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3)));
            }
        }
        return regions;
    }

    // Mới nhất trước; status null: mọi trạng thái; warehouseId null: mọi kho
    public List<DeliveryTrip> findTrips(Long warehouseId, String status, int limit) throws SQLException {
        String sql = HEADER + " WHERE (? IS NULL OR t.warehouse_id = ?) AND (? IS NULL OR t.status = ?)"
                + " ORDER BY t.created_at DESC, t.id DESC LIMIT ?";
        List<DeliveryTrip> trips = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableLong(statement, 1, warehouseId);
            setNullableLong(statement, 2, warehouseId);
            statement.setString(3, status);
            statement.setString(4, status);
            statement.setInt(5, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    trips.add(readTrip(resultSet, List.of()));
                }
            }
        }
        return trips;
    }

    // null nếu không có chuyến
    public DeliveryTrip findDetail(long id) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            List<DeliveryTrip.Stop> stops = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    STOP_SELECT + " WHERE d.trip_id = ? ORDER BY c.name, a.id, d.id")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        stops.add(readStop(resultSet));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(HEADER + " WHERE t.id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? readTrip(resultSet, stops) : null;
                }
            }
        }
    }

    // Phiếu xuất đã xuất kho, chưa vào chuyến nào, cùng kho (và cùng khu vực nếu chọn) để gom vào chuyến
    public List<DeliveryTrip.Stop> findCandidates(long warehouseId, Long regionId) throws SQLException {
        String sql = STOP_SELECT + " WHERE d.status = 'ISSUED' AND d.trip_id IS NULL AND d.warehouse_id = ?"
                + " AND (? IS NULL OR c.region_id = ?) ORDER BY c.name, a.id, d.id";
        List<DeliveryTrip.Stop> stops = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            setNullableLong(statement, 2, regionId);
            setNullableLong(statement, 3, regionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    stops.add(readStop(resultSet));
                }
            }
        }
        return stops;
    }

    public int findMaxNumber(String prefix) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(trip_no, ?) AS UNSIGNED)), 0) FROM delivery_trips"
                + " WHERE trip_no LIKE ?";
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

    // Ném SQLIntegrityConstraintViolationException nếu mã chuyến vừa bị chuyến khác dùng
    public long insert(Connection connection, String tripNo, long warehouseId, Long regionId, String driverName,
                       String driverPhone, String vehiclePlate, LocalDateTime plannedDeparture,
                       LocalDateTime expectedReturnAt, String note, long actorUserId) throws SQLException {
        String sql = "INSERT INTO delivery_trips (trip_no, warehouse_id, region_id, driver_name, driver_phone,"
                + " vehicle_plate, status, planned_departure, expected_return_at, note, created_at, created_by,"
                + " updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, 'PLANNED', ?, ?, ?, UTC_TIMESTAMP(), ?,"
                + " UTC_TIMESTAMP(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, tripNo);
            statement.setLong(2, warehouseId);
            setNullableLong(statement, 3, regionId);
            statement.setString(4, driverName);
            statement.setString(5, driverPhone);
            statement.setString(6, vehiclePlate);
            statement.setObject(7, plannedDeparture, Types.TIMESTAMP);
            statement.setObject(8, expectedReturnAt, Types.TIMESTAMP);
            statement.setString(9, note);
            statement.setLong(10, actorUserId);
            statement.setLong(11, actorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // Chuyến đang khoá; null nếu không có
    public Locked lock(Connection connection, long tripId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT trip_no, warehouse_id, region_id, status FROM delivery_trips WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, tripId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new Locked(resultSet.getString(1), resultSet.getLong(2),
                        resultSet.getObject(3, Long.class), resultSet.getString(4)) : null;
            }
        }
    }

    // Gom phiếu vào chuyến: chỉ phiếu Chờ giao, chưa vào chuyến nào và cùng kho; trả về số phiếu được gom
    public int attach(Connection connection, long tripId, long warehouseId, Collection<Long> noteIds)
            throws SQLException {
        if (noteIds.isEmpty()) {
            return 0;
        }
        String sql = "UPDATE delivery_notes SET trip_id = ? WHERE trip_id IS NULL AND status = 'ISSUED'"
                + " AND warehouse_id = ? AND id IN (" + String.join(", ", Collections.nCopies(noteIds.size(), "?")) + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tripId);
            statement.setLong(2, warehouseId);
            int index = 3;
            for (Long id : noteIds) {
                statement.setLong(index++, id);
            }
            return statement.executeUpdate();
        }
    }

    // Bỏ phiếu khỏi chuyến (chuyến chưa xuất phát); trả về số phiếu được bỏ
    public int detach(Connection connection, long tripId, long noteId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE delivery_notes SET trip_id = NULL WHERE id = ? AND trip_id = ? AND status = 'ISSUED'")) {
            statement.setLong(1, noteId);
            statement.setLong(2, tripId);
            return statement.executeUpdate();
        }
    }

    public int countNotes(Connection connection, long tripId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM delivery_notes WHERE trip_id = ?")) {
            statement.setLong(1, tripId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    // Số phiếu của chuyến còn chờ xác nhận giao
    public int countPendingNotes(Connection connection, long tripId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM delivery_notes WHERE trip_id = ? AND status IN ('ISSUED', 'IN_TRANSIT')")) {
            statement.setLong(1, tripId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    // Xuất phát: chuyến sang Đang giao, các phiếu của chuyến sang Đang giao (IN_TRANSIT)
    public void depart(Connection connection, long tripId, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE delivery_trips SET status = 'RUNNING', departed_at = UTC_TIMESTAMP(),"
                        + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, tripId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE delivery_notes SET status = 'IN_TRANSIT', updated_at = UTC_TIMESTAMP(), updated_by = ?"
                        + " WHERE trip_id = ? AND status = 'ISSUED'")) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, tripId);
            statement.executeUpdate();
        }
    }

    public void complete(Connection connection, long tripId, long actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE delivery_trips SET status = 'COMPLETED', completed_at = UTC_TIMESTAMP(),"
                        + " updated_at = UTC_TIMESTAMP(), updated_by = ? WHERE id = ?")) {
            statement.setLong(1, actorUserId);
            statement.setLong(2, tripId);
            statement.executeUpdate();
        }
    }

    public void delete(Connection connection, long tripId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM delivery_trips WHERE id = ?")) {
            statement.setLong(1, tripId);
            statement.executeUpdate();
        }
    }

    private static DeliveryTrip readTrip(ResultSet resultSet, List<DeliveryTrip.Stop> stops) throws SQLException {
        return new DeliveryTrip(resultSet.getLong("id"), resultSet.getString("trip_no"),
                resultSet.getLong("warehouse_id"), resultSet.getString("warehouse_name"),
                resultSet.getString("region_name"), resultSet.getString("driver_name"),
                resultSet.getString("driver_phone"), resultSet.getString("vehicle_plate"),
                resultSet.getString("status"), resultSet.getObject("planned_departure", LocalDateTime.class),
                resultSet.getObject("expected_return_at", LocalDateTime.class),
                resultSet.getObject("departed_at", LocalDateTime.class),
                resultSet.getObject("completed_at", LocalDateTime.class), resultSet.getString("note"),
                resultSet.getInt("stop_count"), resultSet.getInt("done_count"), stops);
    }

    private static DeliveryTrip.Stop readStop(ResultSet resultSet) throws SQLException {
        BigDecimal value = resultSet.getBigDecimal("note_value");
        return new DeliveryTrip.Stop(resultSet.getLong("id"), resultSet.getString("delivery_no"),
                resultSet.getString("status"), resultSet.getLong("order_id"), resultSet.getString("order_no"),
                resultSet.getString("customer_name"), resultSet.getString("label"), resultSet.getString("address"),
                resultSet.getString("receiver_name"), resultSet.getString("receiver_phone"),
                resultSet.getString("route_note"), value == null ? BigDecimal.ZERO : value);
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    // Chuyến đang khoá trong transaction
    public static class Locked {
        private final String tripNo;
        private final long warehouseId;
        private final Long regionId;
        private final String status;

        Locked(String tripNo, long warehouseId, Long regionId, String status) {
            this.tripNo = tripNo;
            this.warehouseId = warehouseId;
            this.regionId = regionId;
            this.status = status;
        }

        public String getTripNo() {
            return tripNo;
        }

        public long getWarehouseId() {
            return warehouseId;
        }

        public Long getRegionId() {
            return regionId;
        }

        public String getStatus() {
            return status;
        }
    }
}
