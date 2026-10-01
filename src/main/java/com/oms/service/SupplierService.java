package com.oms.service;

import com.oms.dao.SupplierDao;
import com.oms.model.PageResult;
import com.oms.model.Supplier;
import com.oms.model.SupplierFilter;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Danh mục nhà cung cấp (S2-09)
public class SupplierService {

    public static final int PAGE_SIZE = 20;

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9._-]{1,30}");
    // Mã số thuế Việt Nam: 10 số, hoặc 10 số kèm mã đơn vị phụ thuộc "-001"
    private static final Pattern TAX_CODE_PATTERN = Pattern.compile("\\d{10}(-\\d{3})?");
    // Nhà cung cấp có thể dùng số bàn hoặc số nước ngoài nên chỉ kiểm tra là dãy số hợp lý
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d{8,15}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final int NAME_MAX_LENGTH = 200;
    private static final int CONTACT_NAME_MAX_LENGTH = 150;
    private static final int EMAIL_MAX_LENGTH = 150;
    private static final int ADDRESS_MAX_LENGTH = 300;
    private static final int PAYMENT_TERMS_MAX_LENGTH = 200;

    private final SupplierDao supplierDao = new SupplierDao();

    public PageResult<Supplier> search(SupplierFilter filter, int requestedPage) throws SQLException {
        long total = supplierDao.count(filter);
        int totalPages = Math.max(1, (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE));
        // Trang vượt quá (vd sau khi lọc bớt kết quả) thì đưa về trang cuối
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        List<Supplier> items = supplierDao.findPage(filter, (page - 1) * PAGE_SIZE, PAGE_SIZE);
        return new PageResult<>(items, page, PAGE_SIZE, total);
    }

    public Supplier findById(long id) throws SQLException {
        return supplierDao.findById(id);
    }

    // Trả về lỗi theo tên ô (name trong form); rỗng nghĩa là hợp lệ
    public Map<String, String> validate(Supplier supplier) throws SQLException {
        Map<String, String> errors = validateFields(supplier);
        if (!errors.containsKey("code") && supplierDao.existsByCode(supplier.getCode(), supplier.getId())) {
            errors.put("code", "Mã nhà cung cấp đã tồn tại.");
        }
        return errors;
    }

    // Kiểm tra định dạng, không cần DB
    public Map<String, String> validateFields(Supplier supplier) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (supplier.getCode() == null) {
            errors.put("code", "Vui lòng nhập mã nhà cung cấp.");
        } else if (!CODE_PATTERN.matcher(supplier.getCode()).matches()) {
            errors.put("code", "Mã nhà cung cấp gồm tối đa 30 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        }

        if (supplier.getName() == null) {
            errors.put("name", "Vui lòng nhập tên nhà cung cấp.");
        } else if (supplier.getName().length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên nhà cung cấp tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }

        if (supplier.getTaxCode() != null && !TAX_CODE_PATTERN.matcher(supplier.getTaxCode()).matches()) {
            errors.put("taxCode", "Mã số thuế gồm 10 số, hoặc 10 số kèm mã đơn vị phụ thuộc (vd 0101234567-001).");
        }
        checkMaxLength(supplier.getContactName(), CONTACT_NAME_MAX_LENGTH, "contactName", "Người liên hệ", errors);
        if (supplier.getPhone() != null && !PHONE_PATTERN.matcher(supplier.getPhone()).matches()) {
            errors.put("phone", "Số điện thoại gồm 8–15 chữ số.");
        }
        if (supplier.getEmail() != null && (supplier.getEmail().length() > EMAIL_MAX_LENGTH
                || !EMAIL_PATTERN.matcher(supplier.getEmail()).matches())) {
            errors.put("email", "Email không đúng định dạng.");
        }
        checkMaxLength(supplier.getAddress(), ADDRESS_MAX_LENGTH, "address", "Địa chỉ", errors);
        checkMaxLength(supplier.getPaymentTerms(), PAYMENT_TERMS_MAX_LENGTH, "paymentTerms", "Điều khoản thanh toán",
                errors);
        return errors;
    }

    public void create(Supplier supplier, long actorUserId) throws SQLException {
        supplierDao.insert(supplier, actorUserId);
    }

    public void update(Supplier supplier, long actorUserId) throws SQLException {
        supplierDao.update(supplier, actorUserId);
    }

    // active = false: ngừng giao dịch; true: giao dịch lại. Trả về false nếu đã ở trạng thái đó.
    public boolean changeStatus(long id, boolean active, long actorUserId) throws SQLException {
        return supplierDao.updateStatus(id, active ? Supplier.ACTIVE : Supplier.INACTIVE, actorUserId);
    }

    // Trả về false nếu nhà cung cấp đã có phiếu nhập (chỉ được ngừng giao dịch)
    public boolean delete(long id) throws SQLException {
        try {
            return supplierDao.deleteIfNoReceipts(id);
        } catch (SQLIntegrityConstraintViolationException e) {
            // Bảng khác (thêm sau này) tham chiếu nhà cung cấp: khoá ngoại chặn xoá
            return false;
        }
    }

    private static void checkMaxLength(String value, int maxLength, String field, String label,
                                       Map<String, String> errors) {
        if (value != null && value.length() > maxLength) {
            errors.put(field, label + " tối đa " + maxLength + " ký tự.");
        }
    }
}
