package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.CreditNoteDao;
import com.oms.dao.PaymentDao;
import com.oms.model.CreditNote;
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

// Chứng từ điều chỉnh giảm trừ công nợ khi trả hàng (S8-02).
//  * Chỉ lập từ phiếu trả đã được Quản lý kho xác nhận nhận hàng; mỗi phiếu trả một chứng từ, giữ liên kết hoá đơn gốc.
//  * Giảm trừ theo ĐÚNG giá đã bán trên hoá đơn gốc (thành tiền dòng chia số lượng cơ sở), không phải giá hiện hành.
//  * Ghi Có vào sổ công nợ, cập nhật số dư và cờ công nợ cùng transaction; hoá đơn gốc không bị sửa nội dung, chỉ
//    cộng số đã giảm trừ (tối đa số còn nợ của hoá đơn, phần dư là dư có của đại lý trên sổ).
public class CreditNoteService {

    public static final int LIST_LIMIT = 100;
    private static final String AUDIT_ENTITY = "CREDIT_NOTE";
    private static final String NOT_FOUND = "NOT_FOUND";

    private final CreditNoteDao creditNoteDao = new CreditNoteDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();
    private final CreditFlagService creditFlagService = new CreditFlagService();
    private final OrderClosureService orderClosureService = new OrderClosureService();

    // error != null: lỗi nghiệp vụ (hoặc "NOT_FOUND"); ngược lại id và số chứng từ
    public static final class Outcome {
        private final String error;
        private final long id;
        private final String creditNoteNo;

        private Outcome(String error, long id, String creditNoteNo) {
            this.error = error;
            this.id = id;
            this.creditNoteNo = creditNoteNo;
        }

        static Outcome failure(String error) {
            return new Outcome(error, 0, null);
        }

        static Outcome success(long id, String creditNoteNo) {
            return new Outcome(null, id, creditNoteNo);
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

        public String getCreditNoteNo() {
            return creditNoteNo;
        }
    }

    public List<CreditNote> findAll() throws SQLException {
        return creditNoteDao.findAll(LIST_LIMIT);
    }

    public CreditNote find(long id) throws SQLException {
        return creditNoteDao.find(id);
    }

    public Long findByReturn(long returnId) throws SQLException {
        return creditNoteDao.findIdByReturn(returnId);
    }

    // Giá bình quân trên một đơn vị cơ sở theo dòng hoá đơn gốc, 2 số lẻ (chỉ để hiển thị)
    static BigDecimal unitPrice(BigDecimal lineTotal, BigDecimal invoiceQtyBase) {
        return lineTotal.divide(invoiceQtyBase, 2, RoundingMode.HALF_UP);
    }

    // Số tiền giảm trừ của dòng trả: số lượng trả / số lượng trên hoá đơn x thành tiền dòng, làm tròn tới đồng
    static BigDecimal amount(BigDecimal qtyBase, BigDecimal invoiceQtyBase, BigDecimal lineTotal) {
        return qtyBase.multiply(lineTotal).divide(invoiceQtyBase, 0, RoundingMode.HALF_UP).setScale(2);
    }

    public Outcome issue(long actorUserId, long returnId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Outcome outcome = issueInTransaction(connection, actorUserId, returnId, ipAddress);
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

    private Outcome issueInTransaction(Connection connection, long actorUserId, long returnId, String ipAddress)
            throws SQLException {
        CreditNoteDao.ReturnForCredit sourceReturn = creditNoteDao.lockReturn(connection, returnId);
        if (sourceReturn == null) {
            return Outcome.failure(NOT_FOUND);
        }
        if (!"RECEIVED".equals(sourceReturn.status())) {
            return Outcome.failure("Chỉ lập chứng từ điều chỉnh từ phiếu trả đã nhận hàng.");
        }
        if (sourceReturn.hasCreditNote()) {
            return Outcome.failure("Phiếu trả " + sourceReturn.returnNo() + " đã có chứng từ điều chỉnh.");
        }
        creditNoteDao.lockInvoice(connection, sourceReturn.invoiceId());
        List<CreditNoteDao.Line> lines = creditNoteDao.findLines(connection, returnId);
        BigDecimal total = BigDecimal.ZERO;
        for (CreditNoteDao.Line line : lines) {
            total = total.add(amount(line.qtyBase(), line.invoiceQtyBase(), line.invoiceLineTotal()));
        }
        if (total.signum() <= 0) {
            return Outcome.failure("Phiếu trả không có giá trị để giảm trừ.");
        }
        String no = creditNoteDao.nextCreditNoteNo(connection, DateTimeUtil.today().getYear());
        String reason = "Trả hàng " + sourceReturn.returnNo() + " theo hoá đơn " + sourceReturn.invoiceNo();
        long id = creditNoteDao.insert(connection, no, sourceReturn.invoiceId(), returnId, sourceReturn.customerId(),
                total, reason, actorUserId);
        for (CreditNoteDao.Line line : lines) {
            creditNoteDao.insertItem(connection, id, line,
                    unitPrice(line.invoiceLineTotal(), line.invoiceQtyBase()),
                    amount(line.qtyBase(), line.invoiceQtyBase(), line.invoiceLineTotal()));
        }
        BigDecimal applied = total.min(sourceReturn.invoiceOutstanding().max(BigDecimal.ZERO));
        if (applied.signum() > 0) {
            creditNoteDao.applyCredit(connection, sourceReturn.invoiceId(), applied, actorUserId);
        }
        creditNoteDao.insertLedgerEntry(connection, sourceReturn.customerId(), total, id, DateTimeUtil.today(),
                "Giảm trừ " + no + " (" + reason + ")", actorUserId);
        PaymentDao.adjustBalance(connection, sourceReturn.customerId(), total.negate());
        creditFlagService.evaluate(connection, sourceReturn.customerId(), actorUserId, ipAddress);
        orderClosureService.closeSettled(connection, sourceReturn.customerId(), actorUserId, ipAddress);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("creditNoteNo", no);
        values.put("invoiceNo", sourceReturn.invoiceNo());
        values.put("returnNo", sourceReturn.returnNo());
        values.put("total", total.stripTrailingZeros().toPlainString());
        values.put("appliedToInvoice", applied.stripTrailingZeros().toPlainString());
        auditLogDao.insert(connection, actorUserId, "CREDIT_NOTE_ISSUE", AUDIT_ENTITY, id, null,
                JsonUtil.object(values), reason, ipAddress);
        return Outcome.success(id, no);
    }
}
