package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.UnitDao;
import com.oms.model.UnitRow;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Danh mục đơn vị tính (S2-07): mã và tên không trùng; đơn vị đang được dùng thì không xoá. Mọi thay đổi ghi audit_logs.
public class UnitService {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_-]{1,20}");
    private static final int NAME_MAX_LENGTH = 50;
    private static final String ENTITY = "UNIT";

    private final UnitDao unitDao = new UnitDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public List<UnitRow> getUnits() throws SQLException {
        return unitDao.findAllWithUsage();
    }

    public UnitRow find(long id) throws SQLException {
        return unitDao.findById(id);
    }

    // Lỗi theo tên ô (code, name); rỗng = hợp lệ. editing = null khi thêm mới.
    public Map<String, String> validate(String code, String name, UnitRow editing) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        Long editingId = editing == null ? null : editing.getId();
        if (code == null) {
            errors.put("code", "Vui lòng nhập mã đơn vị.");
        } else if (!CODE_PATTERN.matcher(code).matches()) {
            errors.put("code", "Mã đơn vị gồm 1–20 ký tự: chữ không dấu, số, gạch dưới hoặc gạch ngang.");
        } else if (unitDao.existsValue("code", code, editingId)) {
            errors.put("code", "Mã đơn vị đã tồn tại.");
        }
        if (name == null) {
            errors.put("name", "Vui lòng nhập tên đơn vị.");
        } else if (name.length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên đơn vị tối đa " + NAME_MAX_LENGTH + " ký tự.");
        } else if (unitDao.existsValue("name", name, editingId)) {
            errors.put("name", "Đã có đơn vị tên này.");
        }
        return errors;
    }

    public long create(String code, String name, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long id = unitDao.insert(connection, code, name, actorUserId);
                auditLogDao.insert(connection, actorUserId, "UNIT_CREATE", ENTITY, id, null, toJson(code, name), null,
                        ipAddress);
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Đổi tên/mã không ảnh hưởng số liệu: sản phẩm, bảng giá, chứng từ tham chiếu theo id
    public void update(UnitRow editing, String code, String name, long actorUserId, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                unitDao.update(connection, editing.getId(), code, name, actorUserId);
                auditLogDao.insert(connection, actorUserId, "UNIT_UPDATE", ENTITY, editing.getId(),
                        toJson(editing.getCode(), editing.getName()), toJson(code, name), null, ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // null nếu xoá được; ngược lại là lý do hiện cho người dùng
    public String validateDelete(UnitRow unit) {
        return unit.getProductCount() > 0
                ? "Đơn vị \"" + unit.getName() + "\" đang được " + unit.getProductCount()
                        + " sản phẩm dùng nên không xoá được."
                : null;
    }

    // Ném SQLIntegrityConstraintViolationException nếu bảng giá/chứng từ vẫn tham chiếu đơn vị
    public void delete(UnitRow unit, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                unitDao.delete(connection, unit.getId());
                auditLogDao.insert(connection, actorUserId, "UNIT_DELETE", ENTITY, unit.getId(),
                        toJson(unit.getCode(), unit.getName()), null, null, ipAddress);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static String toJson(String code, String name) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("name", name);
        return JsonUtil.object(values);
    }
}
