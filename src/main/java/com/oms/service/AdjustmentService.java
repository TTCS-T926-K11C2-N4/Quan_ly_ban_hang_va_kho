package com.oms.service;

import com.oms.dao.AdjustmentDao;
import com.oms.dao.AuditLogDao;
import com.oms.dao.StockTransferDao;
import com.oms.dao.WarehouseDao;
import com.oms.model.SelectOption;
import com.oms.model.StockAdjustment;
import com.oms.model.StockTransfer;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Phiếu điều chỉnh tồn kho có phê duyệt (S8-03): hàng hỏng, hàng mất, hàng hết hạn phải huỷ.
//  * Bắt buộc lý do và ghi chú; giá trị tổn thất = số lượng x giá vốn hiện tại của SKU, chụp lại lúc lập phiếu.
//  * Chỉ người có quyền chốt kho (Quản lý kho) duyệt, và người duyệt không được là người lập.
//  * Duyệt xong mới trừ tồn (theo lô) và ghi thẻ kho ADJUSTMENT; từ chối thì không đụng tồn.
public class AdjustmentService {

    public static final int LIST_LIMIT = 100;
    public static final int NOTE_MAX = 1000;
    private static final String AUDIT_ENTITY = "STOCK_ADJUSTMENT";
    private static final String NOT_FOUND = "NOT_FOUND";

    private final AdjustmentDao adjustmentDao = new AdjustmentDao();
    private final StockTransferDao stockDao = new StockTransferDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // error != null: lỗi nghiệp vụ (hoặc "NOT_FOUND"); ngược lại id và số phiếu
    public static final class Outcome {
        private final String error;
        private final long id;
        private final String adjustmentNo;

        private Outcome(String error, long id, String adjustmentNo) {
            this.error = error;
            this.id = id;
            this.adjustmentNo = adjustmentNo;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0, null);
        }

        static Outcome success(long id, String adjustmentNo) {
            return new Outcome(null, id, adjustmentNo);
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

        public String getAdjustmentNo() {
            return adjustmentNo;
        }
    }

    // Số lượng trừ nhập ở form theo từng lô
    public record Input(long lotId, String qtyText) {
    }

    public List<SelectOption> warehouses() throws SQLException {
        return warehouseDao.findActive();
    }

    public List<StockTransfer.LotOption> lotOptions(long warehouseId) throws SQLException {
        return stockDao.findLotOptions(warehouseId);
    }

    public boolean isActiveWarehouse(long warehouseId) throws SQLException {
        return stockDao.isActiveWarehouse(warehouseId);
    }

    public List<StockAdjustment> findAll() throws SQLException {
        return adjustmentDao.findAll(LIST_LIMIT);
    }

    public StockAdjustment find(long id) throws SQLException {
        return adjustmentDao.find(id);
    }

    // null nếu không hợp lệ (âm, 0, quá 3 số lẻ, không phải số)
    static BigDecimal parseQty(String text) {
        if (text == null || !text.trim().matches("\\d{1,9}([.,]\\d{1,3})?")) {
            return null;
        }
        BigDecimal value = new BigDecimal(text.trim().replace(',', '.'));
        return value.signum() > 0 ? value : null;
    }

    // Giá trị tổn thất của một dòng: số lượng cơ sở x giá vốn cơ sở, làm tròn tới đồng
    static BigDecimal loss(BigDecimal qtyBase, BigDecimal unitCost) {
        return qtyBase.multiply(unitCost).setScale(0, RoundingMode.HALF_UP).setScale(2);
    }

