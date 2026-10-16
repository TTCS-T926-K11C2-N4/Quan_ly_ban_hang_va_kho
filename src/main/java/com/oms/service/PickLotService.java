package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.PickListDao;
import com.oms.dao.PickLotDao;
import com.oms.model.PickListStatus;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Đổi lô xuất của dòng soạn hàng (S6-04). Mặc định lô do hệ thống gợi ý theo FEFO (hạn dùng gần nhất còn đủ số lượng,
// PickListService). Thủ kho chọn lô khác được nhưng phải nhập lý do; lô chọn phải còn đủ số lượng và chưa quá hạn
// (trừ lô Quản lý kho đã cho phép, S6-01). Xuất kho trừ đúng tồn của lô đã chọn (DeliveryNoteService đọc lô từ dòng soạn).
public class PickLotService {

    public static final int REASON_MAX_LENGTH = 500;
    private static final String AUDIT_ENTITY = "PICK_LIST";

    private final PickLotDao lotDao = new PickLotDao();
    private final PickListDao pickListDao = new PickListDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Lỗi nghiệp vụ hiện cho người dùng
    public static class LotChangeException extends Exception {
        public LotChangeException(String message) {
            super(message);
        }
    }

    // Một lô có thể chọn thay (lớp có getter để JSP đọc được; EL của Tomcat 10.1 không đọc được record)
    public static class Option {
        private final long lotId;
        private final String lotNo;
        private final String expiryText;
        private final String freeText;

        Option(PickListDao.LotStock lot) {
            this.lotId = lot.lotId();
            this.lotNo = lot.lotNo();
            this.expiryText = lot.expiryDate() == null ? "không hạn" : "HSD " + DateTimeUtil.formatDay(lot.expiryDate());
            this.freeText = lot.free().stripTrailingZeros().toPlainString();
        }

        public long getLotId() {
            return lotId;
        }

        public String getLotNo() {
            return lotNo;
        }

        public String getExpiryText() {
            return expiryText;
        }

        public String getFreeText() {
            return freeText;
        }
    }

    // Dòng chưa soạn của phiếu -> các lô khác còn đủ số lượng, theo FEFO
    public Map<Long, List<Option>> candidates(long pickListId) throws SQLException {
        Map<Long, List<Option>> result = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection()) {
            lotDao.findCandidates(connection, pickListId, DateTimeUtil.today()).forEach((lineId, lots) ->
                    result.put(lineId, lots.stream().map(Option::new).toList()));
        }
        return result;
    }

    // Lý do bắt buộc; trả null nếu hợp lệ
    static String validateReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "Vui lòng nhập lý do khi chọn lô khác với gợi ý.";
        }
        if (reason.trim().length() > REASON_MAX_LENGTH) {
            return "Lý do tối đa " + REASON_MAX_LENGTH + " ký tự.";
        }
        return null;
    }

    public void changeLot(long pickListId, long lineId, long newLotId, String reason, long actorUserId,
                          String ipAddress) throws SQLException, LotChangeException {
        String reasonError = validateReason(reason);
        if (reasonError != null) {
            throw new LotChangeException(reasonError);
        }
        String cleanReason = reason.trim();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PickLotDao.Line line = lotDao.lockLine(connection, pickListId, lineId);
                if (line == null) {
                    throw new LotChangeException("Không tìm thấy dòng soạn này trong phiếu.");
                }
                if (!PickListStatus.isEditable(line.status())) {
                    throw new LotChangeException("Phiếu " + line.pickNo() + " đã soạn xong hoặc đã huỷ nên không đổi lô được.");
                }
                if (line.qtyPicked() != null) {
                    throw new LotChangeException("Dòng này đã ghi nhận số thực lấy; bỏ đánh dấu đã soạn rồi mới đổi lô.");
                }
                if (line.lotId() == newLotId) {
                    throw new LotChangeException("Lô được chọn trùng với lô hiện tại.");
                }
                PickListDao.LotStock target = null;
                for (PickListDao.LotStock lot : pickListDao.findPickableLots(connection, line.warehouseId(),
                        line.productId(), DateTimeUtil.today())) {
                    if (lot.lotId() == newLotId) {
                        target = lot;
                        break;
                    }
                }
                if (target == null) {
                    throw new LotChangeException("Lô đã chọn không còn hàng ở kho, hoặc đã quá hạn nên không xuất được.");
                }
                if (target.free().compareTo(line.qtyRequired()) < 0) {
                    throw new LotChangeException("Lô " + target.lotNo() + " chỉ còn " + plain(target.free())
                            + " khả dụng, không đủ " + plain(line.qtyRequired()) + " cho dòng này.");
                }
                lotDao.changeLot(connection, lineId, newLotId, cleanReason);
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("pickNo", line.pickNo());
                before.put("sku", line.sku());
                before.put("lotNo", line.lotNo());
                Map<String, Object> after = new LinkedHashMap<>(before);
                after.put("lotNo", target.lotNo());
                auditLogDao.insert(connection, actorUserId, "PICK_LIST_LOT_CHANGE", AUDIT_ENTITY, pickListId,
                        JsonUtil.object(before), JsonUtil.object(after), cleanReason, ipAddress);
                connection.commit();
            } catch (SQLException | LotChangeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static String plain(java.math.BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
