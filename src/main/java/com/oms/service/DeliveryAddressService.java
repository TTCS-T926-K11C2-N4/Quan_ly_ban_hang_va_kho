package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.DeliveryAddressDao;
import com.oms.model.Customer;
import com.oms.model.DeliveryAddressEntry;
import com.oms.model.DeliveryAddressForm;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Điểm giao hàng của đại lý (S3-04): mỗi điểm có địa chỉ, người nhận, số điện thoại, ghi chú đường đi; luôn có đúng
// một điểm mặc định trong các điểm đang dùng (điểm đầu tiên tự là mặc định). Đơn hàng (S3-09) chỉ chọn được điểm
// đang dùng của đúng đại lý đó. Điểm đã có đơn không xoá được (đơn cũ phải giữ địa chỉ), chỉ ngừng dùng.
// Mọi thay đổi ghi nhật ký thao tác trong cùng transaction (S2-04).
public class DeliveryAddressService {

    public static final int LABEL_MAX_LENGTH = 100;
    public static final int ADDRESS_MAX_LENGTH = 300;
    public static final int RECEIVER_MAX_LENGTH = 150;
    public static final int ROUTE_NOTE_MAX_LENGTH = 500;
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{10}");
    private static final String ENTITY = "DELIVERY_ADDRESS";

    // Kết quả bấm thùng rác: điểm chưa có đơn thì xoá hẳn, đã có đơn thì chỉ ngừng dùng
    public enum RemoveResult { DELETED, DEACTIVATED }

    private final DeliveryAddressDao deliveryAddressDao = new DeliveryAddressDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public List<DeliveryAddressEntry> findEntries(long customerId) throws SQLException {
        return deliveryAddressDao.findEntries(customerId);
    }

    // null nếu không có điểm giao này hoặc điểm thuộc đại lý khác
    public DeliveryAddressEntry findEntry(long customerId, long id) throws SQLException {
        DeliveryAddressEntry entry = deliveryAddressDao.findEntry(id);
        return entry == null || entry.getCustomerId() != customerId ? null : entry;
    }

