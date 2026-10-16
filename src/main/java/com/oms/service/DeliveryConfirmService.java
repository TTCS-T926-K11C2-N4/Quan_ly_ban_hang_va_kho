package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.DeliveryDao;
import com.oms.dao.DeliveryNoteDao;
import com.oms.dao.FileObjectDao;
import com.oms.dao.GoodsReceiptDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.PickListDao;
import com.oms.dao.SalesOrderDao;
import com.oms.dao.StockReservationDao;
import com.oms.dao.SystemSettingDao;
import com.oms.model.DeliveryDay;
import com.oms.model.DeliveryNoteStatus;
import com.oms.model.DeliveryPoint;
import com.oms.model.DeliveryStop;
import com.oms.model.OrderStatus;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.FileStorage;
import com.oms.util.JsonUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

// Xác nhận giao hàng (S6-08). Phiếu xuất của một ngày gom theo điểm giao (điểm giao của đại lý trên đơn).
//  * Giao thành công: ghi người nhận, thời điểm giao, ít nhất một ảnh biên bản có chữ ký (JPG/PNG); phiếu chuyển
//    "Đã giao" (đủ điều kiện lập hoá đơn ở Sprint 7, theo số thực giao); đơn hết hàng phải giao chuyển "Đã giao".
//  * Giao không thành công: chọn lý do; hàng quay về kho (thẻ kho RETURN_IN, cộng lại tồn theo lô) và giữ chỗ lại cho
//    đơn; đơn chưa giao được phiếu nào quay về "Đã duyệt" để soạn giao lại.
public class DeliveryConfirmService {

    public static final int MAX_PROOFS = 5;
    public static final long MAX_PROOF_BYTES = 5L * 1024 * 1024;
    public static final String OTHER_REASON = "Lý do khác";
    public static final List<String> FAILURE_REASONS = List.of("Khách hàng vắng mặt, không liên lạc được",
            "Khách hàng từ chối nhận hàng", "Sai địa chỉ hoặc không tìm thấy điểm giao",
            "Hàng hư hỏng, sai quy cách khi giao", OTHER_REASON);

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter MONTH_FOLDER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int NAME_MAX_LENGTH = 150;
    private static final int REMARK_MAX_LENGTH = 1000;
    // Cho phép đồng hồ máy người giao lệch vài phút
    private static final long FUTURE_TOLERANCE_MINUTES = 5;
    private static final String ENTITY = "DELIVERY_NOTE";

    private final DeliveryDao deliveryDao = new DeliveryDao();
    private final DeliveryNoteDao noteDao = new DeliveryNoteDao();
    private final PickListDao pickListDao = new PickListDao();
    private final GoodsReceiptDao receiptDao = new GoodsReceiptDao();
    private final StockReservationDao reservationDao = new StockReservationDao();
    private final SalesOrderDao salesOrderDao = new SalesOrderDao();
    private final FileObjectDao fileObjectDao = new FileObjectDao();
    private final SystemSettingDao settingDao = new SystemSettingDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Lỗi nghiệp vụ hiện cho người dùng (không ghi nhận được)
    public static class DeliveryException extends Exception {
        public DeliveryException(String message) {
            super(message);
        }
    }

    // Ảnh biên bản tải lên
    public record ProofUpload(byte[] content, String originalName) {
    }

    // deliveredAt dạng yyyy-MM-ddTHH:mm theo giờ Việt Nam (ô datetime-local)
    public record SuccessForm(String recipientName, String deliveredAt, String remark) {
        public String getRecipientName() {
            return recipientName;
        }

        public String getDeliveredAt() {
            return deliveredAt;
        }

        public String getRemark() {
            return remark;
        }
    }

    public record FailureForm(String reason, String remark) {
        public String getReason() {
            return reason;
        }

        public String getRemark() {
            return remark;
        }
    }

    // Khoá sổ tồn theo (kho, SKU) / (kho, lô)
    private record StockKey(long warehouseId, long id) {
    }

    private static final Comparator<StockKey> STOCK_ORDER =
            Comparator.comparingLong(StockKey::warehouseId).thenComparingLong(StockKey::id);

    public DeliveryScope scope(long userId) throws SQLException {
        return DeliveryScope.of(permissionDao, userId);
    }

