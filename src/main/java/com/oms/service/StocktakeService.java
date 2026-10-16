package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.StockDao;
import com.oms.dao.StocktakeDao;
import com.oms.model.Stocktake;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Kiểm kê kho (S5-08): tạo phiếu theo kho hoặc nhóm hàng (chụp số tồn sổ theo lô), nhập số đếm thực tế, chốt phiếu thì
// điều chỉnh tồn về số đếm, ghi thẻ kho (STOCKTAKE) và nhật ký trong cùng transaction. Chốt cần lý do và quyền riêng
// (STOCKTAKE_FINALIZE, do controller kiểm tra theo AccessRules).
public class StocktakeService {

    public static final int REASON_MAX_LENGTH = 1000;
    private static final int LIST_LIMIT = 100;
    private static final int NUMBER_ATTEMPTS = 3;
    private static final String NUMBER_PREFIX = "KK";
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String AUDIT_ENTITY = "INVENTORY";
    private static final Pattern COUNT_PATTERN = Pattern.compile("\\d{1,12}([.,]\\d{1,3})?");

    private final StocktakeDao stocktakeDao = new StocktakeDao();
    private final StockDao stockDao = new StockDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Kết quả một thao tác: error != null là lỗi nghiệp vụ (kèm câu báo), ngược lại id và mã phiếu
    public static final class Outcome {
        private final String error;
        private final long id;
        private final String stocktakeNo;

        private Outcome(String error, long id, String stocktakeNo) {
            this.error = error;
            this.id = id;
            this.stocktakeNo = stocktakeNo;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0, null);
        }

        static Outcome success(long id, String stocktakeNo) {
            return new Outcome(null, id, stocktakeNo);
        }

        public String getError() {
            return error;
        }

        public long getId() {
            return id;
        }

