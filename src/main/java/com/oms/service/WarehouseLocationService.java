package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.WarehouseLocationDao;
import com.oms.model.Warehouse;
import com.oms.model.WarehouseLocation;
import com.oms.model.WarehouseLocationForm;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Vị trí lưu trong kho ở mức khu hoặc kệ (S5-03), hiện thành cây Khu > Kệ. Vị trí đã dùng thì không xoá, chỉ ngừng dùng.
public class WarehouseLocationService {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9._-]{0,29}");
    private static final int NAME_MAX_LENGTH = 100;
    private static final int PICK_SEQUENCE_MAX = 99999;
    private static final String ENTITY = "WAREHOUSE_LOCATION";

    private final WarehouseLocationDao locationDao = new WarehouseLocationDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Cây vị trí: khu (kèm các kệ bên trong) và kệ đứng riêng, theo thứ tự đi soạn hàng
    public List<WarehouseLocation> findTree(long warehouseId) throws SQLException {
        return buildTree(locationDao.findByWarehouse(warehouseId));
    }

    static List<WarehouseLocation> buildTree(List<WarehouseLocation> locations) {
        Map<Long, WarehouseLocation> zones = new LinkedHashMap<>();
        for (WarehouseLocation location : locations) {
            if (location.isZone()) {
                zones.put(location.getId(), location);
            }
        }
        List<WarehouseLocation> roots = new ArrayList<>();
        for (WarehouseLocation location : locations) {
            WarehouseLocation zone = location.isZone() || location.getParentId() == null ? null
                    : zones.get(location.getParentId());
            if (zone == null) {
                roots.add(location);
            } else {
                zone.getChildren().add(location);
            }
        }
        return roots;
    }

    // Khu đang dùng để chọn làm khu chứa kệ
    public List<WarehouseLocation> findActiveZones(long warehouseId) throws SQLException {
        return locationDao.findByWarehouse(warehouseId).stream()
                .filter(location -> location.isZone() && location.isActive()).toList();
    }

    // null nếu không có vị trí này trong kho đó
    public WarehouseLocation find(long warehouseId, long locationId) throws SQLException {
        WarehouseLocation location = locationDao.findById(locationId);
        return location == null || location.getWarehouseId() != warehouseId ? null : location;
    }

    // Lỗi theo tên ô; rỗng nghĩa là hợp lệ. editing = null khi thêm mới.
    public Map<String, String> validate(Warehouse warehouse, WarehouseLocationForm form, WarehouseLocation editing)
            throws SQLException {
        Map<String, String> errors = validateFields(form);
        if (!errors.containsKey("code") && locationDao.existsCode(warehouse.getId(), form.getCode(),
                editing == null ? null : editing.getId())) {
            errors.put("code", "Mã vị trí đã có trong kho " + warehouse.getCode() + ".");
        }
        if (!errors.containsKey("parentId") && form.getParentId() != null) {
            WarehouseLocation parent = find(warehouse.getId(), Long.parseLong(form.getParentId()));
            if (parent == null || !parent.isZone() || !parent.isActive()) {
                errors.put("parentId", "Khu chứa kệ phải là một khu đang dùng của kho này.");
            }
        }
        if (editing != null && editing.isZone() && WarehouseLocation.RACK.equals(form.getType())
                && locationDao.findByWarehouse(warehouse.getId()).stream()
                        .anyMatch(location -> Long.valueOf(editing.getId()).equals(location.getParentId()))) {
            errors.put("type", "Khu đang chứa kệ nên không đổi thành kệ được.");
        }
        return errors;
    }

    static Map<String, String> validateFields(WarehouseLocationForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!WarehouseLocation.TYPE_LABELS.containsKey(form.getType())) {
            errors.put("type", "Vui lòng chọn loại vị trí.");
        }
        if (form.getCode() == null) {
            errors.put("code", "Vui lòng nhập mã vị trí.");
        } else if (!CODE_PATTERN.matcher(form.getCode()).matches()) {
            errors.put("code", "Mã vị trí tối đa 30 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang"
                    + " (vd A, A-01, A-01-02).");
        }
        if (form.getName() != null && form.getName().length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên vị trí tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }
        if (form.getParentId() != null) {
            if (WarehouseLocation.ZONE.equals(form.getType())) {
                errors.put("parentId", "Khu không nằm trong khu khác; chỉ kệ mới chọn khu chứa.");
            } else if (!form.getParentId().matches("\\d{1,18}")) {
                errors.put("parentId", "Khu chứa kệ không hợp lệ.");
            }
        }
        if (parsePickSequence(form.getPickSequence()) == null) {
            errors.put("pickSequence", "Thứ tự soạn hàng là số nguyên từ 0 đến " + PICK_SEQUENCE_MAX + ".");
        }
        return errors;
    }

    // Ô trống là 0
    static Integer parsePickSequence(String value) {
        if (value == null) {
            return 0;
        }
        if (!value.matches("\\d{1,5}")) {
            return null;
        }
        int number = Integer.parseInt(value);
        return number <= PICK_SEQUENCE_MAX ? number : null;
    }

    // Gọi validate trước. Thêm mới khi editing = null; false nếu vị trí vừa bị xoá.
    // Ném SQLIntegrityConstraintViolationException nếu mã vừa bị vị trí khác dùng.
    public boolean save(Warehouse warehouse, WarehouseLocationForm form, WarehouseLocation editing, long actorUserId,
                        String ipAddress) throws SQLException {
        Long parentId = form.getParentId() == null ? null : Long.valueOf(form.getParentId());
        int pickSequence = parsePickSequence(form.getPickSequence());
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long id;
                if (editing == null) {
                    id = locationDao.insert(connection, warehouse.getId(), parentId, form.getType(), form.getCode(),
                            form.getName(), pickSequence, actorUserId);
                } else if (locationDao.update(connection, editing.getId(), parentId, form.getType(), form.getCode(),
                        form.getName(), pickSequence, actorUserId)) {
                    id = editing.getId();
                } else {
                    connection.rollback();
                    return false;
                }
                auditLogDao.insert(connection, actorUserId,
                        editing == null ? "WAREHOUSE_LOCATION_CREATE" : "WAREHOUSE_LOCATION_UPDATE", ENTITY, id,
                        editing == null ? null : JsonUtil.object(toValues(warehouse, WarehouseLocationForm.of(editing))),
                        JsonUtil.object(toValues(warehouse, form)), null, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // null nếu đổi được; ngược lại là lý do hiện cho người dùng
    public String validateDeactivate(WarehouseLocation location, List<WarehouseLocation> tree) {
        if (location.getSkuCount() > 0) {
            return "Vị trí " + location.getCode() + " đang là vị trí lưu chính của " + location.getSkuCount()
                    + " SKU nên chưa ngừng dùng được.";
        }
        boolean hasActiveRack = tree.stream().filter(node -> node.getId() == location.getId())
                .flatMap(node -> node.getChildren().stream()).anyMatch(WarehouseLocation::isActive);
        return hasActiveRack ? "Khu " + location.getCode() + " còn kệ đang dùng; ngừng dùng các kệ trong khu trước."
                : null;
    }

    // Ngừng dùng / dùng lại; false nếu đã ở trạng thái đó
    public boolean changeActive(Warehouse warehouse, WarehouseLocation location, boolean active, long actorUserId,
                                String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!locationDao.updateActive(connection, location.getId(), active, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                String code = warehouse.getCode() + "/" + location.getCode();
                auditLogDao.insert(connection, actorUserId,
                        active ? "WAREHOUSE_LOCATION_ACTIVATE" : "WAREHOUSE_LOCATION_DEACTIVATE", ENTITY,
                        location.getId(), JsonUtil.object(Map.of("code", code, "active", location.isActive())),
                        JsonUtil.object(Map.of("code", code, "active", active)), null, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // "code" ghép mã kho và mã vị trí (KHO-TT/A-01) để nhật ký thao tác hiện được vị trí của kho nào
    private static Map<String, Object> toValues(Warehouse warehouse, WarehouseLocationForm form) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", warehouse.getCode() + "/" + form.getCode());
        values.put("type", form.getType());
        values.put("parentId", form.getParentId() == null ? null : Long.valueOf(form.getParentId()));
        values.put("name", form.getName());
        values.put("pickSequence", parsePickSequence(form.getPickSequence()));
        return values;
    }
}