    public boolean canUseWarehouse(long userId, long warehouseId) throws SQLException {
        Set<Long> assigned = receiptDao.findUserWarehouseIds(userId);
        return assigned.isEmpty() || assigned.contains(warehouseId);
    }

    // Phiếu xuất trong ngày (xem hôm nay thì kèm phiếu ngày trước chưa giao), lọc theo từ khoá và trạng thái điểm giao
    public DeliveryDay day(LocalDate date, String keyword, String status, DeliveryScope scope) throws SQLException {
        List<DeliveryPoint> points = scope.allowed() ? loadPoints(date, null, scope) : List.of();
        int total = 0;
        int delivered = 0;
        int failed = 0;
        for (DeliveryPoint point : points) {
            total += point.getOrderCount();
            delivered += point.getDeliveredCount();
            failed += point.getFailedCount();
        }
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<DeliveryPoint> shown = points.stream()
                .filter(point -> needle.isEmpty() || matches(point, needle))
                .filter(point -> status == null || switch (status) {
                    case DeliveryPoint.PENDING -> point.getPendingCount() > 0;
                    case DeliveryPoint.DELIVERED -> point.getDeliveredCount() > 0 && point.getPendingCount() == 0;
                    case DeliveryPoint.FAILED -> point.getFailedCount() > 0;
                    default -> true;
                }).toList();
        return new DeliveryDay(date, shown, total, delivered, failed);
    }

    private static boolean matches(DeliveryPoint point, String needle) {
        StringBuilder text = new StringBuilder();
        text.append(point.getCustomerName()).append(' ').append(point.getCustomerCode()).append(' ')
                .append(point.getAddress()).append(' ').append(point.getLabel()).append(' ');
        for (DeliveryStop stop : point.getStops()) {
            text.append(stop.getOrderNo()).append(' ').append(stop.getDeliveryNo()).append(' ');
        }
        return text.toString().toLowerCase(Locale.ROOT).contains(needle);
    }

    // null nếu ngày đó không có phiếu xuất tới điểm giao này (hoặc người dùng không được xem)
    public DeliveryPoint point(long addressId, LocalDate date, DeliveryScope scope) throws SQLException {
        if (!scope.allowed()) {
            return null;
        }
        List<DeliveryPoint> points = loadPoints(date, addressId, scope);
        return points.isEmpty() ? null : points.get(0);
    }

    private List<DeliveryPoint> loadPoints(LocalDate date, Long addressId, DeliveryScope scope) throws SQLException {
        return deliveryDao.findPoints(DateTimeUtil.startOfDayUtc(date), DateTimeUtil.startOfDayUtc(date.plusDays(1)),
                date.equals(DateTimeUtil.today()), addressId, scope.salesRepId());
    }

    // Phiếu chờ giao được chọn trong "Phạm vi hành động"; không chọn gì thì lấy mọi phiếu chờ giao của điểm
    public static List<DeliveryStop> selectStops(DeliveryPoint point, List<Long> noteIds) {
        return point.getPendingStops().stream()
                .filter(stop -> noteIds.isEmpty() || noteIds.contains(stop.getNoteId())).toList();
    }