        public String getStocktakeNo() {
            return stocktakeNo;
        }
    }

    public List<Stocktake> findAll() throws SQLException {
        return stocktakeDao.findAll(LIST_LIMIT);
    }

    public Stocktake find(long id) throws SQLException {
        return stocktakeDao.findDetail(id);
    }

    // Số đếm: số không âm, tối đa 3 chữ số thập phân như cột decimal(18,3); null nếu sai định dạng
    static BigDecimal parseCount(String text) {
        if (text == null || !COUNT_PATTERN.matcher(text.trim()).matches()) {
            return null;
        }
        return new BigDecimal(text.trim().replace(',', '.'));
    }

    public Outcome create(long actorUserId, long warehouseId, Long categoryId, String ipAddress)
            throws SQLException {
        String warehouseName = stockDao.findWarehouseName(warehouseId);
        if (warehouseName == null) {
            return Outcome.failure("Kho không tồn tại.");
        }
        String prefix = NUMBER_PREFIX + DateTimeUtil.today().format(NUMBER_DATE) + "-";
        for (int attempt = 1; ; attempt++) {
            String number = prefix + String.format("%04d", stocktakeDao.findMaxNumber(prefix) + 1);
            try {
                return insert(number, warehouseId, categoryId, actorUserId, ipAddress);
            } catch (SQLIntegrityConstraintViolationException e) {
                // Hai người cùng tạo phiếu một lúc lấy trùng số: thử lại với số kế tiếp
                if (attempt == NUMBER_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    private Outcome insert(String number, long warehouseId, Long categoryId, long actorUserId, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long id = stocktakeDao.insert(connection, number, warehouseId, categoryId, actorUserId);
                int lines = stocktakeDao.insertSnapshot(connection, id, warehouseId, categoryId);
                if (lines == 0) {
                    connection.rollback();
                    return Outcome.failure(categoryId == null
                            ? "Kho này chưa có hàng tồn nào để kiểm kê."
                            : "Nhóm hàng này chưa có hàng tồn ở kho đã chọn để kiểm kê.");
                }
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("stocktakeNo", number);
                values.put("warehouseId", warehouseId);
                values.put("categoryId", categoryId);
                values.put("lines", lines);
                auditLogDao.insert(connection, actorUserId, "STOCKTAKE_CREATE", AUDIT_ENTITY, id, null,
                        JsonUtil.object(values), null, ipAddress);
                connection.commit();
                return Outcome.success(id, number);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // itemIds và counted cùng thứ tự; ô trống = chưa đếm. Phiếu đang ở DRAFT chuyển sang COUNTING.
    public Outcome saveCounts(long actorUserId, long stocktakeId, List<Long> itemIds, List<String> counted,
                              String ipAddress) throws SQLException {
        List<BigDecimal> quantities = new java.util.ArrayList<>();
        for (int i = 0; i < itemIds.size(); i++) {
            String text = i < counted.size() ? counted.get(i).trim() : "";
            if (text.isEmpty()) {
                quantities.add(null);
                continue;
            }
            BigDecimal qty = parseCount(text);
            if (qty == null) {
                return Outcome.failure("Số đếm dòng " + (i + 1) + " không hợp lệ: nhập số không âm, tối đa 3 chữ số"
                        + " thập phân.");
            }
            quantities.add(qty);
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                StocktakeDao.Locked locked = stocktakeDao.lock(connection, stocktakeId);
                if (locked == null) {
                    connection.rollback();
                    return Outcome.failure("NOT_FOUND");
                }
                if (!isOpen(locked.getStatus())) {
                    connection.rollback();
                    return Outcome.failure("Phiếu " + locked.getStocktakeNo() + " đã chốt hoặc đã huỷ nên không nhập"
                            + " số đếm được nữa.");
                }
                int entered = 0;
                for (int i = 0; i < itemIds.size(); i++) {
                    stocktakeDao.saveCount(connection, stocktakeId, itemIds.get(i), quantities.get(i));
                    if (quantities.get(i) != null) {
                        entered++;
                    }
                }
                if (Stocktake.DRAFT.equals(locked.getStatus())) {
                    stocktakeDao.setStatus(connection, stocktakeId, Stocktake.COUNTING, actorUserId);
                }
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("stocktakeNo", locked.getStocktakeNo());
                values.put("countedLines", entered);
                auditLogDao.insert(connection, actorUserId, "STOCKTAKE_COUNT", AUDIT_ENTITY, stocktakeId, null,
                        JsonUtil.object(values), null, ipAddress);
                connection.commit();
                return Outcome.success(stocktakeId, locked.getStocktakeNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public Outcome cancel(long actorUserId, long stocktakeId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                StocktakeDao.Locked locked = stocktakeDao.lock(connection, stocktakeId);
                if (locked == null) {
                    connection.rollback();
                    return Outcome.failure("NOT_FOUND");
                }
                if (!isOpen(locked.getStatus())) {
                    connection.rollback();
                    return Outcome.failure("Phiếu " + locked.getStocktakeNo() + " đã chốt hoặc đã huỷ.");
                }
                stocktakeDao.setStatus(connection, stocktakeId, Stocktake.CANCELLED, actorUserId);
                Map<String, Object> oldValues = new LinkedHashMap<>();
                oldValues.put("stocktakeNo", locked.getStocktakeNo());
                oldValues.put("status", locked.getStatus());
                Map<String, Object> newValues = new LinkedHashMap<>();
                newValues.put("stocktakeNo", locked.getStocktakeNo());
                newValues.put("status", Stocktake.CANCELLED);
                auditLogDao.insert(connection, actorUserId, "STOCKTAKE_CANCEL", AUDIT_ENTITY, stocktakeId,
                        JsonUtil.object(oldValues), JsonUtil.object(newValues), null, ipAddress);
                connection.commit();
                return Outcome.success(stocktakeId, locked.getStocktakeNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Chốt phiếu: bắt buộc lý do và đã đếm hết các dòng. Mỗi dòng lệch đưa tồn của lô (và tồn SKU) về số đếm.
    public Outcome finalizeStocktake(long actorUserId, long stocktakeId, String reason, String ipAddress)
            throws SQLException {
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isEmpty()) {
            return Outcome.failure("Vui lòng nhập lý do điều chỉnh tồn khi chốt kiểm kê.");
        }
        if (cleanReason.length() > REASON_MAX_LENGTH) {
            return Outcome.failure("Lý do tối đa " + REASON_MAX_LENGTH + " ký tự.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Outcome outcome = finalizeInTransaction(connection, actorUserId, stocktakeId, cleanReason, ipAddress);
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

    private Outcome finalizeInTransaction(Connection connection, long actorUserId, long stocktakeId, String reason,
                                          String ipAddress) throws SQLException {
        StocktakeDao.Locked locked = stocktakeDao.lock(connection, stocktakeId);
        if (locked == null) {
            return Outcome.failure("NOT_FOUND");
        }
        if (!isOpen(locked.getStatus())) {
            return Outcome.failure("Phiếu " + locked.getStocktakeNo() + " đã chốt hoặc đã huỷ.");
        }
        List<StocktakeDao.FinalizeItem> items = stocktakeDao.findFinalizeItems(connection, stocktakeId);
        long notCounted = items.stream().filter(item -> item.getCountedQty() == null).count();
        if (notCounted > 0) {
            return Outcome.failure("Còn " + notCounted + " dòng chưa nhập số đếm. Nhập đủ số đếm (nhập 0 nếu không"
                    + " còn hàng) rồi mới chốt.");
        }
        long warehouseId = locked.getWarehouseId();
        int adjusted = 0;
        BigDecimal netChange = BigDecimal.ZERO;
        long currentProduct = 0;
        BigDecimal onHand = BigDecimal.ZERO;
        BigDecimal reserved = BigDecimal.ZERO;
        for (StocktakeDao.FinalizeItem item : items) {
            if (item.getProductId() != currentProduct) {
                BigDecimal[] balance = stocktakeDao.lockBalance(connection, warehouseId, item.getProductId());
                if (balance == null) {
                    return Outcome.failure("SKU " + item.getSku() + " chưa có dòng tồn ở kho này nên chưa điều chỉnh"
                            + " được.");
                }
                currentProduct = item.getProductId();
                onHand = balance[0];
                reserved = balance[1];
            }
            BigDecimal lotCurrent = stocktakeDao.lockLotBalance(connection, warehouseId, item.getLotId());
            BigDecimal delta = item.getCountedQty().subtract(lotCurrent);
            if (delta.signum() == 0) {
                continue;
            }
            BigDecimal newOnHand = onHand.add(delta);
            if (newOnHand.compareTo(reserved) < 0) {
                return Outcome.failure("SKU " + item.getSku() + " đang giữ chỗ " + plain(reserved) + " cho đơn hàng,"
                        + " không thể điều chỉnh tồn xuống " + plain(newOnHand) + ". Xử lý các đơn đang giữ chỗ"
                        + " trước khi chốt.");
            }
            stocktakeDao.updateLotBalance(connection, warehouseId, item.getLotId(), item.getCountedQty());
            stocktakeDao.updateBalance(connection, warehouseId, item.getProductId(), newOnHand);
            stocktakeDao.insertMovement(connection, warehouseId, item.getProductId(), item.getLotId(), delta,
                    newOnHand, stocktakeId, reason, actorUserId);
            onHand = newOnHand;
            adjusted++;
            netChange = netChange.add(delta);
        }
        stocktakeDao.complete(connection, stocktakeId, reason, actorUserId);
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("stocktakeNo", locked.getStocktakeNo());
        oldValues.put("status", locked.getStatus());
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("stocktakeNo", locked.getStocktakeNo());
        newValues.put("status", Stocktake.COMPLETED);
        newValues.put("adjustedLines", adjusted);
        newValues.put("netChangeBase", plain(netChange));
        auditLogDao.insert(connection, actorUserId, "STOCKTAKE_FINALIZE", AUDIT_ENTITY, stocktakeId,
                JsonUtil.object(oldValues), JsonUtil.object(newValues), reason, ipAddress);
        return Outcome.success(stocktakeId, locked.getStocktakeNo());
    }

    private static boolean isOpen(String status) {
        return Stocktake.DRAFT.equals(status) || Stocktake.COUNTING.equals(status);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
