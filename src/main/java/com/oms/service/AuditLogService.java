package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.ProductCategoryDao;
import com.oms.dao.RegionDao;
import com.oms.dao.RoleDao;
import com.oms.dao.UnitDao;
import com.oms.dao.UserDao;
import com.oms.dao.WarehouseDao;
import com.oms.model.AuditCatalog;
import com.oms.model.AuditLogFilter;
import com.oms.model.AuditLogRow;
import com.oms.model.PageResult;
import com.oms.model.ProductCategory;
import com.oms.model.Role;
import com.oms.model.SelectOption;
import com.oms.util.DateTimeUtil;
import com.oms.util.JsonUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Nhật ký thao tác (S2-04): xem, lọc theo người dùng/đối tượng/hành động/khoảng thời gian, xem giá trị
// trước và sau, xuất Excel. Chỉ đọc audit_logs; việc ghi nằm ở từng service cùng transaction với thao tác.
public class AuditLogService {

    public static final int PAGE_SIZE = 10;
    public static final int DEFAULT_DAYS = 30;
    // Giới hạn để một lần xuất không làm đầy bộ nhớ; lọc hẹp hơn nếu cần nhiều hơn
    public static final int EXPORT_MAX_ROWS = 5000;

    private static final String[] EXPORT_COLUMNS = {"STT", "Thời gian", "Người dùng", "Tài khoản", "Hành động",
            "Đối tượng", "Nội dung", "IP truy cập", "Giá trị trước", "Giá trị sau", "Lý do"};

    private final AuditLogDao auditLogDao = new AuditLogDao();
    private final UserDao userDao = new UserDao();
    private final RoleDao roleDao = new RoleDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final RegionDao regionDao = new RegionDao();
    private final ProductCategoryDao categoryDao = new ProductCategoryDao();
    private final UnitDao unitDao = new UnitDao();

    // from/to là ngày theo giờ Việt Nam, tính cả ngày to; null = không giới hạn phía đó
    public static AuditLogFilter filter(LocalDate from, LocalDate to, Long actorUserId, String entityType,
                                        String actionGroup) {
        return new AuditLogFilter(from == null ? null : DateTimeUtil.startOfDayUtc(from),
                to == null ? null : DateTimeUtil.startOfDayUtc(to.plusDays(1)), actorUserId,
                entityType != null && AuditCatalog.entities().containsKey(entityType) ? entityType : null,
                actionGroup != null && AuditCatalog.groups().containsKey(actionGroup) ? actionGroup : null);
    }

    public PageResult<AuditLogRow> search(AuditLogFilter filter, int requestedPage) throws SQLException {
        long total = auditLogDao.count(filter);
        int totalPages = Math.max(1, (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE));
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        return new PageResult<>(auditLogDao.findPage(filter, (page - 1) * PAGE_SIZE, PAGE_SIZE), page, PAGE_SIZE,
                total);
    }

    public List<SelectOption> getActors() throws SQLException {
        return auditLogDao.findActors();
    }

    // Tên ứng với các id/mã trong old_values/new_values, để hộp chi tiết hiện "Đồ uống" thay cho categoryId 20.
    // Khoá ngoài cùng là tên trường trong JSON.
    public String getNameLookupsJson() throws SQLException {
        Map<String, Map<String, String>> lookups = new LinkedHashMap<>();
        Map<String, String> categories = new LinkedHashMap<>();
        for (ProductCategory category : categoryDao.findAll()) {
            categories.put(String.valueOf(category.getId()), category.getName());
        }
        lookups.put("categoryId", categories);
        lookups.put("baseUnitId", names(unitDao.findAll()));
        lookups.put("warehouseId", names(warehouseDao.findActive()));
        lookups.put("regionId", names(regionDao.findActive()));
        Map<String, String> users = new LinkedHashMap<>();
        for (SelectOption user : userDao.findAllNames()) {
            users.put(String.valueOf(user.getId()), user.getName() + " (" + user.getCode() + ")");
        }
        lookups.put("handoverToUserId", users);
        Map<String, String> roles = new LinkedHashMap<>();
        for (Role role : roleDao.findAll()) {
            roles.put(role.getCode(), role.getName());
        }
        lookups.put("roleCodes", roles);
        return JsonUtil.object(lookups);
    }

    // Trả về số dòng đã xuất (tối đa EXPORT_MAX_ROWS, mới nhất trước)
    public int writeExcel(AuditLogFilter filter, OutputStream out) throws SQLException, IOException {
        List<AuditLogRow> rows = auditLogDao.findPage(filter, 0, EXPORT_MAX_ROWS);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle header = workbook.createCellStyle();
            header.setFont(bold);

            Sheet sheet = workbook.createSheet("Nhật ký thao tác");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_COLUMNS[i]);
                cell.setCellStyle(header);
            }
            int r = 1;
            for (AuditLogRow row : rows) {
                Row excelRow = sheet.createRow(r);
                excelRow.createCell(0).setCellValue(r);
                String[] values = {row.getOccurredAtText(), row.getActorName(), row.getActorUsername(),
                        row.getActionLabel(), row.getEntityLabel(), row.getSummary(), row.getIpAddress(),
                        row.getOldValues(), row.getNewValues(), row.getReason()};
                for (int i = 0; i < values.length; i++) {
                    excelRow.createCell(i + 1).setCellValue(values[i] == null ? "" : values[i]);
                }
                r++;
            }
            int[] widths = {6, 20, 24, 16, 18, 16, 36, 16, 50, 50, 30};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(out);
        }
        return rows.size();
    }

    private static Map<String, String> names(List<SelectOption> options) {
        Map<String, String> names = new LinkedHashMap<>();
        for (SelectOption option : options) {
            names.put(String.valueOf(option.getId()), option.getName());
        }
        return names;
    }
}
