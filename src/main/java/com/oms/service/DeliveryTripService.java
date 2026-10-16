package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.DeliveryTripDao;
import com.oms.dao.GoodsReceiptDao;
import com.oms.dao.PermissionDao;
import com.oms.model.DeliveryTrip;
import com.oms.model.SelectOption;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Phân tuyến giao hàng (S6-06): gom nhiều phiếu xuất cùng kho (và cùng khu vực) vào một chuyến, gán tài xế và phương
// tiện, in danh sách điểm giao. Chuyến "Đã lập" sửa được; xuất phát thì các phiếu sang "Đang giao" và chuyến "Đang giao"
// (theo dõi ở S6-07); mọi điểm đã có kết quả thì hoàn thành chuyến. Người dùng chỉ thao tác kho mình phụ trách.
public class DeliveryTripService {

    public static final int LIST_LIMIT = 100;
    public static final int TEXT_MAX = 150;
    public static final int NOTE_MAX = 1000;
    private static final int NUMBER_ATTEMPTS = 3;
    private static final String NUMBER_PREFIX = "CG";
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String AUDIT_ENTITY = "DELIVERY_TRIP";
    private static final String NOT_FOUND = "NOT_FOUND";

    private final DeliveryTripDao tripDao = new DeliveryTripDao();
    private final GoodsReceiptDao receiptDao = new GoodsReceiptDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // error != null: lỗi nghiệp vụ (hoặc "NOT_FOUND"); ngược lại id / mã chuyến và câu báo
    public static final class Outcome {
        private final String error;
        private final long id;
        private final String message;

        private Outcome(String error, long id, String message) {
            this.error = error;
            this.id = id;
            this.message = message;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0, null);
        }

        static Outcome success(long id, String message) {
            return new Outcome(null, id, message);
        }

        public boolean isNotFound() {
            return NOT_FOUND.equals(error);
        }

        public String getError() {
            return error;
        }

        public long getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    public List<DeliveryTrip> findTrips(long userId, String status) throws SQLException {
        return tripDao.findTrips(null, status, LIST_LIMIT).stream()
                .filter(trip -> canUseWarehouse(userId, trip.getWarehouseId())).toList();
    }

    public DeliveryTrip find(long id) throws SQLException {
        return tripDao.findDetail(id);
    }

    public List<DeliveryTrip.Stop> candidates(long warehouseId, Long regionId) throws SQLException {
        return tripDao.findCandidates(warehouseId, regionId);
    }

    public List<SelectOption> regions() throws SQLException {
        return tripDao.findRegions();
    }

    // Chuyến giao chứa địa chỉ và số điện thoại của nhiều đại lý nên chỉ người xem được mọi đơn (DELIVERY_VIEW phạm vi ALL:
    // nhân viên kho, quản lý kho, kế toán, quản lý kinh doanh, Admin); nhân viên kinh doanh (ASSIGNED) và đại lý (OWN) không
    public boolean canView(long userId) throws SQLException {
        DeliveryScope scope = DeliveryScope.of(permissionDao, userId);
        return scope.allowed() && scope.salesRepId() == null;
    }

