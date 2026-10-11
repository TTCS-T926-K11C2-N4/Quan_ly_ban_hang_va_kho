package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerAssignmentDao;
import com.oms.dao.CustomerProfileDao;
import com.oms.model.Customer;
import com.oms.model.CustomerForm;
import com.oms.model.CustomerProfile;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

// Hồ sơ đại lý (S3-03): mã đại lý duy nhất (không đổi sau khi tạo), tên, mã số thuế, nhóm khách hàng (quyết định bảng
// giá áp dụng khi tạo đơn, SalesOrderDao.findPricingRules), khu vực, người phụ trách, kho phục vụ, trạng thái.
// Đại lý đã phát sinh giao dịch không xoá được, chỉ ngừng giao dịch. Mọi thay đổi ghi nhật ký cùng transaction (S2-04).
public class CustomerProfileService {

    public static final String CODE_PREFIX = "DL";
    public static final int NAME_MAX_LENGTH = 200;
    public static final int ADDRESS_MAX_LENGTH = 300;
    public static final int EMAIL_MAX_LENGTH = 150;
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9._-]{2,30}");
    private static final Pattern TAX_CODE_PATTERN = Pattern.compile("\\d{10}(-\\d{3})?|\\d{12}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{10}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final String ENTITY = "CUSTOMER";

    // Kết quả bấm Xoá: đại lý chưa có giao dịch thì xoá hẳn, đã có thì chỉ ngừng giao dịch
    public enum RemoveResult { DELETED, DEACTIVATED }

    private final CustomerProfileDao profileDao = new CustomerProfileDao();
    private final CustomerAssignmentDao assignmentDao = new CustomerAssignmentDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public CustomerProfile find(long id) throws SQLException {
        return profileDao.findProfile(id);
    }

    public String suggestCode() throws SQLException {
        return CODE_PREFIX + String.format("%05d", profileDao.findMaxCodeNumber(CODE_PREFIX) + 1);
    }

    // Danh sách lựa chọn của form, để kiểm tra giá trị gửi lên và ghi tên vào nhật ký
    public record Options(List<SelectOption> groups, List<SelectOption> regions, List<SelectOption> warehouses,
                          List<SelectOption> activeReps) {
    }

    // values: giá trị để lưu; names: tên nhóm / khu vực / kho / người phụ trách để ghi nhật ký
    public record Checked(Map<String, String> errors, CustomerProfileDao.Values values, Map<String, String> names) {
        public boolean isValid() {
            return errors.isEmpty();
        }
    }

    // editing = null: thêm mới. canAssign: được chọn người phụ trách (S3-06); không có quyền thì người tạo là Nhân viên
    // kinh doanh (selfAssign) tự phụ trách, còn lại giữ nguyên người phụ trách hiện có.
    public Checked validate(CustomerForm form, CustomerProfile editing, boolean canAssign, Long selfAssignId,
                            Options options) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        String code = editing != null ? editing.getCode()
                : form.getCode() == null ? null : form.getCode().toUpperCase(Locale.ROOT);
        if (editing == null) {
            if (code == null) {
                errors.put("code", "Vui lòng nhập mã đại lý.");
            } else if (!CODE_PATTERN.matcher(code).matches()) {
                errors.put("code", "Mã đại lý 2–30 ký tự, gồm chữ không dấu, số, dấu chấm, gạch ngang, gạch dưới.");
            } else if (profileDao.existsCode(code, null)) {
                errors.put("code", "Mã đại lý " + code + " đã được dùng.");
            }
        }
        if (form.getName() == null) {
            errors.put("name", "Vui lòng nhập tên đại lý.");
        } else if (form.getName().length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên đại lý tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }
        if (form.getTaxCode() != null && !TAX_CODE_PATTERN.matcher(form.getTaxCode()).matches()) {
            errors.put("taxCode", "Mã số thuế gồm 10 số, 10 số kèm -3 số chi nhánh, hoặc 12 số.");
        }
        if (form.getPhone() != null && !PHONE_PATTERN.matcher(form.getPhone()).matches()) {
            errors.put("phone", "Số điện thoại gồm đúng 10 chữ số.");
        }
        if (form.getEmail() != null && (form.getEmail().length() > EMAIL_MAX_LENGTH
                || !EMAIL_PATTERN.matcher(form.getEmail()).matches())) {
            errors.put("email", "Email không hợp lệ.");
        }
        if (form.getAddress() != null && form.getAddress().length() > ADDRESS_MAX_LENGTH) {
            errors.put("address", "Địa chỉ tối đa " + ADDRESS_MAX_LENGTH + " ký tự.");
        }

        SelectOption group = pick(options.groups(), form.getCustomerGroupId());
        if (group == null) {
            errors.put("customerGroupId", "Chọn nhóm khách hàng.");
        }
        SelectOption region = pick(options.regions(), form.getRegionId());
        if (region == null) {
            errors.put("regionId", "Chọn khu vực.");
        }
        SelectOption warehouse = pick(options.warehouses(), form.getDefaultWarehouseId());
        if (warehouse == null) {
            errors.put("defaultWarehouseId", "Chọn kho phục vụ mặc định.");
        }

        Long salesRepId;
        String salesRepName;
        if (canAssign) {
            SelectOption rep = pick(options.activeReps(), form.getSalesRepId());
            boolean keepsCurrent = editing != null && editing.getSalesRepId() != null
                    && String.valueOf(editing.getSalesRepId()).equals(form.getSalesRepId());
            if (form.getSalesRepId() != null && rep == null && !keepsCurrent) {
                errors.put("salesRepId", "Người phụ trách phải là nhân viên kinh doanh đang hoạt động.");
            }
            salesRepId = keepsCurrent ? editing.getSalesRepId() : rep == null ? null : rep.getId();
            salesRepName = keepsCurrent ? editing.getSalesRepName() : rep == null ? null : rep.getName();
        } else if (editing != null) {
            salesRepId = editing.getSalesRepId();
            salesRepName = editing.getSalesRepName();
        } else {
            SelectOption self = selfAssignId == null ? null : find(options.activeReps(), selfAssignId);
            salesRepId = self == null ? null : self.getId();
            salesRepName = self == null ? null : self.getName();
        }

        String status = Customer.ACTIVE.equals(form.getStatus()) || "INACTIVE".equals(form.getStatus())
                ? form.getStatus() : null;
        if (status == null) {
            errors.put("status", "Chọn trạng thái.");
        }

        if (!errors.isEmpty()) {
            return new Checked(errors, null, Map.of());
        }
        Map<String, String> names = new LinkedHashMap<>();
        names.put("customerGroup", group.getName());
        names.put("region", region.getName());
        names.put("warehouse", warehouse.getName());
        names.put("salesRep", salesRepName);
        return new Checked(errors, new CustomerProfileDao.Values(code, form.getName(), form.getTaxCode(),
                form.getPhone(), form.getEmail(), form.getAddress(), group.getId(), region.getId(), salesRepId,
                warehouse.getId(), status), names);
    }

    // Gọi validate trước. Người phụ trách lúc tạo chỉ ghi vào nhật ký, không ghi lịch sử chuyển giao: lịch sử không xoá
    // được nên ghi lúc tạo sẽ làm đại lý nhập nhầm không bao giờ xoá được.
    public long create(Checked checked, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                CustomerProfileDao.Values values = checked.values();
                long id = profileDao.insert(connection, values, actorUserId);
                audit(connection, actorUserId, "CUSTOMER_CREATE", id, null, values(values, checked.names()), ipAddress);
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // false nếu người khác vừa sửa hồ sơ (version đã khác). Đổi người phụ trách thì ghi lịch sử chuyển giao.
    public boolean update(CustomerProfile old, long version, Checked checked, long actorUserId, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                CustomerProfileDao.Values values = checked.values();
                if (!profileDao.update(connection, old.getId(), version, values, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                if (values.salesRepId() != null && !Objects.equals(values.salesRepId(), old.getSalesRepId())) {
                    assignmentDao.insertHistory(connection, old.getId(), old.getSalesRepId(), values.salesRepId(),
                            "Đổi người phụ trách trong hồ sơ đại lý", actorUserId);
                }
                audit(connection, actorUserId, "CUSTOMER_UPDATE", old.getId(), values(old),
                        values(values, checked.names()), ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Ngừng giao dịch / giao dịch lại; false nếu đại lý đã ở trạng thái đó
    public boolean setActive(CustomerProfile profile, boolean active, long actorUserId, String ipAddress)
            throws SQLException {
        String status = active ? Customer.ACTIVE : "INACTIVE";
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!profileDao.updateStatus(connection, profile.getId(), status, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                audit(connection, actorUserId, active ? "CUSTOMER_ACTIVATE" : "CUSTOMER_DEACTIVATE", profile.getId(),
                        statusValues(profile.getCode(), profile.getStatus()), statusValues(profile.getCode(), status),
                        ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // S3-03 AC4: đại lý đã phát sinh giao dịch không xoá được, chỉ ngừng giao dịch
    public RemoveResult remove(CustomerProfile profile, long actorUserId, String ipAddress) throws SQLException {
        if (profile.isHasTransactions()) {
            setActive(profile, false, actorUserId, ipAddress);
            return RemoveResult.DEACTIVATED;
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                profileDao.delete(connection, profile.getId());
                audit(connection, actorUserId, "CUSTOMER_DELETE", profile.getId(), values(profile), null, ipAddress);
                connection.commit();
                return RemoveResult.DELETED;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void audit(Connection connection, long actorUserId, String action, long id, Map<String, Object> oldValues,
                       Map<String, Object> newValues, String ipAddress) throws SQLException {
        auditLogDao.insert(connection, actorUserId, action, ENTITY, id,
                oldValues == null ? null : JsonUtil.object(oldValues),
                newValues == null ? null : JsonUtil.object(newValues), null, ipAddress);
    }

    static Map<String, Object> values(CustomerProfileDao.Values values, Map<String, String> names) {
        return values(values.code(), values.name(), values.taxCode(), values.phone(), values.email(), values.address(),
                names.get("customerGroup"), names.get("region"), names.get("salesRep"), names.get("warehouse"),
                values.status());
    }

    static Map<String, Object> values(CustomerProfile profile) {
        return values(profile.getCode(), profile.getName(), profile.getTaxCode(), profile.getPhone(),
                profile.getEmail(), profile.getAddress(), profile.getCustomerGroupName(), profile.getRegionName(),
                profile.getSalesRepName(), profile.getDefaultWarehouseName(), profile.getStatus());
    }

    // Ghi tên (nhóm, khu vực, người phụ trách, kho) thay vì mã id để trang Nhật ký đọc được ngay
    private static Map<String, Object> values(String code, String name, String taxCode, String phone, String email,
                                              String address, String customerGroup, String region, String salesRep,
                                              String warehouse, String status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("name", name);
        map.put("taxCode", taxCode);
        map.put("phone", phone);
        map.put("email", email);
        map.put("address", address);
        map.put("customerGroup", customerGroup);
        map.put("region", region);
        map.put("salesRep", salesRep);
        map.put("warehouse", warehouse);
        map.put("status", status);
        return map;
    }

    private static Map<String, Object> statusValues(String code, String status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("status", status);
        return map;
    }

    private static SelectOption pick(List<SelectOption> options, String id) {
        try {
            return id == null ? null : find(options, Long.parseLong(id));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static SelectOption find(List<SelectOption> options, long id) {
        return options.stream().filter(option -> option.getId() == id).findFirst().orElse(null);
    }
}
