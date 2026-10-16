package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.ReturnDao;
import com.oms.dao.StockTransferDao;
import com.oms.model.Permission;
import com.oms.model.ReturnLine;
import com.oms.model.SalesReturn;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Phiếu trả hàng từ đại lý (S8-01).
//  * Lập từ hoá đơn gốc, mỗi dòng chọn số lượng và tình trạng (tốt/hỏng), bắt buộc có lý do trả.
//  * Số lượng trả không vượt (đã giao - đã trả trước đó, kể cả phiếu đang chờ nhận); khoá hoá đơn để các lần trả
//    đồng thời xếp hàng.
//  * Hàng trả về đúng lô đã giao; hàng hỏng về kho hàng lỗi, hàng tốt về kho đã xuất.
//  * Chỉ khi Quản lý kho xác nhận nhận hàng mới cộng tồn và ghi thẻ kho (RETURN_IN).
public class ReturnService {

    public static final int LIST_LIMIT = 100;
    public static final int NOTE_MAX = 1000;
    private static final String AUDIT_ENTITY = "SALES_RETURN";
    private static final String NOT_FOUND = "NOT_FOUND";

    private final ReturnDao returnDao = new ReturnDao();
    private final StockTransferDao stockDao = new StockTransferDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Quyền của người dùng với phiếu trả: salesRepId != null là chỉ đại lý do mình phụ trách
    public static final class Access {
        private final boolean allowed;
        private final boolean canCreate;
        private final boolean canReceive;
        private final Long salesRepId;

        Access(boolean allowed, boolean canCreate, boolean canReceive, Long salesRepId) {
            this.allowed = allowed;
            this.canCreate = canCreate;
            this.canReceive = canReceive;
            this.salesRepId = salesRepId;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public boolean isCanCreate() {
            return canCreate;
        }

        public boolean isCanReceive() {
            return canReceive;
        }

        public Long getSalesRepId() {
            return salesRepId;
        }
    }

    // error != null: lỗi nghiệp vụ (hoặc "NOT_FOUND"); ngược lại id và số phiếu trả
    public static final class Outcome {
        private final String error;
        private final long id;
        private final String returnNo;

        private Outcome(String error, long id, String returnNo) {
            this.error = error;
            this.id = id;
            this.returnNo = returnNo;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0, null);
        }

        static Outcome success(long id, String returnNo) {
            return new Outcome(null, id, returnNo);
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

        public String getReturnNo() {
            return returnNo;
        }
    }

    // Số lượng trả nhập ở form theo từng dòng hoá đơn
    public record Input(long invoiceItemId, String qtyText, String condition) {
    }

    public Access access(long userId) throws SQLException {
        String orderScope = permissionDao.findScope(userId, Permission.ORDER_MANAGE);
        if ("OWN".equals(orderScope)) {
            // Đại lý đặt hàng cho chính mình, không lập phiếu trả thay công ty
            orderScope = null;
        }
        boolean receive = permissionDao.findScope(userId, Permission.WAREHOUSE_MANAGE) != null;
        boolean accountant = "ALL".equals(permissionDao.findScope(userId, Permission.INVOICE_MANAGE));
        if (orderScope == null && !receive && !accountant) {
            return new Access(false, false, false, null);
        }
        boolean create = orderScope != null;
        Long rep = "ASSIGNED".equals(orderScope) && !receive && !accountant ? userId : null;
        return new Access(true, create, receive, rep);
    }

    public List<ReturnDao.InvoiceOption> invoiceOptions(Long salesRepId) throws SQLException {
        return returnDao.findInvoiceOptions(salesRepId, LIST_LIMIT);
    }

    public List<SalesReturn> findAll(Long salesRepId) throws SQLException {
        return returnDao.findAll(salesRepId, null, LIST_LIMIT);
    }

    public SalesReturn find(long id, Long salesRepId) throws SQLException {
        return returnDao.find(id, salesRepId);
    }

