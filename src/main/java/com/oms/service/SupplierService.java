package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.SupplierDao;
import com.oms.model.PageResult;
import com.oms.model.Supplier;
import com.oms.model.SupplierForm;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Danh mục nhà cung cấp (S2-09): thêm, sửa, tìm; đã có phiếu nhập thì không xoá, chỉ ngừng giao dịch
public class SupplierService {

    public static final int PAGE_SIZE = 10;

    // Ô chọn trên form (theo thiết kế); lưu nguyên nhãn vào payment_terms
    public static final List<String> PAYMENT_TERMS = List.of(
            "Thanh toán ngay", "TT 7 ngày", "TT 15 ngày", "TT 30 ngày", "TT 45 ngày", "TT 60 ngày", "TT 90 ngày");

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_-]{2,30}");
    // 10 chữ số, hoặc 10 chữ số + mã chi nhánh 3 chữ số (0101234567-001)
    private static final Pattern TAX_CODE_PATTERN = Pattern.compile("\\d{10}(-\\d{3})?");
    private static final int NAME_MAX_LENGTH = 200;
    private static final int CONTACT_MAX_LENGTH = 150;
    private static final String ENTITY = "SUPPLIER";

    private final SupplierDao supplierDao = new SupplierDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public PageResult<Supplier> search(String keyword, int requestedPage) throws SQLException {
        long total = supplierDao.count(keyword);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        return new PageResult<>(supplierDao.findPage(keyword, (page - 1) * PAGE_SIZE, PAGE_SIZE), page, PAGE_SIZE, total);
    }

    public Supplier find(long id) throws SQLException {
        return supplierDao.findById(id);
    }

    // Lỗi theo tên ô; rỗng nghĩa là hợp lệ. editing = null khi thêm mới.
    public Map<String, String> validate(SupplierForm form, Supplier editing) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        Long editingId = editing == null ? null : editing.getId();

        if (form.getCode() == null) {
            errors.put("code", "Vui lòng nhập mã nhà cung cấp.");
        } else if (!CODE_PATTERN.matcher(form.getCode()).matches()) {
            errors.put("code", "Mã gồm 2–30 ký tự: chữ không dấu, số, gạch dưới hoặc gạch ngang.");
        } else {
            String usedBy = supplierDao.findNameUsing("code", form.getCode(), editingId);
            if (usedBy != null) {
                errors.put("code", "Mã đã dùng cho nhà cung cấp \"" + usedBy + "\".");
            }
        }

        if (form.getName() == null) {
            errors.put("name", "Vui lòng nhập tên nhà cung cấp.");
        } else if (form.getName().length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên nhà cung cấp tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }

        if (form.getTaxCode() == null) {
            errors.put("taxCode", "Vui lòng nhập mã số thuế.");
        } else if (!TAX_CODE_PATTERN.matcher(form.getTaxCode()).matches()) {
            errors.put("taxCode", "Mã số thuế gồm 10 chữ số, hoặc thêm mã chi nhánh 3 chữ số (vd 0101234567-001).");
        } else {
            String usedBy = supplierDao.findNameUsing("tax_code", form.getTaxCode(), editingId);
            if (usedBy != null) {
                errors.put("taxCode", "Mã số thuế đã dùng cho nhà cung cấp \"" + usedBy + "\".");
            }
        }

        if (form.getContactName() == null) {
            errors.put("contactName", "Vui lòng nhập người liên hệ.");
        } else if (form.getContactName().length() > CONTACT_MAX_LENGTH) {
            errors.put("contactName", "Người liên hệ tối đa " + CONTACT_MAX_LENGTH + " ký tự.");
        }

        // Giữ được điều khoản cũ ngoài danh sách (dữ liệu nhập trước khi có ô chọn) nếu không đổi
        boolean unchangedTerms = editing != null && form.getPaymentTerms() != null
                && form.getPaymentTerms().equals(editing.getPaymentTerms());
        if (form.getPaymentTerms() == null) {
            errors.put("paymentTerms", "Vui lòng chọn điều khoản thanh toán.");
        } else if (!PAYMENT_TERMS.contains(form.getPaymentTerms()) && !unchangedTerms) {
            errors.put("paymentTerms", "Điều khoản thanh toán không hợp lệ.");
        }

        if (!Supplier.ACTIVE.equals(form.getStatus()) && !Supplier.INACTIVE.equals(form.getStatus())) {
            errors.put("status", "Vui lòng chọn trạng thái.");
        }
        return errors;
    }

    // Gọi validate trước
    public long create(SupplierForm form, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long id = supplierDao.insert(connection, form, actorUserId);
                auditLogDao.insert(connection, actorUserId, "SUPPLIER_CREATE", ENTITY, id, null,
                        JsonUtil.object(toValues(form)), null, ipAddress);
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Gọi validate trước. Trả về false nếu nhà cung cấp vừa bị xoá.
    public boolean update(Supplier editing, SupplierForm form, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!supplierDao.update(connection, editing.getId(), form, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                auditLogDao.insert(connection, actorUserId, "SUPPLIER_UPDATE", ENTITY, editing.getId(),
                        JsonUtil.object(toValues(SupplierForm.of(editing))), JsonUtil.object(toValues(form)), null,
                        ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Ngừng giao dịch / giao dịch lại; false nếu đã ở trạng thái đó
    public boolean changeStatus(Supplier supplier, String status, long actorUserId, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!supplierDao.updateStatus(connection, supplier.getId(), status, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                String action = Supplier.ACTIVE.equals(status) ? "SUPPLIER_ACTIVATE" : "SUPPLIER_DEACTIVATE";
                auditLogDao.insert(connection, actorUserId, action, ENTITY, supplier.getId(),
                        JsonUtil.object(Map.of("code", supplier.getCode(), "status", supplier.getStatus())),
                        JsonUtil.object(Map.of("code", supplier.getCode(), "status", status)), null, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // null nếu xoá được; ngược lại là lý do hiện cho người dùng (AC2)
    public String validateDelete(Supplier supplier) {
        return supplier.isHasReceipts()
                ? "Nhà cung cấp \"" + supplier.getName() + "\" đã có phiếu nhập nên không xoá được, chỉ ngừng giao dịch."
                : null;
    }

    // Gọi validateDelete trước. Ném SQLIntegrityConstraintViolationException nếu vừa có phiếu nhập gắn vào.
    public boolean delete(Supplier supplier, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!supplierDao.delete(connection, supplier.getId())) {
                    connection.rollback();
                    return false;
                }
                auditLogDao.insert(connection, actorUserId, "SUPPLIER_DELETE", ENTITY, supplier.getId(),
                        JsonUtil.object(toValues(SupplierForm.of(supplier))), null, null, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static Map<String, Object> toValues(SupplierForm form) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", form.getCode());
        values.put("name", form.getName());
        values.put("taxCode", form.getTaxCode());
        values.put("contactName", form.getContactName());
        values.put("paymentTerms", form.getPaymentTerms());
        values.put("status", form.getStatus());
        return values;
    }
}
