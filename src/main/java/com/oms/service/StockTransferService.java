package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.StockTransferDao;
import com.oms.model.StockTransfer;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Chuyển kho nội bộ (S5-07). Xuất kho đi: trừ tồn kho đi ngay (hàng "đang trên đường", chưa cộng vào kho đến).
// Kho đến xác nhận nhận: lúc đó tồn kho đến mới được cộng; nhận thiếu so với số đã xuất thì phải nhập lý do.
// Số lượng tính theo đơn vị cơ sở. Mọi thao tác làm đổi tồn ghi thẻ kho và nhật ký cùng transaction.
public class StockTransferService {

    public static final int NOTE_MAX_LENGTH = 1000;
    public static final int REASON_MAX_LENGTH = 500;
    private static final int LIST_LIMIT = 100;
    private static final int NUMBER_ATTEMPTS = 3;
    private static final String NUMBER_PREFIX = "CK";
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String AUDIT_ENTITY = "INVENTORY";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final Pattern QTY_PATTERN = Pattern.compile("\\d{1,12}([.,]\\d{1,3})?");

    private final StockTransferDao transferDao = new StockTransferDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // error != null: lỗi nghiệp vụ (hoặc "NOT_FOUND"); ngược lại id và mã phiếu
    public static final class Outcome {
        private final String error;
        private final long id;
        private final String transferNo;

        private Outcome(String error, long id, String transferNo) {
            this.error = error;
            this.id = id;
            this.transferNo = transferNo;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0, null);
        }

        static Outcome success(long id, String transferNo) {
            return new Outcome(null, id, transferNo);
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

        public String getTransferNo() {
            return transferNo;
        }
    }

    public List<StockTransfer> findAll() throws SQLException {
        return transferDao.findAll(LIST_LIMIT);
    }

    public StockTransfer find(long id) throws SQLException {
        return transferDao.findDetail(id);
    }

    public List<StockTransfer.LotOption> findLotOptions(long warehouseId) throws SQLException {
        return transferDao.findLotOptions(warehouseId);
    }

    // Số lượng: số dương, tối đa 3 chữ số thập phân; null nếu sai
    static BigDecimal parseQty(String text) {
        if (text == null || !QTY_PATTERN.matcher(text.trim()).matches()) {
            return null;
        }
        BigDecimal qty = new BigDecimal(text.trim().replace(',', '.'));
        return qty.signum() > 0 ? qty : null;
    }

    // Số nhận: số không âm (0 = không nhận được gì)
    static BigDecimal parseReceived(String text) {
        if (text == null || !QTY_PATTERN.matcher(text.trim()).matches()) {
            return null;
        }
        return new BigDecimal(text.trim().replace(',', '.'));
    }