    // Người dùng chưa gán kho nào thì thao tác mọi kho (như phiếu soạn)
    public boolean canUseWarehouse(long userId, long warehouseId) {
        try {
            Set<Long> assigned = receiptDao.findUserWarehouseIds(userId);
            return assigned.isEmpty() || assigned.contains(warehouseId);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    // "yyyy-MM-ddTHH:mm" theo giờ Việt Nam -> UTC; null nếu trống; IllegalArgumentException nếu sai định dạng
    static LocalDateTime parseLocalDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim()).atZone(DISPLAY_ZONE).withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Thời gian không hợp lệ.");
        }
    }

    public Outcome create(long actorUserId, long warehouseId, Long regionId, String driverName, String driverPhone,
                          String vehiclePlate, String plannedDeparture, String expectedReturn, String note,
                          List<Long> noteIds, String ipAddress) throws SQLException {
        if (!canUseWarehouse(actorUserId, warehouseId)) {
            return Outcome.failure("Bạn chỉ lập chuyến giao cho kho mình phụ trách.");
        }
        String driver = clean(driverName);
        String plate = clean(vehiclePlate);
        if (driver == null) {
            return Outcome.failure("Vui lòng nhập tên tài xế.");
        }
        if (plate == null) {
            return Outcome.failure("Vui lòng nhập biển số phương tiện.");
        }
        if (driver.length() > TEXT_MAX || plate.length() > 20 || (clean(driverPhone) != null && clean(driverPhone).length() > 20)) {
            return Outcome.failure("Tên tài xế tối đa " + TEXT_MAX + " ký tự, số điện thoại và biển số tối đa 20 ký tự.");
        }
        String cleanNote = clean(note);
        if (cleanNote != null && cleanNote.length() > NOTE_MAX) {
            return Outcome.failure("Ghi chú tối đa " + NOTE_MAX + " ký tự.");
        }
        LocalDateTime departure;
        LocalDateTime returnAt;
        try {
            departure = parseLocalDateTime(plannedDeparture);
            returnAt = parseLocalDateTime(expectedReturn);
        } catch (IllegalArgumentException e) {
            return Outcome.failure(e.getMessage());
        }
        if (departure != null && returnAt != null && !returnAt.isAfter(departure)) {
            return Outcome.failure("Giờ dự kiến về phải sau giờ xuất phát.");
        }
        if (noteIds.isEmpty()) {
            return Outcome.failure("Chọn ít nhất một phiếu xuất để gom vào chuyến.");
        }
        String prefix = NUMBER_PREFIX + DateTimeUtil.today().format(NUMBER_DATE) + "-";
        for (int attempt = 1; ; attempt++) {
            String number = prefix + String.format("%04d", tripDao.findMaxNumber(prefix) + 1);
            try (Connection connection = DbConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    long id = tripDao.insert(connection, number, warehouseId, regionId, driver, clean(driverPhone),
                            plate, departure, returnAt, cleanNote, actorUserId);
                    int attached = tripDao.attach(connection, id, warehouseId, noteIds);
                    if (attached != noteIds.stream().distinct().count()) {
                        connection.rollback();
                        return Outcome.failure("Có phiếu xuất không còn ở trạng thái Chờ giao, đã vào chuyến khác hoặc"
                                + " thuộc kho khác. Tải lại danh sách rồi chọn lại.");
                    }
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("tripNo", number);
                    values.put("warehouseId", warehouseId);
                    values.put("driverName", driver);
                    values.put("vehiclePlate", plate);
                    values.put("stops", attached);
                    auditLogDao.insert(connection, actorUserId, "TRIP_CREATE", AUDIT_ENTITY, id, null,
                            JsonUtil.object(values), null, ipAddress);
                    connection.commit();
                    return Outcome.success(id, "Đã lập chuyến " + number + " gồm " + attached + " điểm giao.");
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                if (attempt == NUMBER_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    public Outcome addNotes(long actorUserId, long tripId, List<Long> noteIds, String ipAddress) throws SQLException {
        if (noteIds.isEmpty()) {
            return Outcome.failure("Chọn ít nhất một phiếu xuất để thêm vào chuyến.");
        }
        return inTrip(actorUserId, tripId, true, (connection, trip) -> {
            int attached = tripDao.attach(connection, tripId, trip.getWarehouseId(), noteIds);
            if (attached != noteIds.stream().distinct().count()) {
                return Outcome.failure("Có phiếu xuất không còn ở trạng thái Chờ giao, đã vào chuyến khác hoặc thuộc"
                        + " kho khác.");
            }
            audit(connection, actorUserId, "TRIP_UPDATE", tripId, trip.getTripNo(), "ADD_STOPS", attached, ipAddress);
            return Outcome.success(tripId, "Đã thêm " + attached + " điểm giao vào chuyến " + trip.getTripNo() + ".");
        });
    }

    public Outcome removeNote(long actorUserId, long tripId, long noteId, String ipAddress) throws SQLException {
        return inTrip(actorUserId, tripId, true, (connection, trip) -> {
            if (tripDao.detach(connection, tripId, noteId) != 1) {
                return Outcome.failure("Phiếu xuất này không còn trong chuyến.");
            }
            audit(connection, actorUserId, "TRIP_UPDATE", tripId, trip.getTripNo(), "REMOVE_STOP", 1, ipAddress);
            return Outcome.success(tripId, "Đã bỏ điểm giao khỏi chuyến " + trip.getTripNo() + ".");
        });
    }

    public Outcome depart(long actorUserId, long tripId, String ipAddress) throws SQLException {
        return inTrip(actorUserId, tripId, true, (connection, trip) -> {
            int stops = tripDao.countNotes(connection, tripId);
            if (stops == 0) {
                return Outcome.failure("Chuyến chưa có điểm giao nào để xuất phát.");
            }
            tripDao.depart(connection, tripId, actorUserId);
            audit(connection, actorUserId, "TRIP_DEPART", tripId, trip.getTripNo(), "DEPART", stops, ipAddress);
            return Outcome.success(tripId, "Chuyến " + trip.getTripNo() + " đã xuất phát, " + stops + " điểm giao.");
        });
    }

    public Outcome complete(long actorUserId, long tripId, String ipAddress) throws SQLException {
        return inTrip(actorUserId, tripId, false, (connection, trip) -> {
            int pending = tripDao.countPendingNotes(connection, tripId);
            if (pending > 0) {
                return Outcome.failure("Chuyến " + trip.getTripNo() + " còn " + pending + " điểm chưa có kết quả giao."
                        + " Xác nhận giao (thành công hoặc không thành công) cho các điểm đó trước.");
            }
            tripDao.complete(connection, tripId, actorUserId);
            audit(connection, actorUserId, "TRIP_COMPLETE", tripId, trip.getTripNo(), "COMPLETE", 0, ipAddress);
            return Outcome.success(tripId, "Đã hoàn thành chuyến " + trip.getTripNo() + ".");
        });
    }

    // Huỷ chuyến chưa xuất phát: các phiếu quay về danh sách chờ gom
    public Outcome cancel(long actorUserId, long tripId, String ipAddress) throws SQLException {
        return inTrip(actorUserId, tripId, true, (connection, trip) -> {
            int released = 0;
            DeliveryTrip detail = tripDao.findDetail(tripId);
            if (detail != null) {
                for (DeliveryTrip.Stop stop : detail.getStops()) {
                    released += tripDao.detach(connection, tripId, stop.getNoteId());
                }
            }
            tripDao.delete(connection, tripId);
            audit(connection, actorUserId, "TRIP_CANCEL", tripId, trip.getTripNo(), "CANCEL", released, ipAddress);
            return Outcome.success(0, "Đã huỷ chuyến " + trip.getTripNo() + ", " + released
                    + " phiếu xuất quay về danh sách chờ gom.");
        });
    }

    @FunctionalInterface
    private interface TripAction {
        Outcome run(Connection connection, DeliveryTripDao.Locked trip) throws SQLException;
    }

    // plannedOnly: chỉ chuyến "Đã lập"; ngược lại chỉ chuyến "Đang giao"
    private Outcome inTrip(long actorUserId, long tripId, boolean plannedOnly, TripAction action)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                DeliveryTripDao.Locked trip = tripDao.lock(connection, tripId);
                if (trip == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!canUseWarehouse(actorUserId, trip.getWarehouseId())) {
                    connection.rollback();
                    return Outcome.failure("Bạn chỉ thao tác chuyến giao của kho mình phụ trách.");
                }
                String expected = plannedOnly ? DeliveryTrip.PLANNED : DeliveryTrip.RUNNING;
                if (!expected.equals(trip.getStatus())) {
                    connection.rollback();
                    return Outcome.failure(plannedOnly
                            ? "Chuyến " + trip.getTripNo() + " đã xuất phát hoặc đã hoàn thành nên không sửa được."
                            : "Chuyến " + trip.getTripNo() + " chưa xuất phát hoặc đã hoàn thành.");
                }
                Outcome outcome = action.run(connection, trip);
                if (outcome.getError() != null) {
                    connection.rollback();
                } else {
                    connection.commit();
                }
                return outcome;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void audit(Connection connection, long actorUserId, String action, long tripId, String tripNo,
                       String detail, int count, String ipAddress) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tripNo", tripNo);
        values.put("action", detail);
        values.put("stops", count);
        auditLogDao.insert(connection, actorUserId, action, AUDIT_ENTITY, tripId, null, JsonUtil.object(values),
                null, ipAddress);
    }

    private static String clean(String text) {
        return text == null || text.trim().isEmpty() ? null : text.trim();
    }
}
