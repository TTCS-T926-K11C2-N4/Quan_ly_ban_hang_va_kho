package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.LotDao;
import com.oms.dao.SystemSettingDao;
import com.oms.model.LotRow;
import com.oms.model.LotStatus;
import com.oms.model.PageResult;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Lô và hạn sử dụng (S6-01): bật / tắt quản lý lô theo SKU; danh sách lô kèm hạn dùng, số còn lại, cảnh báo sắp hết
// hạn theo ngưỡng khai báo; lô đã quá hạn không được soạn / xuất trừ khi Quản lý kho cho phép kèm lý do.
public class LotService {

    public static final int PAGE_SIZE = 20;
    public static final int PRODUCT_LIMIT = 30;
    public static final int REASON_MAX_LENGTH = 500;
    public static final int DEFAULT_NEAR_DAYS = 30;
    public static final int MAX_NEAR_DAYS = 365;
    private static final String AUDIT_ENTITY = "INVENTORY";
    private static final String NOT_FOUND = "NOT_FOUND";

    private final LotDao lotDao = new LotDao();
    private final SystemSettingDao settingDao = new SystemSettingDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // error != null: lỗi nghiệp vụ (hoặc "NOT_FOUND"); ngược lại message là câu báo thành công
    public static final class Outcome {
        private final String error;
        private final String message;

        private Outcome(String error, String message) {
            this.error = error;
            this.message = message;
        }

        static Outcome failure(String error) {
            return new Outcome(error, null);
        }

        static Outcome success(String message) {
            return new Outcome(null, message);
        }

        public boolean isNotFound() {
            return NOT_FOUND.equals(error);
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }

    public int nearExpiryDays() throws SQLException {
        return settingDao.getInt(LotDao.NEAR_EXPIRY_KEY, DEFAULT_NEAR_DAYS);
    }

    public long[] countByStatus(String keyword, Long warehouseId) throws SQLException {
        return lotDao.countByStatus(keyword, warehouseId, DateTimeUtil.today(), nearExpiryDays());
    }

    public PageResult<LotRow> search(String keyword, Long warehouseId, LotStatus status, int requestedPage)
            throws SQLException {
        LocalDate today = DateTimeUtil.today();
        int nearDays = nearExpiryDays();
        long total = lotDao.count(keyword, warehouseId, status, today, nearDays);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        return new PageResult<>(lotDao.findRows(keyword, warehouseId, status, today, nearDays,
                (page - 1) * PAGE_SIZE, PAGE_SIZE), page, PAGE_SIZE, total);
    }

    public List<LotDao.ProductRow> findProducts(String keyword) throws SQLException {
        return lotDao.findProducts(keyword, PRODUCT_LIMIT);
    }

    // Bật quản lý lô luôn được; tắt chỉ khi SKU không còn lô nào (ngoài lô mặc định) còn hàng
    public Outcome setTracked(long actorUserId, long productId, boolean tracked, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LotDao.ProductRow product = lotDao.lockProduct(connection, productId);
                if (product == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (product.isTracked() == tracked) {
                    connection.rollback();
                    return Outcome.success("SKU " + product.getSku() + " đã ở trạng thái đó rồi.");
                }
                if (!tracked && product.getLotsInStock() > 0) {
                    connection.rollback();
                    return Outcome.failure("SKU " + product.getSku() + " còn " + product.getLotsInStock()
                            + " lô đang có hàng nên chưa tắt được quản lý lô. Xuất hết hoặc kiểm kê về 0 các lô này trước.");
                }
                lotDao.setTracked(connection, productId, tracked, actorUserId);
                auditLogDao.insert(connection, actorUserId, "LOT_TRACKING_UPDATE", AUDIT_ENTITY, productId,
                        JsonUtil.object(trackingValues(product.getSku(), product.isTracked())),
                        JsonUtil.object(trackingValues(product.getSku(), tracked)), null, ipAddress);
                connection.commit();
                return Outcome.success((tracked ? "Đã bật" : "Đã tắt") + " quản lý lô cho SKU " + product.getSku() + ".");
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Quản lý kho cho phép (reason bắt buộc) hoặc thu hồi việc xuất lô đã quá hạn
    public Outcome setExpiredIssue(long actorUserId, long lotId, boolean allow, String reason, String ipAddress)
            throws SQLException {
        String cleanReason = reason == null ? "" : reason.trim();
        if (allow && cleanReason.isEmpty()) {
            return Outcome.failure("Vui lòng nhập lý do cho phép xuất lô quá hạn.");
        }
        if (cleanReason.length() > REASON_MAX_LENGTH) {
            return Outcome.failure("Lý do tối đa " + REASON_MAX_LENGTH + " ký tự.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LotDao.LotInfo lot = lotDao.lockLot(connection, lotId);
                if (lot == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (allow && (lot.getExpiryDate() == null || !lot.getExpiryDate().isBefore(DateTimeUtil.today()))) {
                    connection.rollback();
                    return Outcome.failure("Lô " + lot.getLotNo() + " chưa quá hạn nên không cần cho phép xuất.");
                }
                lotDao.setExpiredIssue(connection, lotId, allow ? actorUserId : null, allow ? cleanReason : null);
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("sku", lot.getSku());
                before.put("lotNo", lot.getLotNo());
                before.put("expiredIssueAllowed", lot.isOverridden());
                Map<String, Object> after = new LinkedHashMap<>(before);
                after.put("expiredIssueAllowed", allow);
                auditLogDao.insert(connection, actorUserId, allow ? "LOT_EXPIRED_ISSUE_ALLOW" : "LOT_EXPIRED_ISSUE_REVOKE",
                        AUDIT_ENTITY, lotId, JsonUtil.object(before), JsonUtil.object(after),
                        allow ? cleanReason : null, ipAddress);
                connection.commit();
                return Outcome.success(allow
                        ? "Đã cho phép xuất lô quá hạn " + lot.getLotNo() + " của " + lot.getSku() + "."
                        : "Đã thu hồi cho phép xuất lô " + lot.getLotNo() + " của " + lot.getSku() + ".");
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Ngưỡng cảnh báo lô sắp hết hạn (ngày), 1..365
    public Outcome saveNearExpiryDays(long actorUserId, String rawDays, String ipAddress) throws SQLException {
        int days;
        try {
            days = Integer.parseInt(rawDays == null ? "" : rawDays.trim());
        } catch (NumberFormatException e) {
            return Outcome.failure("Ngưỡng cảnh báo phải là số ngày nguyên từ 1 đến " + MAX_NEAR_DAYS + ".");
        }
        if (days < 1 || days > MAX_NEAR_DAYS) {
            return Outcome.failure("Ngưỡng cảnh báo phải là số ngày nguyên từ 1 đến " + MAX_NEAR_DAYS + ".");
        }
        int before = nearExpiryDays();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lotDao.saveNearExpiryDays(connection, days, actorUserId);
                Map<String, Object> oldValues = new LinkedHashMap<>();
                oldValues.put("nearExpiryDays", before);
                Map<String, Object> newValues = new LinkedHashMap<>();
                newValues.put("nearExpiryDays", days);
                auditLogDao.insert(connection, actorUserId, "LOT_NEAR_EXPIRY_UPDATE", AUDIT_ENTITY, 0L,
                        JsonUtil.object(oldValues), JsonUtil.object(newValues), null, ipAddress);
                connection.commit();
                return Outcome.success("Đã đặt ngưỡng cảnh báo lô sắp hết hạn là " + days + " ngày.");
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static Map<String, Object> trackingValues(String sku, boolean tracked) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sku", sku);
        values.put("isLotTracked", tracked);
        return values;
    }
}