    // Lỗi theo tên ô ("recipientName", "deliveredAt", "proofs", "remark"); rỗng là hợp lệ. nowLocal: giờ Việt Nam.
    public static Map<String, String> validateSuccess(SuccessForm form, int proofCount, LocalDateTime nowLocal) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.recipientName() == null) {
            errors.put("recipientName", "Nhập tên người nhận hàng.");
        } else if (form.recipientName().length() > NAME_MAX_LENGTH) {
            errors.put("recipientName", "Tên người nhận tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }
        LocalDateTime deliveredAt = parseDateTime(form.deliveredAt());
        if (deliveredAt == null) {
            errors.put("deliveredAt", "Chọn thời điểm giao hàng.");
        } else if (deliveredAt.isAfter(nowLocal.plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            errors.put("deliveredAt", "Thời điểm giao không được sau hiện tại.");
        }
        if (proofCount == 0) {
            errors.put("proofs", "Tải lên ít nhất một ảnh biên bản giao hàng có chữ ký.");
        } else if (proofCount > MAX_PROOFS) {
            errors.put("proofs", "Tối đa " + MAX_PROOFS + " ảnh biên bản cho một lần xác nhận.");
        }
        if (form.remark() != null && form.remark().length() > REMARK_MAX_LENGTH) {
            errors.put("remark", "Ghi chú tối đa " + REMARK_MAX_LENGTH + " ký tự.");
        }
        return errors;
    }

    public static Map<String, String> validateFailure(FailureForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.reason() == null || !FAILURE_REASONS.contains(form.reason())) {
            errors.put("reason", "Chọn lý do giao không thành công.");
        } else if (OTHER_REASON.equals(form.reason()) && form.remark() == null) {
            errors.put("remark", "Chọn \"" + OTHER_REASON + "\" thì ghi rõ lý do ở ghi chú bổ sung.");
        }
        if (form.remark() != null && form.remark().length() > REMARK_MAX_LENGTH) {
            errors.put("remark", "Ghi chú tối đa " + REMARK_MAX_LENGTH + " ký tự.");
        }
        return errors;
    }

    // Loại ảnh theo vài byte đầu (không tin phần mở rộng / content type trình duyệt gửi); null nếu không phải JPG/PNG
    public static String detectImageType(byte[] content) {
        if (content.length >= 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (content.length >= 8 && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N'
                && content[3] == 'G' && content[4] == 0x0D && content[5] == 0x0A && content[6] == 0x1A
                && content[7] == 0x0A) {
            return "image/png";
        }
        return null;
    }

    // Gọi validateSuccess trước (không lỗi); ảnh đã kiểm tra là JPG/PNG. Trả về số phiếu đã ghi nhận.
    public int confirmSuccess(List<Long> noteIds, SuccessForm form, List<ProofUpload> proofs, long actorUserId,
                              String ipAddress) throws SQLException, DeliveryException, IOException {
        LocalDateTime deliveredUtc = parseDateTime(form.deliveredAt()).atZone(DISPLAY_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        List<String> savedKeys = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<DeliveryDao.NoteLock> notes = lockPending(connection, noteIds, actorUserId);
                for (DeliveryDao.NoteLock note : notes) {
                    // Ô thời điểm giao chỉ tới phút
                    if (note.issuedAt() != null && deliveredUtc.isBefore(note.issuedAt().withSecond(0).withNano(0))) {
                        throw new DeliveryException("Thời điểm giao không được trước lúc xuất kho phiếu "
                                + note.deliveryNo() + " (" + DateTimeUtil.formatDateTime(note.issuedAt()) + ").");
                    }
                }
                List<Long> fileIds = new ArrayList<>();
                String folder = "delivery-proofs/" + LocalDate.now(DISPLAY_ZONE).format(MONTH_FOLDER) + "/";
                for (ProofUpload proof : proofs) {
                    String type = detectImageType(proof.content());
                    String key = folder + UUID.randomUUID() + ("image/png".equals(type) ? ".png" : ".jpg");
                    FileStorage.save(key, proof.content());
                    savedKeys.add(key);
                    fileIds.add(fileObjectDao.insert(connection, key, proof.originalName(), type,
                            proof.content().length, "DELIVERY_PROOF", actorUserId));
                }
                for (DeliveryDao.NoteLock note : notes) {
                    deliveryDao.markDelivered(connection, note.id(), form.recipientName(), deliveredUtc, form.remark(),
                            actorUserId);
                    for (Long fileId : fileIds) {
                        deliveryDao.insertProof(connection, note.id(), fileId, actorUserId);
                    }
                }
                Map<Long, List<String>> notesByOrder = notesByOrder(notes);
                for (DeliveryDao.NoteLock note : notes) {
                    List<String> deliveryNos = notesByOrder.remove(note.orderId());
                    if (deliveryNos != null && OrderStatus.SHIPPED.equals(note.orderStatus())
                            && deliveryDao.countNotes(connection, note.orderId(), DeliveryNoteStatus.ISSUED,
                            DeliveryNoteStatus.IN_TRANSIT) == 0
                            && deliveryDao.findRemaining(connection, note.orderId()).signum() == 0) {
                        pickListDao.changeOrderStatus(connection, note.orderId(), OrderStatus.SHIPPED,
                                OrderStatus.DELIVERED, actorUserId);
                        salesOrderDao.insertStatusHistory(connection, note.orderId(), OrderStatus.SHIPPED,
                                OrderStatus.DELIVERED, "Giao thành công theo phiếu " + String.join(", ", deliveryNos)
                                        + ", người nhận " + form.recipientName(), actorUserId);
                    }
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("deliveryNo", note.deliveryNo());
                    values.put("orderNo", note.orderNo());
                    values.put("status", DeliveryNoteStatus.DELIVERED);
                    values.put("recipientName", form.recipientName());
                    values.put("deliveredAt", DateTimeUtil.formatDateTime(deliveredUtc));
                    values.put("proofs", fileIds.size());
                    auditLogDao.insert(connection, actorUserId, "DELIVERY_CONFIRM", ENTITY, note.id(),
                            JsonUtil.object(Map.of("deliveryNo", note.deliveryNo(), "status", note.status())),
                            JsonUtil.object(values), form.remark(), ipAddress);
                }
                connection.commit();
                return notes.size();
            } catch (SQLException | DeliveryException | IOException | RuntimeException e) {
                connection.rollback();
                for (String key : savedKeys) {
                    Files.deleteIfExists(FileStorage.resolve(key));
                }
                throw e;
            }
        }
    }

    // Gọi validateFailure trước (không lỗi). Trả về số phiếu đã ghi nhận.
    public int confirmFailure(List<Long> noteIds, FailureForm form, long actorUserId, String ipAddress)
            throws SQLException, DeliveryException {
        int holdHours = settingDao.getInt(StockReservationService.HOLD_HOURS_SETTING,
                StockReservationService.DEFAULT_HOLD_HOURS);
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<DeliveryDao.NoteLock> notes = lockPending(connection, noteIds, actorUserId);
                Map<Long, List<DeliveryDao.NoteItem>> itemsByNote = new LinkedHashMap<>();
                Map<StockKey, PickListDao.Balance> balances = new TreeMap<>(STOCK_ORDER);
                Map<StockKey, BigDecimal> lotOnHand = new TreeMap<>(STOCK_ORDER);
                for (DeliveryDao.NoteLock note : notes) {
                    List<DeliveryDao.NoteItem> items = deliveryDao.findNoteItems(connection, note.id());
                    itemsByNote.put(note.id(), items);
                    for (DeliveryDao.NoteItem item : items) {
                        balances.put(new StockKey(note.warehouseId(), item.productId()), null);
                        lotOnHand.put(new StockKey(note.warehouseId(), item.lotId()), null);
                    }
                }
                // Khoá sổ tồn theo thứ tự (kho, SKU) rồi tồn theo lô theo (kho, lô)
                Map<StockKey, BigDecimal> onHand = new HashMap<>();
                Map<StockKey, BigDecimal> reserved = new HashMap<>();
                for (StockKey key : balances.keySet()) {
                    PickListDao.Balance balance = pickListDao.lockBalance(connection, key.warehouseId(), key.id());
                    if (balance == null) {
                        throw new DeliveryException("Sổ tồn chưa có SKU của phiếu xuất tại kho; không nhận lại hàng được.");
                    }
                    balances.put(key, balance);
                    onHand.put(key, balance.onHand());
                    reserved.put(key, balance.reserved());
                }
                for (StockKey key : lotOnHand.keySet()) {
                    lotOnHand.put(key, receiptDao.lockLotBalance(connection, key.warehouseId(), key.id()));
                }
                String reasonText = form.reason() + (form.remark() == null ? "" : ": " + form.remark());
                for (DeliveryDao.NoteLock note : notes) {
                    deliveryDao.markFailed(connection, note.id(), form.reason(), form.remark(), actorUserId);
                    String movementNote = "Hàng quay về kho: giao không thành công phiếu " + note.deliveryNo()
                            + " (" + form.reason() + ")";
                    for (DeliveryDao.NoteItem item : itemsByNote.get(note.id())) {
                        StockKey skuKey = new StockKey(note.warehouseId(), item.productId());
                        StockKey lotKey = new StockKey(note.warehouseId(), item.lotId());
                        BigDecimal lotAfter = lotOnHand.get(lotKey).add(item.qty());
                        BigDecimal skuAfter = onHand.get(skuKey).add(item.qty());
                        lotOnHand.put(lotKey, lotAfter);
                        onHand.put(skuKey, skuAfter);
                        receiptDao.updateLotBalance(connection, note.warehouseId(), item.lotId(), lotAfter);
                        noteDao.insertMovement(connection, note.warehouseId(), item.productId(), item.lotId(),
                                "RETURN_IN", item.qty(), skuAfter, note.id(), movementNote, actorUserId);
                        noteDao.addShipped(connection, item.orderItemId(), item.qty().negate(), BigDecimal.ZERO,
                                actorUserId);
                        // Hàng về kho được giữ lại cho đơn để soạn giao lại
                        DeliveryNoteDao.Reservation reservation = noteDao.lockReservation(connection, item.orderItemId());
                        if (reservation == null) {
                            pickListDao.insertReservation(connection, item.orderItemId(), note.warehouseId(),
                                    item.productId(), item.qty(), actorUserId);
                        } else {
                            deliveryDao.addToReservation(connection, reservation.id(), item.qty());
                        }
                        reserved.merge(skuKey, item.qty(), BigDecimal::add);
                    }
                }
                for (StockKey key : balances.keySet()) {
                    noteDao.setBalance(connection, key.warehouseId(), key.id(), onHand.get(key), reserved.get(key));
                }
                Set<Long> handledOrders = new LinkedHashSet<>();
                for (DeliveryDao.NoteLock note : notes) {
                    if (handledOrders.add(note.orderId()) && OrderStatus.SHIPPED.equals(note.orderStatus())
                            && deliveryDao.countNotes(connection, note.orderId(), DeliveryNoteStatus.DELIVERED,
                            DeliveryNoteStatus.ISSUED, DeliveryNoteStatus.IN_TRANSIT) == 0) {
                        pickListDao.changeOrderStatus(connection, note.orderId(), OrderStatus.SHIPPED,
                                OrderStatus.APPROVED, actorUserId);
                        salesOrderDao.insertStatusHistory(connection, note.orderId(), OrderStatus.SHIPPED,
                                OrderStatus.APPROVED, "Giao không thành công (" + reasonText
                                        + "); hàng quay về kho, đơn chờ soạn giao lại", actorUserId);
                        reservationDao.startHoldClock(connection, note.orderId(), holdHours);
                    }
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("deliveryNo", note.deliveryNo());
                    values.put("orderNo", note.orderNo());
                    values.put("status", DeliveryNoteStatus.FAILED);
                    values.put("failureReason", form.reason());
                    auditLogDao.insert(connection, actorUserId, "DELIVERY_FAIL", ENTITY, note.id(),
                            JsonUtil.object(Map.of("deliveryNo", note.deliveryNo(), "status", note.status())),
                            JsonUtil.object(values), reasonText, ipAddress);
                }
                connection.commit();
                return notes.size();
            } catch (SQLException | DeliveryException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Khoá các phiếu được chọn; mọi phiếu phải còn chờ giao và thuộc kho người dùng phụ trách
    private List<DeliveryDao.NoteLock> lockPending(Connection connection, List<Long> noteIds, long actorUserId)
            throws SQLException, DeliveryException {
        if (noteIds.isEmpty()) {
            throw new DeliveryException("Chọn ít nhất một đơn trong phạm vi xác nhận.");
        }
        List<DeliveryDao.NoteLock> notes = deliveryDao.lockNotes(connection, noteIds);
        if (notes.size() != noteIds.size()) {
            throw new DeliveryException("Có phiếu xuất không còn tồn tại; tải lại trang rồi chọn lại.");
        }
        for (DeliveryDao.NoteLock note : notes) {
            if (!DeliveryNoteStatus.isPending(note.status())) {
                throw new DeliveryException("Phiếu " + note.deliveryNo() + " (đơn " + note.orderNo()
                        + ") đã được ghi nhận \"" + DeliveryNoteStatus.label(note.status()) + "\" trước đó.");
            }
            if (!canUseWarehouse(actorUserId, note.warehouseId())) {
                throw new DeliveryException("Bạn chỉ xác nhận giao hàng cho phiếu xuất của kho mình phụ trách.");
            }
        }
        return notes;
    }

    private static Map<Long, List<String>> notesByOrder(List<DeliveryDao.NoteLock> notes) {
        Map<Long, List<String>> byOrder = new LinkedHashMap<>();
        for (DeliveryDao.NoteLock note : notes) {
            byOrder.computeIfAbsent(note.orderId(), key -> new ArrayList<>()).add(note.deliveryNo());
        }
        return byOrder;
    }

    static LocalDateTime parseDateTime(String value) {
        try {
            return value == null ? null : LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