    public Outcome create(long actorUserId, long fromWarehouseId, long toWarehouseId, String note, String ipAddress)
            throws SQLException {
        if (fromWarehouseId == toWarehouseId) {
            return Outcome.failure("Kho đi và kho đến phải khác nhau.");
        }
        if (!transferDao.isActiveWarehouse(fromWarehouseId) || !transferDao.isActiveWarehouse(toWarehouseId)) {
            return Outcome.failure("Kho đi và kho đến phải là kho đang hoạt động.");
        }
        String cleanNote = note == null || note.trim().isEmpty() ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > NOTE_MAX_LENGTH) {
            return Outcome.failure("Ghi chú tối đa " + NOTE_MAX_LENGTH + " ký tự.");
        }
        String prefix = NUMBER_PREFIX + DateTimeUtil.today().format(NUMBER_DATE) + "-";
        for (int attempt = 1; ; attempt++) {
            String number = prefix + String.format("%04d", transferDao.findMaxNumber(prefix) + 1);
            try (Connection connection = DbConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    long id = transferDao.insert(connection, number, fromWarehouseId, toWarehouseId, cleanNote,
                            actorUserId);
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("transferNo", number);
                    values.put("fromWarehouseId", fromWarehouseId);
                    values.put("toWarehouseId", toWarehouseId);
                    auditLogDao.insert(connection, actorUserId, "TRANSFER_CREATE", AUDIT_ENTITY, id, null,
                            JsonUtil.object(values), null, ipAddress);
                    connection.commit();
                    return Outcome.success(id, number);
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

    // Thêm một dòng vào phiếu nháp: lô phải đang có hàng ở kho đi và số lượng không vượt tồn của lô
    public Outcome addLine(long actorUserId, long transferId, long lotId, String rawQty, String ipAddress)
            throws SQLException {
        BigDecimal qty = parseQty(rawQty);
        if (qty == null) {
            return Outcome.failure("Số lượng phải lớn hơn 0, tối đa 3 chữ số thập phân.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                StockTransferDao.Locked locked = transferDao.lock(connection, transferId);
                if (locked == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!StockTransfer.DRAFT.equals(locked.getStatus())) {
                    connection.rollback();
                    return Outcome.failure("Phiếu " + locked.getTransferNo() + " không còn là nháp nên không sửa dòng"
                            + " hàng được.");
                }
                Long productId = transferDao.findProductOfLot(connection, locked.getFromWarehouseId(), lotId);
                if (productId == null) {
                    connection.rollback();
                    return Outcome.failure("Lô hàng đã chọn không có ở kho đi.");
                }
                if (transferDao.hasItemForLot(connection, transferId, lotId)) {
                    connection.rollback();
                    return Outcome.failure("Lô này đã có trong phiếu. Xoá dòng cũ rồi thêm lại nếu muốn đổi số lượng.");
                }
                BigDecimal onHand = transferDao.lockLotBalance(connection, locked.getFromWarehouseId(), lotId);
                if (qty.compareTo(onHand) > 0) {
                    connection.rollback();
                    return Outcome.failure("Số lượng chuyển " + plain(qty) + " vượt tồn của lô ở kho đi (" + plain(onHand)
                            + ").");
                }
                transferDao.insertItem(connection, transferId, productId, lotId, qty);
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("transferNo", locked.getTransferNo());
                values.put("action", "ADD_LINE");
                values.put("lotId", lotId);
                values.put("qtyBase", plain(qty));
                auditLogDao.insert(connection, actorUserId, "TRANSFER_UPDATE", AUDIT_ENTITY, transferId, null,
                        JsonUtil.object(values), null, ipAddress);
                connection.commit();
                return Outcome.success(transferId, locked.getTransferNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public Outcome removeLine(long actorUserId, long transferId, long itemId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                StockTransferDao.Locked locked = transferDao.lock(connection, transferId);
                if (locked == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!StockTransfer.DRAFT.equals(locked.getStatus())) {
                    connection.rollback();
                    return Outcome.failure("Phiếu " + locked.getTransferNo() + " không còn là nháp nên không sửa dòng"
                            + " hàng được.");
                }
                if (transferDao.deleteItem(connection, transferId, itemId) == 1) {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("transferNo", locked.getTransferNo());
                    values.put("action", "REMOVE_LINE");
                    values.put("itemId", itemId);
                    auditLogDao.insert(connection, actorUserId, "TRANSFER_UPDATE", AUDIT_ENTITY, transferId, null,
                            JsonUtil.object(values), null, ipAddress);
                }
                connection.commit();
                return Outcome.success(transferId, locked.getTransferNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public Outcome cancel(long actorUserId, long transferId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                StockTransferDao.Locked locked = transferDao.lock(connection, transferId);
                if (locked == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!StockTransfer.DRAFT.equals(locked.getStatus())) {
                    connection.rollback();
                    return Outcome.failure("Chỉ huỷ được phiếu nháp. Phiếu " + locked.getTransferNo()
                            + " đã xuất kho hoặc đã đóng.");
                }
                transferDao.setStatus(connection, transferId, StockTransfer.CANCELLED, actorUserId);
                auditLogDao.insert(connection, actorUserId, "TRANSFER_CANCEL", AUDIT_ENTITY, transferId,
                        JsonUtil.object(statusValues(locked.getTransferNo(), StockTransfer.DRAFT)),
                        JsonUtil.object(statusValues(locked.getTransferNo(), StockTransfer.CANCELLED)), null,
                        ipAddress);
                connection.commit();
                return Outcome.success(transferId, locked.getTransferNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Xuất kho đi: trừ tồn lô và tồn SKU ở kho đi, ghi TRANSFER_OUT; vượt tồn khả dụng (đã trừ giữ chỗ) thì không xuất
    public Outcome ship(long actorUserId, long transferId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Outcome outcome = shipInTransaction(connection, actorUserId, transferId, ipAddress);
                finish(connection, outcome);
                return outcome;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private Outcome shipInTransaction(Connection connection, long actorUserId, long transferId, String ipAddress)
            throws SQLException {
        StockTransferDao.Locked locked = transferDao.lock(connection, transferId);
        if (locked == null) {
            return Outcome.failure(NOT_FOUND);
        }
        if (!StockTransfer.DRAFT.equals(locked.getStatus())) {
            return Outcome.failure("Phiếu " + locked.getTransferNo() + " không còn là nháp nên không xuất kho được.");
        }
        List<StockTransfer.Item> items = transferDao.findItems(connection, transferId);
        if (items.isEmpty()) {
            return Outcome.failure("Phiếu chưa có dòng hàng nào để xuất.");
        }
        long warehouseId = locked.getFromWarehouseId();
        long currentProduct = 0;
        BigDecimal onHand = BigDecimal.ZERO;
        BigDecimal reserved = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (StockTransfer.Item item : items) {
            if (item.getProductId() != currentProduct) {
                BigDecimal[] balance = transferDao.lockBalance(connection, warehouseId, item.getProductId());
                if (balance == null) {
                    return Outcome.failure("SKU " + item.getSku() + " không còn tồn ở kho đi.");
                }
                currentProduct = item.getProductId();
                onHand = balance[0];
                reserved = balance[1];
            }
            BigDecimal lotQty = transferDao.lockLotBalance(connection, warehouseId, item.getLotId());
            if (item.getQtySent().compareTo(lotQty) > 0) {
                return Outcome.failure("Lô " + item.getLotNo() + " của " + item.getSku() + " chỉ còn "
                        + plain(lotQty) + " ở kho đi, không đủ " + plain(item.getQtySent()) + " để chuyển.");
            }
            BigDecimal newOnHand = onHand.subtract(item.getQtySent());
            if (newOnHand.compareTo(reserved) < 0) {
                return Outcome.failure("SKU " + item.getSku() + " đang giữ chỗ " + plain(reserved) + " cho đơn hàng,"
                        + " chỉ chuyển được tối đa " + plain(onHand.subtract(reserved)) + ".");
            }
            transferDao.updateLotBalance(connection, warehouseId, item.getLotId(), lotQty.subtract(item.getQtySent()));
            transferDao.updateBalance(connection, warehouseId, item.getProductId(), newOnHand);
            transferDao.insertMovement(connection, warehouseId, item.getProductId(), item.getLotId(), "TRANSFER_OUT",
                    item.getQtySent().negate(), newOnHand, transferId, "Xuất chuyển kho " + locked.getTransferNo(),
                    actorUserId);
            onHand = newOnHand;
            total = total.add(item.getQtySent());
        }
        transferDao.markShipped(connection, transferId, actorUserId);
        Map<String, Object> newValues = statusValues(locked.getTransferNo(), StockTransfer.IN_TRANSIT);
        newValues.put("lines", items.size());
        newValues.put("totalQtyBase", plain(total));
        auditLogDao.insert(connection, actorUserId, "TRANSFER_SHIP", AUDIT_ENTITY, transferId,
                JsonUtil.object(statusValues(locked.getTransferNo(), StockTransfer.DRAFT)),
                JsonUtil.object(newValues), null, ipAddress);
        return Outcome.success(transferId, locked.getTransferNo());
    }

    // Kho đến xác nhận nhận. itemIds / received / reasons cùng thứ tự; nhận khác số đã xuất thì bắt buộc lý do.
    public Outcome receive(long actorUserId, long transferId, List<Long> itemIds, List<String> received,
                           List<String> reasons, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Outcome outcome = receiveInTransaction(connection, actorUserId, transferId, itemIds, received, reasons,
                        ipAddress);
                finish(connection, outcome);
                return outcome;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private Outcome receiveInTransaction(Connection connection, long actorUserId, long transferId,
                                         List<Long> itemIds, List<String> received, List<String> reasons,
                                         String ipAddress) throws SQLException {
        StockTransferDao.Locked locked = transferDao.lock(connection, transferId);
        if (locked == null) {
            return Outcome.failure(NOT_FOUND);
        }
        if (!StockTransfer.IN_TRANSIT.equals(locked.getStatus())) {
            return Outcome.failure("Phiếu " + locked.getTransferNo() + " không ở trạng thái đang trên đường nên không"
                    + " nhận được.");
        }
        List<StockTransfer.Item> items = transferDao.findItems(connection, transferId);
        Map<Long, BigDecimal> receivedBy = new LinkedHashMap<>();
        Map<Long, String> reasonBy = new LinkedHashMap<>();
        for (int i = 0; i < itemIds.size(); i++) {
            String text = i < received.size() ? received.get(i) : null;
            BigDecimal qty = parseReceived(text);
            if (qty == null) {
                return Outcome.failure("Số lượng nhận dòng " + (i + 1) + " không hợp lệ: nhập số không âm, tối đa 3"
                        + " chữ số thập phân.");
            }
            receivedBy.put(itemIds.get(i), qty);
            String reason = i < reasons.size() ? reasons.get(i).trim() : "";
            reasonBy.put(itemIds.get(i), reason);
        }
        long warehouseId = locked.getToWarehouseId();
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalShort = BigDecimal.ZERO;
        int line = 0;
        for (StockTransfer.Item item : items) {
            line++;
            BigDecimal qty = receivedBy.get(item.getId());
            if (qty == null) {
                return Outcome.failure("Thiếu số lượng nhận của dòng " + line + " (" + item.getSku() + ").");
            }
            if (qty.compareTo(item.getQtySent()) > 0) {
                return Outcome.failure("Dòng " + line + " (" + item.getSku() + "): số nhận " + plain(qty)
                        + " lớn hơn số đã xuất " + plain(item.getQtySent()) + ".");
            }
            boolean differs = qty.compareTo(item.getQtySent()) != 0;
            String reason = reasonBy.get(item.getId());
            if (differs && (reason == null || reason.isEmpty())) {
                return Outcome.failure("Dòng " + line + " (" + item.getSku() + ") nhận khác số đã xuất: vui lòng nhập"
                        + " lý do chênh lệch.");
            }
            if (reason != null && reason.length() > REASON_MAX_LENGTH) {
                return Outcome.failure("Lý do chênh lệch tối đa " + REASON_MAX_LENGTH + " ký tự.");
            }
            transferDao.setReceived(connection, item.getId(), qty, differs ? reason : null);
            if (qty.signum() > 0) {
                transferDao.ensureBalance(connection, warehouseId, item.getProductId());
                BigDecimal[] balance = transferDao.lockBalance(connection, warehouseId, item.getProductId());
                BigDecimal lotQty = transferDao.lockLotBalance(connection, warehouseId, item.getLotId());
                BigDecimal newOnHand = balance[0].add(qty);
                transferDao.updateLotBalance(connection, warehouseId, item.getLotId(), lotQty.add(qty));
                transferDao.updateBalance(connection, warehouseId, item.getProductId(), newOnHand);
                transferDao.insertMovement(connection, warehouseId, item.getProductId(), item.getLotId(),
                        "TRANSFER_IN", qty, newOnHand, transferId,
                        "Nhận chuyển kho " + locked.getTransferNo(), actorUserId);
            }
            totalReceived = totalReceived.add(qty);
            totalShort = totalShort.add(item.getQtySent().subtract(qty));
        }
        transferDao.markReceived(connection, transferId, actorUserId);
        Map<String, Object> newValues = statusValues(locked.getTransferNo(), StockTransfer.RECEIVED);
        newValues.put("totalReceivedBase", plain(totalReceived));
        newValues.put("totalShortBase", plain(totalShort));
        auditLogDao.insert(connection, actorUserId, "TRANSFER_RECEIVE", AUDIT_ENTITY, transferId,
                JsonUtil.object(statusValues(locked.getTransferNo(), StockTransfer.IN_TRANSIT)),
                JsonUtil.object(newValues), null, ipAddress);
        return Outcome.success(transferId, locked.getTransferNo());
    }

    private static void finish(Connection connection, Outcome outcome) throws SQLException {
        if (outcome.getError() != null) {
            connection.rollback();
        } else {
            connection.commit();
        }
    }

    private static Map<String, Object> statusValues(String transferNo, String status) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("transferNo", transferNo);
        values.put("status", status);
        return values;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    // Cho servlet: danh sách id dòng từ tham số, giá trị sai thành -1
    public static List<Long> parseIds(String[] values) {
        List<Long> ids = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                try {
                    ids.add(Long.parseLong(value.trim()));
                } catch (NumberFormatException e) {
                    ids.add(-1L);
                }
            }
        }
        return ids;
    }
}
