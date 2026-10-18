package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.WarehouseDao;
import com.oms.model.PageResult;
import com.oms.model.SelectOption;
import com.oms.model.Warehouse;
import com.oms.model.WarehouseFilter;
import com.oms.model.WarehouseForm;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

// Khai báo kho (S5-03): danh sách có lọc, thêm, sửa, xuất Excel. Mỗi kho có mã, tên, địa chỉ, người phụ trách,
// loại (bán được / hàng lỗi) và trạng thái. Không xoá kho vì phiếu kho, tồn, đơn hàng trỏ tới; chỉ tạm ngừng.
public class WarehouseService {

    public static final int PAGE_SIZE = 10;

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_-]{2,30}");
    private static final int NAME_MAX_LENGTH = 150;
    private static final int DESCRIPTION_MAX_LENGTH = 200;
    private static final int ADDRESS_MAX_LENGTH = 300;
    private static final String ENTITY = "WAREHOUSE";
    private static final String[] EXPORT_HEADERS = {"Mã kho", "Tên kho", "Mô tả", "Địa chỉ", "Người phụ trách",
        "Loại kho", "Trạng thái", "Số vị trí"};

    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Ba thẻ số liệu đầu trang danh sách kho
    public record Summary(long total, long active, long defect) {
        public long getTotal() {
            return total;
        }

        public long getActive() {
            return active;
        }

        public long getDefect() {
            return defect;
        }
    }

    public PageResult<Warehouse> search(WarehouseFilter filter, int requestedPage) throws SQLException {
        long total = warehouseDao.count(filter);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        return new PageResult<>(warehouseDao.findPage(filter, (page - 1) * PAGE_SIZE, PAGE_SIZE), page, PAGE_SIZE,
                total);
    }

    public Summary summary() throws SQLException {
        long[] counts = warehouseDao.countSummary();
        return new Summary(counts[0], counts[1], counts[2]);
    }

    public Warehouse find(long id) throws SQLException {
        return warehouseDao.findById(id);
    }

    public List<SelectOption> getManagerCandidates() throws SQLException {
        return warehouseDao.findManagerCandidates();
    }

    // Lỗi theo tên ô; rỗng nghĩa là hợp lệ. editing = null khi thêm mới.
    public Map<String, String> validate(WarehouseForm form, Warehouse editing) throws SQLException {
        Map<String, String> errors = validateFields(form);
        Long editingId = editing == null ? null : editing.getId();
        if (!errors.containsKey("code")) {
            String usedBy = warehouseDao.findNameUsingCode(form.getCode(), editingId);
            if (usedBy != null) {
                errors.put("code", "Mã kho đã dùng cho \"" + usedBy + "\".");
            }
        }
        if (!errors.containsKey("managerUserId") && form.getManagerId() != null
                && (editing == null || !form.getManagerId().equals(editing.getManagerUserId()))
                && warehouseDao.findManagerCandidates().stream().noneMatch(user -> user.getId() == form.getManagerId())) {
            errors.put("managerUserId", "Người phụ trách phải là tài khoản Quản lý kho hoặc Nhân viên kho đang hoạt động.");
        }
        if (editing != null) {
            validateChange(form, editing, errors);
        }
        return errors;
    }

    // Kiểm tra từng ô không cần CSDL (định dạng, độ dài, giá trị chọn)
    static Map<String, String> validateFields(WarehouseForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getCode() == null) {
            errors.put("code", "Vui lòng nhập mã kho.");
        } else if (!CODE_PATTERN.matcher(form.getCode()).matches()) {
            errors.put("code", "Mã kho gồm 2–30 ký tự: chữ không dấu, số, gạch dưới hoặc gạch ngang.");
        }
        if (form.getName() == null) {
            errors.put("name", "Vui lòng nhập tên kho.");
        } else if (form.getName().length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên kho tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }
        if (form.getDescription() != null && form.getDescription().length() > DESCRIPTION_MAX_LENGTH) {
            errors.put("description", "Mô tả tối đa " + DESCRIPTION_MAX_LENGTH + " ký tự.");
        }
        if (form.getAddress() == null) {
            errors.put("address", "Vui lòng nhập địa chỉ kho.");
        } else if (form.getAddress().length() > ADDRESS_MAX_LENGTH) {
            errors.put("address", "Địa chỉ tối đa " + ADDRESS_MAX_LENGTH + " ký tự.");
        }
        if (form.getManagerUserId() != null && form.getManagerId() == null) {
            errors.put("managerUserId", "Người phụ trách không hợp lệ.");
        }
        if (!Warehouse.TYPE_LABELS.containsKey(form.getType())) {
            errors.put("type", "Vui lòng chọn loại kho.");
        }
        if (!Warehouse.STATUS_LABELS.containsKey(form.getStatus())) {
            errors.put("status", "Vui lòng chọn trạng thái.");
        }
        return errors;
    }

    // Kho còn hàng thì không tạm ngừng; kho đang phục vụ đại lý thì không đổi sang hàng lỗi hay tạm ngừng
    private void validateChange(WarehouseForm form, Warehouse editing, Map<String, String> errors) throws SQLException {
        boolean deactivating = editing.isActive() && Warehouse.INACTIVE.equals(form.getStatus());
        if (deactivating && editing.isHasStock()) {
            errors.putIfAbsent("status", "Kho còn hàng tồn nên chưa tạm ngừng được; chuyển hết hàng sang kho khác trước.");
        }
        boolean toDefect = !editing.isDefect() && Warehouse.DEFECT.equals(form.getType());
        if (deactivating || toDefect) {
            int customers = warehouseDao.countCustomersServed(editing.getId());
            if (customers > 0) {
                errors.putIfAbsent(deactivating ? "status" : "type", "Kho đang là kho phục vụ mặc định của " + customers
                        + " đại lý; đổi kho phục vụ của các đại lý đó trước.");
            }
        }
    }

    // Gọi validate trước. Ném SQLIntegrityConstraintViolationException nếu mã vừa bị kho khác dùng.
    public long create(WarehouseForm form, long actorUserId, String ipAddress) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long id = warehouseDao.insert(connection, form, actorUserId);
                auditLogDao.insert(connection, actorUserId, "WAREHOUSE_CREATE", ENTITY, id, null,
                        JsonUtil.object(toValues(form)), null, ipAddress);
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Gọi validate trước. false nếu kho không còn.
    public boolean update(Warehouse editing, WarehouseForm form, long actorUserId, String ipAddress)
            throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!warehouseDao.update(connection, editing.getId(), form, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                auditLogDao.insert(connection, actorUserId, "WAREHOUSE_UPDATE", ENTITY, editing.getId(),
                        JsonUtil.object(toValues(WarehouseForm.of(editing))), JsonUtil.object(toValues(form)), null,
                        ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // File Excel danh sách kho theo bộ lọc đang chọn; selectedIds không rỗng thì chỉ các kho được tích chọn
    public ByteArrayOutputStream export(WarehouseFilter filter, Set<Long> selectedIds) throws SQLException, IOException {
        List<Warehouse> warehouses = warehouseDao.findPage(filter, 0, 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh sách kho");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (Warehouse warehouse : warehouses) {
                if (!selectedIds.isEmpty() && !selectedIds.contains(warehouse.getId())) {
                    continue;
                }
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(warehouse.getCode());
                row.createCell(1).setCellValue(warehouse.getName());
                row.createCell(2).setCellValue(nullToEmpty(warehouse.getDescription()));
                row.createCell(3).setCellValue(nullToEmpty(warehouse.getAddress()));
                row.createCell(4).setCellValue(nullToEmpty(warehouse.getManagerName()));
                row.createCell(5).setCellValue(warehouse.getTypeLabel());
                row.createCell(6).setCellValue(warehouse.getStatusLabel());
                row.createCell(7).setCellValue(warehouse.getLocationCount());
            }
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, Object> toValues(WarehouseForm form) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", form.getCode());
        values.put("name", form.getName());
        values.put("description", form.getDescription());
        values.put("address", form.getAddress());
        values.put("managerUserId", form.getManagerId());
        values.put("type", form.getType());
        values.put("status", form.getStatus());
        return values;
    }
}