    // Lỗi theo tên ô; rỗng nghĩa là hợp lệ. Form đã được trim, số điện thoại đã bỏ dấu cách/chấm/gạch.
    public Map<String, String> validate(DeliveryAddressForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getLabel() != null && form.getLabel().length() > LABEL_MAX_LENGTH) {
            errors.put("label", "Tên điểm giao tối đa " + LABEL_MAX_LENGTH + " ký tự.");
        }
        if (form.getAddress() == null) {
            errors.put("address", "Vui lòng nhập địa chỉ giao hàng.");
        } else if (form.getAddress().length() > ADDRESS_MAX_LENGTH) {
            errors.put("address", "Địa chỉ tối đa " + ADDRESS_MAX_LENGTH + " ký tự.");
        }
        if (form.getReceiverName() == null) {
            errors.put("receiverName", "Vui lòng nhập tên người nhận hàng.");
        } else if (form.getReceiverName().length() > RECEIVER_MAX_LENGTH) {
            errors.put("receiverName", "Tên người nhận tối đa " + RECEIVER_MAX_LENGTH + " ký tự.");
        }
        if (form.getReceiverPhone() == null) {
            errors.put("receiverPhone", "Vui lòng nhập số điện thoại người nhận.");
        } else if (!PHONE_PATTERN.matcher(form.getReceiverPhone()).matches()) {
            errors.put("receiverPhone", "Số điện thoại gồm đúng 10 chữ số.");
        }
        if (form.getRouteNote() != null && form.getRouteNote().length() > ROUTE_NOTE_MAX_LENGTH) {
            errors.put("routeNote", "Ghi chú đường đi tối đa " + ROUTE_NOTE_MAX_LENGTH + " ký tự.");
        }
        return errors;
    }

    // Gọi validate trước. Đại lý chưa có điểm mặc định đang dùng thì điểm mới tự là mặc định.
    public long create(Customer customer, DeliveryAddressForm form, long actorUserId, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean hasDefault = deliveryAddressDao.hasActiveDefault(connection, customer.getId());
                boolean isDefault = form.isMakeDefault() || !hasDefault;
                if (isDefault && hasDefault) {
                    deliveryAddressDao.clearDefault(connection, customer.getId(), actorUserId);
                }
                long id = deliveryAddressDao.insert(connection, customer.getId(), form, isDefault, actorUserId);
                audit(connection, actorUserId, "DELIVERY_ADDRESS_CREATE", id, null,
                        values(customer.getCode(), form, isDefault, true), ipAddress);
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Gọi validate trước. Tích "Đặt làm mặc định" khi sửa thì chuyển mặc định sang điểm này (nếu đang dùng).
    public void update(Customer customer, DeliveryAddressEntry entry, DeliveryAddressForm form, long actorUserId,
                       String ipAddress) throws SQLException {
        boolean becomeDefault = form.isMakeDefault() && entry.isActive() && !entry.isDefaultAddress();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                deliveryAddressDao.update(connection, entry.getId(), form, actorUserId);
                if (becomeDefault) {
                    deliveryAddressDao.clearDefault(connection, customer.getId(), actorUserId);
                    deliveryAddressDao.setDefault(connection, entry.getId(), actorUserId);
                }
                audit(connection, actorUserId, "DELIVERY_ADDRESS_UPDATE", entry.getId(),
                        values(customer.getCode(), entry), values(customer.getCode(), form,
                                entry.isDefaultAddress() || becomeDefault, entry.isActive()), ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // false nếu điểm đã ngừng dùng (chỉ điểm đang dùng mới làm mặc định được)
    public boolean setDefault(Customer customer, DeliveryAddressEntry entry, long actorUserId, String ipAddress)
            throws SQLException {
        if (!entry.isActive()) {
            return false;
        }
        if (entry.isDefaultAddress()) {
            return true;
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                deliveryAddressDao.clearDefault(connection, customer.getId(), actorUserId);
                deliveryAddressDao.setDefault(connection, entry.getId(), actorUserId);
                audit(connection, actorUserId, "DELIVERY_ADDRESS_SET_DEFAULT", entry.getId(),
                        defaultValues(customer.getCode(), entry, false), defaultValues(customer.getCode(), entry, true),
                        ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Xoá điểm mặc định / ngừng dùng thì mặc định chuyển sang điểm đang dùng còn lại đầu tiên
    public RemoveResult remove(Customer customer, DeliveryAddressEntry entry, long actorUserId, String ipAddress)
            throws SQLException {
        RemoveResult result = entry.isUsed() ? RemoveResult.DEACTIVATED : RemoveResult.DELETED;
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (result == RemoveResult.DELETED) {
                    deliveryAddressDao.delete(connection, entry.getId());
                    audit(connection, actorUserId, "DELIVERY_ADDRESS_DELETE", entry.getId(),
                            values(customer.getCode(), entry), null, ipAddress);
                } else if (entry.isActive()) {
                    deliveryAddressDao.setActive(connection, entry.getId(), false, actorUserId);
                    audit(connection, actorUserId, "DELIVERY_ADDRESS_DEACTIVATE", entry.getId(),
                            statusValues(customer.getCode(), entry, entry.isDefaultAddress(), true),
                            statusValues(customer.getCode(), entry, false, false), ipAddress);
                }
                if (entry.isDefaultAddress()) {
                    moveDefault(connection, customer, entry.getId(), actorUserId, ipAddress);
                }
                connection.commit();
                return result;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Dùng lại điểm đã ngừng; đại lý đang không có điểm mặc định thì điểm này thành mặc định
    public void activate(Customer customer, DeliveryAddressEntry entry, long actorUserId, String ipAddress)
            throws SQLException {
        if (entry.isActive()) {
            return;
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean becomeDefault = !deliveryAddressDao.hasActiveDefault(connection, customer.getId());
                deliveryAddressDao.setActive(connection, entry.getId(), true, actorUserId);
                if (becomeDefault) {
                    deliveryAddressDao.setDefault(connection, entry.getId(), actorUserId);
                }
                audit(connection, actorUserId, "DELIVERY_ADDRESS_ACTIVATE", entry.getId(),
                        statusValues(customer.getCode(), entry, false, false),
                        statusValues(customer.getCode(), entry, becomeDefault, true), ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void moveDefault(Connection connection, Customer customer, long removedId, long actorUserId,
                             String ipAddress) throws SQLException {
        Long nextId = deliveryAddressDao.findFirstActiveId(connection, customer.getId(), removedId);
        if (nextId == null) {
            return;
        }
        deliveryAddressDao.setDefault(connection, nextId, actorUserId);
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("code", customer.getCode());
        before.put("isDefault", false);
        Map<String, Object> after = new LinkedHashMap<>(before);
        after.put("isDefault", true);
        audit(connection, actorUserId, "DELIVERY_ADDRESS_SET_DEFAULT", nextId, before, after, ipAddress);
    }

    private void audit(Connection connection, long actorUserId, String action, long id, Map<String, Object> oldValues,
                       Map<String, Object> newValues, String ipAddress) throws SQLException {
        auditLogDao.insert(connection, actorUserId, action, ENTITY, id,
                oldValues == null ? null : JsonUtil.object(oldValues),
                newValues == null ? null : JsonUtil.object(newValues), null, ipAddress);
    }

    static Map<String, Object> values(String customerCode, DeliveryAddressForm form, boolean isDefault,
                                      boolean active) {
        return values(customerCode, form.getLabel(), form.getAddress(), form.getReceiverName(),
                form.getReceiverPhone(), form.getRouteNote(), isDefault, active);
    }

    static Map<String, Object> values(String customerCode, DeliveryAddressEntry entry) {
        return values(customerCode, entry.getLabel(), entry.getAddress(), entry.getReceiverName(),
                entry.getReceiverPhone(), entry.getRouteNote(), entry.isDefaultAddress(), entry.isActive());
    }

    private static Map<String, Object> values(String customerCode, String label, String address, String receiverName,
                                              String receiverPhone, String routeNote, boolean isDefault,
                                              boolean active) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", customerCode);
        values.put("label", label);
        values.put("address", address);
        values.put("receiverName", receiverName);
        values.put("receiverPhone", receiverPhone);
        values.put("routeNote", routeNote);
        values.put("isDefault", isDefault);
        values.put("active", active);
        return values;
    }

    private static Map<String, Object> defaultValues(String customerCode, DeliveryAddressEntry entry,
                                                     boolean isDefault) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", customerCode);
        values.put("address", entry.getAddress());
        values.put("isDefault", isDefault);
        return values;
    }

    private static Map<String, Object> statusValues(String customerCode, DeliveryAddressEntry entry, boolean isDefault,
                                                    boolean active) {
        Map<String, Object> values = defaultValues(customerCode, entry, isDefault);
        values.put("active", active);
        return values;
    }
}