    public Outcome create(long actorUserId, long warehouseId, String reason, String note, List<Input> inputs,
                          String ipAddress) throws SQLException {
        if (!StockAdjustment.isReason(reason)) {
            return Outcome.failure("Chọn lý do điều chỉnh (hàng hỏng, hàng mất hoặc huỷ hàng hết hạn).");
        }
        String cleanNote = note == null ? "" : note.trim();
        if (cleanNote.isEmpty()) {
            return Outcome.failure("Ghi rõ lý do chi tiết của phiếu điều chỉnh.");
        }
        if (cleanNote.length() > NOTE_MAX) {
            return Outcome.failure("Ghi chú tối đa " + NOTE_MAX + " ký tự.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Outcome outcome = createInTransaction(connection, actorUserId, warehouseId, reason, cleanNote,
                        inputs, ipAddress);
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

    private Outcome createInTransaction(Connection connection, long actorUserId, long warehouseId, String reason,
                                        String note, List<Input> inputs, String ipAddress) throws SQLException {
        record Line(long lotId, AdjustmentDao.LotInfo lot, BigDecimal qty, BigDecimal loss) {
        }
        List<Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Input input : inputs) {
            if (input.qtyText() == null || input.qtyText().trim().isEmpty()) {
                continue;
            }
            AdjustmentDao.LotInfo lot = adjustmentDao.findLotInfo(connection, warehouseId, input.lotId());
            if (lot == null) {
                return Outcome.failure(NOT_FOUND);
            }
            BigDecimal qty = parseQty(input.qtyText());
            if (qty == null) {
                return Outcome.failure("Số lượng của " + lot.sku() + " (lô " + lot.lotNo() + ") không hợp lệ.");
            }
            if (qty.compareTo(lot.onHand()) > 0) {
                return Outcome.failure("Số lượng của " + lot.sku() + " (lô " + lot.lotNo() + ") vượt tồn của lô ("
                        + lot.onHand().stripTrailingZeros().toPlainString() + ").");
            }
            BigDecimal loss = loss(qty, lot.costPrice());
            total = total.add(loss);
            lines.add(new Line(input.lotId(), lot, qty, loss));
        }
        if (lines.isEmpty()) {
            return Outcome.failure("Nhập số lượng điều chỉnh ít nhất một lô hàng.");
        }
        String no = adjustmentDao.nextAdjustmentNo(connection, DateTimeUtil.today().getYear());
        long id = adjustmentDao.insert(connection, no, warehouseId, reason, total, note, actorUserId);
        for (Line line : lines) {
            adjustmentDao.insertItem(connection, id, line.lot().productId(), line.lotId(), line.qty().negate(),
                    line.lot().costPrice(), line.loss());
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("adjustmentNo", no);
        values.put("warehouseId", warehouseId);
        values.put("reason", reason);
        values.put("lines", lines.size());
        values.put("totalLoss", total.stripTrailingZeros().toPlainString());
        values.put("status", StockAdjustment.PENDING);
        auditLogDao.insert(connection, actorUserId, "ADJUSTMENT_CREATE", AUDIT_ENTITY, id, null,
                JsonUtil.object(values), note, ipAddress);
        return Outcome.success(id, no);
    }

    // Duyệt (approve = true): trừ tồn theo lô và ghi thẻ kho; từ chối (approve = false) cần lý do. Người duyệt phải
    // khác người lập (cũng được CHECK ở CSDL).
    public Outcome decide(long actorUserId, long id, boolean approve, String reason, String ipAddress)
            throws SQLException {
        String cleanReason = reason == null ? null : reason.trim();
        if (!approve && (cleanReason == null || cleanReason.isEmpty())) {
            return Outcome.failure("Nhập lý do từ chối phiếu điều chỉnh.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                AdjustmentDao.Lock locked = adjustmentDao.lock(connection, id);
                if (locked == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!StockAdjustment.PENDING.equals(locked.status())) {
                    connection.rollback();
                    return Outcome.failure("Phiếu " + locked.adjustmentNo() + " không còn ở trạng thái Chờ duyệt.");
                }
                if (locked.createdBy() == actorUserId) {
                    connection.rollback();
                    return Outcome.failure("Người lập phiếu không được tự duyệt phiếu của mình.");
                }
                if (approve) {
                    for (AdjustmentDao.ApplyItem item : adjustmentDao.findApplyItems(connection, id)) {
                        BigDecimal[] balance = stockDao.lockBalance(connection, locked.warehouseId(),
                                item.productId());
                        BigDecimal lotOnHand = stockDao.lockLotBalance(connection, locked.warehouseId(),
                                item.lotId());
                        BigDecimal change = item.qtyChange();
                        if (balance == null || lotOnHand.add(change).signum() < 0
                                || balance[0].add(change).compareTo(balance[1]) < 0) {
                            connection.rollback();
                            return Outcome.failure("Tồn kho hiện tại không đủ để trừ (hàng đã xuất hoặc đang giữ chỗ"
                                    + " cho đơn); không duyệt được phiếu " + locked.adjustmentNo() + ".");
                        }
                        BigDecimal onHand = balance[0].add(change);
                        stockDao.updateBalance(connection, locked.warehouseId(), item.productId(), onHand);
                        stockDao.updateLotBalance(connection, locked.warehouseId(), item.lotId(),
                                lotOnHand.add(change));
                        adjustmentDao.insertMovement(connection, locked.warehouseId(), item.productId(),
                                item.lotId(), change, onHand, id, "Điều chỉnh tồn " + locked.adjustmentNo(),
                                actorUserId);
                    }
                }
                String status = approve ? StockAdjustment.APPROVED : StockAdjustment.REJECTED;
                adjustmentDao.decide(connection, id, status, actorUserId);
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("status", StockAdjustment.PENDING);
                Map<String, Object> after = new LinkedHashMap<>();
                after.put("adjustmentNo", locked.adjustmentNo());
                after.put("status", status);
                auditLogDao.insert(connection, actorUserId, approve ? "ADJUSTMENT_APPROVE" : "ADJUSTMENT_REJECT",
                        AUDIT_ENTITY, id, JsonUtil.object(before), JsonUtil.object(after), cleanReason, ipAddress);
                connection.commit();
                return Outcome.success(id, locked.adjustmentNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