    // Dòng hoá đơn kèm số còn trả được; null nếu không có hoá đơn hoặc ngoài phạm vi
    public InvoiceLines lines(long invoiceId, Long salesRepId) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ReturnDao.InvoiceLock invoice = returnDao.lockInvoice(connection, invoiceId, salesRepId);
                if (invoice == null) {
                    return null;
                }
                return new InvoiceLines(invoice.invoiceNo(), invoice.status(),
                        returnDao.findLines(connection, invoiceId));
            } finally {
                connection.rollback();
            }
        }
    }

    public record InvoiceLines(String invoiceNo, String status, List<ReturnLine> lines) {
        public String getInvoiceNo() {
            return invoiceNo;
        }

        public List<ReturnLine> getLines() {
            return lines;
        }
    }

    // null nếu không hợp lệ (âm, 0, quá 3 số lẻ, không phải số)
    static BigDecimal parseQty(String text) {
        if (text == null || !text.trim().matches("\\d{1,9}([.,]\\d{1,3})?")) {
            return null;
        }
        BigDecimal value = new BigDecimal(text.trim().replace(',', '.'));
        return value.signum() > 0 ? value : null;
    }

    // Đổi số lượng theo đơn vị hoá đơn sang đơn vị cơ sở bằng hệ số đã chụp trên hoá đơn: phiếu trả đảo đúng giao
    // dịch đã bán nên không đọc lại hệ số hiện hành của SKU
    static BigDecimal toBase(BigDecimal qty, BigDecimal unitFactor) {
        return qty.multiply(unitFactor).setScale(3, RoundingMode.HALF_UP);
    }

    public Outcome create(long actorUserId, Long salesRepId, long invoiceId, String reasonCode, String reasonNote,
                          List<Input> inputs, String ipAddress) throws SQLException {
        if (!SalesReturn.isReason(reasonCode)) {
            return Outcome.failure("Chọn lý do trả hàng.");
        }
        String note = reasonNote == null || reasonNote.trim().isEmpty() ? null : reasonNote.trim();
        if ("OTHER".equals(reasonCode) && note == null) {
            return Outcome.failure("Chọn \"Lý do khác\" thì ghi rõ lý do ở ghi chú.");
        }
        if (note != null && note.length() > NOTE_MAX) {
            return Outcome.failure("Ghi chú tối đa " + NOTE_MAX + " ký tự.");
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Outcome outcome = createInTransaction(connection, actorUserId, salesRepId, invoiceId, reasonCode,
                        note, inputs, ipAddress);
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

    private Outcome createInTransaction(Connection connection, long actorUserId, Long salesRepId, long invoiceId,
                                        String reasonCode, String note, List<Input> inputs, String ipAddress)
            throws SQLException {
        ReturnDao.InvoiceLock invoice = returnDao.lockInvoice(connection, invoiceId, salesRepId);
        if (invoice == null) {
            return Outcome.failure(NOT_FOUND);
        }
        if ("CANCELLED".equals(invoice.status())) {
            return Outcome.failure("Hoá đơn đã huỷ nên không lập phiếu trả được.");
        }
        Map<Long, ReturnLine> byId = new LinkedHashMap<>();
        for (ReturnLine line : returnDao.findLines(connection, invoiceId)) {
            byId.put(line.getInvoiceItemId(), line);
        }
        Long defectWarehouse = returnDao.findDefectWarehouseId(connection);
        record Chosen(ReturnLine line, BigDecimal qty, BigDecimal qtyBase, String condition, long toWarehouse) {
        }
        List<Chosen> chosen = new java.util.ArrayList<>();
        for (Input input : inputs) {
            if (input.qtyText() == null || input.qtyText().trim().isEmpty()) {
                continue;
            }
            ReturnLine line = byId.get(input.invoiceItemId());
            if (line == null) {
                return Outcome.failure(NOT_FOUND);
            }
            BigDecimal qty = parseQty(input.qtyText());
            if (qty == null) {
                return Outcome.failure("Số lượng trả của " + line.getSku() + " không hợp lệ.");
            }
            BigDecimal qtyBase = toBase(qty, line.getUnitFactor());
            if (qtyBase.compareTo(line.getReturnableBase()) > 0) {
                return Outcome.failure("Số lượng trả của " + line.getSku() + " vượt số còn trả được ("
                        + line.getReturnable().stripTrailingZeros().toPlainString() + " " + line.getUnitName()
                        + ").");
            }
            String condition = SalesReturn.DAMAGED.equals(input.condition()) ? SalesReturn.DAMAGED : SalesReturn.GOOD;
            long toWarehouse = line.getWarehouseId();
            if (SalesReturn.DAMAGED.equals(condition)) {
                if (defectWarehouse == null) {
                    return Outcome.failure("Chưa có kho hàng lỗi để nhận hàng hỏng.");
                }
                toWarehouse = defectWarehouse;
            }
            chosen.add(new Chosen(line, qty, qtyBase, condition, toWarehouse));
        }
        if (chosen.isEmpty()) {
            return Outcome.failure("Nhập số lượng trả ít nhất một dòng hàng.");
        }
        String returnNo = returnDao.nextReturnNo(connection, DateTimeUtil.today().getYear());
        long id = returnDao.insert(connection, returnNo, invoiceId, invoice.customerId(),
                chosen.get(0).line().getWarehouseId(), reasonCode, note, actorUserId);
        for (Chosen item : chosen) {
            returnDao.insertItem(connection, id, item.line(), item.qty(), item.qtyBase(), item.condition(),
                    item.toWarehouse());
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("returnNo", returnNo);
        values.put("invoiceNo", invoice.invoiceNo());
        values.put("reason", reasonCode);
        values.put("lines", chosen.size());
        values.put("status", SalesReturn.SUBMITTED);
        auditLogDao.insert(connection, actorUserId, "RETURN_CREATE", AUDIT_ENTITY, id, null,
                JsonUtil.object(values), note, ipAddress);
        return Outcome.success(id, returnNo);
    }

    // Quản lý kho xác nhận đã nhận hàng: cộng tồn đúng lô vào kho nhận và ghi thẻ kho
    public Outcome receive(long actorUserId, long id, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ReturnDao.ReturnLock locked = returnDao.lock(connection, id);
                if (locked == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!SalesReturn.SUBMITTED.equals(locked.status())) {
                    connection.rollback();
                    return Outcome.failure("Phiếu trả " + locked.returnNo() + " không còn ở trạng thái Chờ nhận hàng.");
                }
                for (ReturnDao.ReceiveItem item : returnDao.findReceiveItems(connection, id)) {
                    stockDao.ensureBalance(connection, item.toWarehouseId(), item.productId());
                    BigDecimal[] balance = stockDao.lockBalance(connection, item.toWarehouseId(), item.productId());
                    BigDecimal onHand = balance[0].add(item.qtyBase());
                    stockDao.updateBalance(connection, item.toWarehouseId(), item.productId(), onHand);
                    BigDecimal lotOnHand = stockDao.lockLotBalance(connection, item.toWarehouseId(), item.lotId());
                    stockDao.updateLotBalance(connection, item.toWarehouseId(), item.lotId(),
                            lotOnHand.add(item.qtyBase()));
                    returnDao.insertMovement(connection, item.toWarehouseId(), item.productId(), item.lotId(),
                            item.qtyBase(), onHand, id, "Nhận hàng trả " + locked.returnNo(), actorUserId);
                }
                returnDao.markReceived(connection, id, actorUserId);
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("status", SalesReturn.SUBMITTED);
                Map<String, Object> after = new LinkedHashMap<>();
                after.put("returnNo", locked.returnNo());
                after.put("status", SalesReturn.RECEIVED);
                auditLogDao.insert(connection, actorUserId, "RETURN_RECEIVE", AUDIT_ENTITY, id,
                        JsonUtil.object(before), JsonUtil.object(after), null, ipAddress);
                connection.commit();
                return Outcome.success(id, locked.returnNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Huỷ phiếu chưa nhận hàng; số lượng được giải phóng cho phiếu khác
    public Outcome cancel(long actorUserId, Long salesRepId, long id, String ipAddress) throws SQLException {
        if (returnDao.find(id, salesRepId) == null) {
            return Outcome.failure(NOT_FOUND);
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ReturnDao.ReturnLock locked = returnDao.lock(connection, id);
                if (locked == null) {
                    connection.rollback();
                    return Outcome.failure(NOT_FOUND);
                }
                if (!SalesReturn.SUBMITTED.equals(locked.status())) {
                    connection.rollback();
                    return Outcome.failure("Chỉ huỷ được phiếu trả đang Chờ nhận hàng.");
                }
                returnDao.markCancelled(connection, id, actorUserId);
                Map<String, Object> before = new LinkedHashMap<>();
                before.put("status", SalesReturn.SUBMITTED);
                Map<String, Object> after = new LinkedHashMap<>();
                after.put("returnNo", locked.returnNo());
                after.put("status", SalesReturn.CANCELLED);
                auditLogDao.insert(connection, actorUserId, "RETURN_CANCEL", AUDIT_ENTITY, id,
                        JsonUtil.object(before), JsonUtil.object(after), null, ipAddress);
                connection.commit();
                return Outcome.success(id, locked.returnNo());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
