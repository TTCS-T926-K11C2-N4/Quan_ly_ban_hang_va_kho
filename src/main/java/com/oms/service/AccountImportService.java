package com.oms.service;

import com.oms.model.AccountForm;
import com.oms.model.ImportBatch;
import com.oms.model.ImportRow;
import com.oms.model.Role;
import com.oms.model.SelectOption;
import jakarta.mail.MessagingException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

// Nhập người dùng hàng loạt từ Excel (S2-01): tệp mẫu, đọc + kiểm tra từng dòng, tạo tài khoản, báo cáo lỗi
public class AccountImportService {

    // Mỗi tài khoản gửi một email mật khẩu tạm (~1-2 giây/thư) nên giới hạn số dòng để một lần nhập
    // không kéo dài quá vài phút
    public static final int MAX_ROWS = 200;

    private static final String[] COLUMNS = {
            "Họ và tên *", "Tên đăng nhập *", "Email *", "Số điện thoại *", "Vai trò *", "Mã kho", "Mã địa bàn"};
    private static final String DATA_SHEET = "Người dùng";
    // Dòng mẫu trong tệp mẫu có tên đăng nhập bắt đầu bằng "vd.": khi nhập thì bỏ qua để quên xoá cũng không tạo
    // tài khoản mẫu (và không gửi email tới địa chỉ mẫu)
    static final String SAMPLE_PREFIX = "vd.";
    private static final Pattern ROLE_SEPARATOR = Pattern.compile("[,;\\n]");
    private static final Pattern PHONE_SEPARATOR = Pattern.compile("[\\s.\\-]");
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

    private final AccountService accountService = new AccountService();

    // Lỗi của cả file (sai mẫu, không đọc được, quá nhiều dòng...), message hiện thẳng cho người dùng
    public static class InvalidFileException extends Exception {
        public InvalidFileException(String message) {
            super(message);
        }
    }

    public void writeTemplate(OutputStream out) throws IOException, SQLException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle header = headerStyle(workbook);
            CellStyle text = workbook.createCellStyle();
            // Định dạng chữ để Excel không bỏ số 0 đầu của số điện thoại
            text.setDataFormat(workbook.createDataFormat().getFormat("@"));

            Sheet data = workbook.createSheet(DATA_SHEET);
            Row headerRow = data.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUMNS[i]);
                cell.setCellStyle(header);
                data.setDefaultColumnStyle(i, text);
                data.setColumnWidth(i, (i == 2 || i == 4 ? 32 : 22) * 256);
            }
            data.createFreezePane(0, 1);
            writeSampleRows(workbook, data);

            Sheet guide = workbook.createSheet("Hướng dẫn");
            guide.setColumnWidth(0, 22 * 256);
            guide.setColumnWidth(1, 70 * 256);
            int r = 0;
            r = writeGuideTitle(guide, r, "Cách điền sheet \"" + DATA_SHEET + "\"", header);
            String[][] notes = {
                    {"Họ và tên *", "Tối đa 150 ký tự."},
                    {"Tên đăng nhập *", "3–50 ký tự: chữ không dấu, số, dấu chấm, gạch dưới, gạch ngang. Không trùng tài khoản đã có."},
                    {"Email *", "Mật khẩu tạm được gửi tới email này. Không trùng tài khoản đã có."},
                    {"Số điện thoại *", "Đúng 10 chữ số, vd 0987123456."},
                    {"Vai trò *", "Mã hoặc tên vai trò ở bảng dưới; nhiều vai trò cách nhau bằng dấu phẩy."},
                    {"Mã kho", "Bắt buộc khi có vai trò Quản lý kho hoặc Nhân viên kho. Ghi mã hoặc tên kho."},
                    {"Mã địa bàn", "Bắt buộc khi có vai trò Nhân viên kinh doanh. Ghi mã hoặc tên địa bàn."},
                    {"Giới hạn", "Tối đa " + MAX_ROWS + " dòng mỗi file. Dòng lỗi bị bỏ qua, dòng hợp lệ vẫn được nhập."},
                    {"Dòng mẫu", "Các dòng chữ nghiêng nền xám (tên đăng nhập bắt đầu bằng \"" + SAMPLE_PREFIX
                            + "\") chỉ để xem cách điền: hệ thống tự bỏ qua khi nhập. Điền dữ liệu thật vào các dòng bên dưới "
                            + "hoặc ghi đè lên dòng mẫu."}};
            for (String[] note : notes) {
                r = writeGuideRow(guide, r, note[0], note[1]);
            }

            r = writeGuideTitle(guide, r + 1, "Vai trò", header);
            for (Role role : accountService.getAssignableRoles()) {
                r = writeGuideRow(guide, r, role.getCode(), role.getName());
            }
            r = writeGuideTitle(guide, r + 1, "Kho", header);
            for (SelectOption warehouse : accountService.getWarehouses()) {
                r = writeGuideRow(guide, r, warehouse.getCode(), warehouse.getName());
            }
            r = writeGuideTitle(guide, r + 1, "Địa bàn", header);
            for (SelectOption region : accountService.getRegions()) {
                r = writeGuideRow(guide, r, region.getCode(), region.getName());
            }
            workbook.write(out);
        }
    }

    // Đọc sheet đầu tiên và kiểm tra từng dòng; không ghi gì vào CSDL
    public ImportBatch read(InputStream in, String fileName) throws IOException, SQLException, InvalidFileException {
        List<List<String>> values = new ArrayList<>();
        List<Integer> excelRowNumbers = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(VIETNAMESE);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            checkHeader(sheet.getRow(sheet.getFirstRowNum()), formatter, evaluator);

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                List<String> cells = readCells(sheet.getRow(r), formatter, evaluator);
                if (cells.stream().allMatch(String::isEmpty) || isSampleRow(cells)) {
                    continue;
                }
                if (values.size() == MAX_ROWS) {
                    throw new InvalidFileException("File có quá " + MAX_ROWS + " dòng dữ liệu. Hãy chia thành nhiều file.");
                }
                values.add(cells);
                excelRowNumbers.add(r + 1);
            }
        } catch (EncryptedDocumentException e) {
            throw new InvalidFileException("File đang đặt mật khẩu. Hãy bỏ mật khẩu rồi tải lên lại.");
        } catch (IOException | RuntimeException e) {
            // POI báo file hỏng/không phải Excel bằng nhiều loại exception khác nhau
            throw new InvalidFileException("Không đọc được file. Hãy dùng file .xlsx hoặc .xls tạo từ file mẫu.");
        }
        if (values.isEmpty()) {
            throw new InvalidFileException("File chưa có dòng dữ liệu nào.");
        }

        Lookups lookups = new Lookups(accountService.getAssignableRoles(), accountService.getWarehouses(),
                accountService.getRegions());
        List<ImportRow> rows = new ArrayList<>();
        Map<String, Integer> usernames = new HashMap<>();
        Map<String, Integer> emails = new HashMap<>();
        for (int i = 0; i < values.size(); i++) {
            ImportRow row = buildRow(i + 1, excelRowNumbers.get(i), values.get(i), lookups);
            checkDuplicateInFile(row, row.getForm().getUsername(), usernames, "Trùng tên đăng nhập với dòng ");
            checkDuplicateInFile(row, row.getForm().getEmail(), emails, "Trùng email với dòng ");
            rows.add(row);
        }
        return new ImportBatch(fileName, rows);
    }

    // Tạo tài khoản cho các dòng hợp lệ. Kiểm tra lại với CSDL vì có thể đã có người tạo trùng sau bước xem trước.
    // Mỗi dòng một transaction: dòng lỗi không ảnh hưởng các dòng khác.
    public void importValidRows(ImportBatch batch, long actorUserId, String ipAddress, String loginUrl)
            throws SQLException {
        for (ImportRow row : batch.getRows()) {
            if (!row.isValid()) {
                continue;
            }
            Map<String, String> errors = accountService.validateNewAccount(row.getForm());
            if (!errors.isEmpty()) {
                errors.values().forEach(row::addError);
                continue;
            }
            try {
                accountService.create(row.getForm(), actorUserId, ipAddress, loginUrl);
                row.markCreated();
            } catch (SQLIntegrityConstraintViolationException e) {
                row.addError("Tên đăng nhập hoặc email vừa được tài khoản khác sử dụng.");
            } catch (MessagingException e) {
                row.addError("Không gửi được email mật khẩu tạm nên chưa tạo tài khoản.");
            }
        }
    }

    // File Excel gồm các dòng bị lỗi (giữ nguyên dữ liệu gốc) kèm cột lý do để sửa rồi nhập lại
    public void writeErrorReport(List<ImportRow> rows, OutputStream out) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle header = headerStyle(workbook);
            Sheet sheet = workbook.createSheet(DATA_SHEET);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                setCell(headerRow, i, COLUMNS[i], header);
                sheet.setColumnWidth(i, 22 * 256);
            }
            setCell(headerRow, COLUMNS.length, "Dòng trong file gốc", header);
            setCell(headerRow, COLUMNS.length + 1, "Lỗi", header);
            sheet.setColumnWidth(COLUMNS.length, 18 * 256);
            sheet.setColumnWidth(COLUMNS.length + 1, 60 * 256);

            int r = 1;
            for (ImportRow row : rows) {
                Row excelRow = sheet.createRow(r++);
                String[] cells = {row.getFullName(), row.getUsername(), row.getEmail(), row.getPhone(),
                        row.getRolesText(), row.getWarehouseText(), row.getRegionText()};
                for (int i = 0; i < cells.length; i++) {
                    setCell(excelRow, i, cells[i], null);
                }
                excelRow.createCell(COLUMNS.length).setCellValue(row.getExcelRowNumber());
                setCell(excelRow, COLUMNS.length + 1, row.getErrorText(), null);
            }
            workbook.write(out);
        }
    }

    private ImportRow buildRow(int index, int excelRowNumber, List<String> cells, Lookups lookups)
            throws SQLException {
        String fullName = cells.get(0);
        String username = cells.get(1);
        String email = cells.get(2);
        String phone = normalizePhone(cells.get(3));
        String rolesText = cells.get(4);
        String warehouseText = cells.get(5);
        String regionText = cells.get(6);

        List<String> rowErrors = new ArrayList<>();
        List<String> roleCodes = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();
        for (String token : splitRoles(rolesText)) {
            Role role = lookups.roles.get(key(token));
            if (role == null) {
                rowErrors.add("Vai trò không hợp lệ: " + token + ".");
            } else if (!roleCodes.contains(role.getCode())) {
                roleCodes.add(role.getCode());
                roleNames.add(role.getName());
            }
        }
        SelectOption warehouse = warehouseText.isEmpty() ? null : lookups.warehouses.get(key(warehouseText));
        if (!warehouseText.isEmpty() && warehouse == null) {
            rowErrors.add("Không tìm thấy kho: " + warehouseText + ".");
        }
        SelectOption region = regionText.isEmpty() ? null : lookups.regions.get(key(regionText));
        if (!regionText.isEmpty() && region == null) {
            rowErrors.add("Không tìm thấy địa bàn: " + regionText + ".");
        }

        AccountForm form = new AccountForm(emptyToNull(fullName), emptyToNull(username), emptyToNull(email),
                emptyToNull(phone), roleCodes, warehouse == null ? null : warehouse.getId(),
                region == null ? null : region.getId());
        Map<String, String> formErrors = new LinkedHashMap<>(accountService.validateNewAccount(form));
        // Đã có lỗi cụ thể hơn ("Vai trò không hợp lệ: X") thì bỏ lỗi chung của form cho ô đó
        if (rowErrors.stream().anyMatch(error -> error.startsWith("Vai trò"))) {
            formErrors.remove("roleCodes");
        }
        if (!warehouseText.isEmpty() && warehouse == null) {
            formErrors.remove("warehouseId");
        }
        if (!regionText.isEmpty() && region == null) {
            formErrors.remove("regionId");
        }

        List<String> assignments = new ArrayList<>();
        if (warehouse != null) {
            assignments.add(warehouse.getName());
        }
        if (region != null) {
            assignments.add(region.getName());
        }
        ImportRow row = new ImportRow(index, excelRowNumber, fullName, username, email, phone, rolesText,
                warehouseText, regionText, String.join(", ", roleNames), String.join(" • ", assignments), form);
        formErrors.values().forEach(row::addError);
        rowErrors.forEach(row::addError);
        return row;
    }

    static boolean isSampleRow(List<String> cells) {
        return cells.get(1).toLowerCase(Locale.ROOT).startsWith(SAMPLE_PREFIX);
    }

    // Ba dòng minh hoạ đủ các trường hợp: vai trò cần địa bàn, vai trò cần kho, nhiều vai trò. Mã kho/địa bàn lấy từ
    // danh mục hiện có để dòng mẫu luôn đúng với hệ thống đang chạy.
    private void writeSampleRows(Workbook workbook, Sheet data) throws SQLException {
        List<SelectOption> warehouses = accountService.getWarehouses();
        List<SelectOption> regions = accountService.getRegions();
        String warehouse = warehouses.isEmpty() ? "" : warehouses.get(0).getCode();
        String region = regions.isEmpty() ? "" : regions.get(0).getCode();
        String[][] samples = {
                {"Nguyễn Văn A (dòng mẫu)", SAMPLE_PREFIX + "nguyenvana", "vd.nguyenvana@example.com", "0987123456",
                        "Nhân viên kinh doanh", "", region},
                {"Trần Thị B (dòng mẫu)", SAMPLE_PREFIX + "tranthib", "vd.tranthib@example.com", "0912345678",
                        "WAREHOUSE", warehouse, ""},
                {"Lê Văn C (dòng mẫu)", SAMPLE_PREFIX + "levanc", "vd.levanc@example.com", "0909876543",
                        "Kế toán công nợ, Quản lý kinh doanh", "", ""}};
        CellStyle style = sampleStyle(workbook);
        for (int r = 0; r < samples.length; r++) {
            Row row = data.createRow(r + 1);
            for (int i = 0; i < samples[r].length; i++) {
                setCell(row, i, samples[r][i], style);
            }
        }
    }

    // Chữ nghiêng màu xám, nền xám nhạt; giữ định dạng chữ (@) để số điện thoại không mất số 0 đầu
    private static CellStyle sampleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setDataFormat(workbook.createDataFormat().getFormat("@"));
        return style;
    }

    // Báo theo số dòng Excel (khớp cột "Dòng trong file gốc" của báo cáo lỗi) để người dùng tìm được dòng kia
    private static void checkDuplicateInFile(ImportRow row, String value, Map<String, Integer> seen, String message) {
        if (value == null) {
            return;
        }
        Integer firstRow = seen.putIfAbsent(value.toLowerCase(Locale.ROOT), row.getExcelRowNumber());
        if (firstRow != null) {
            row.addError(message + firstRow + " trong file Excel.");
        }
    }

    private static void checkHeader(Row header, DataFormatter formatter, FormulaEvaluator evaluator)
            throws InvalidFileException {
        List<String> cells = readCells(header, formatter, evaluator);
        for (int i = 0; i < COLUMNS.length; i++) {
            if (!key(cells.get(i).replace("*", "")).equals(key(COLUMNS[i].replace("*", "")))) {
                throw new InvalidFileException("File không đúng mẫu: cột " + (i + 1) + " phải là \""
                        + COLUMNS[i].replace(" *", "") + "\". Hãy tải file mẫu và điền lại.");
            }
        }
    }

    private static List<String> readCells(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < COLUMNS.length; i++) {
            Cell cell = row == null ? null : row.getCell(i);
            cells.add(cell == null ? "" : cellText(cell, formatter, evaluator).trim());
        }
        return cells;
    }

    private static String cellText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        // Số nhập vào ô kiểu Number bị Excel hiển thị dạng 9.88E+08 hoặc mất số 0 đầu: lấy đủ chữ số
        if (cell.getCellType() == CellType.NUMERIC) {
            return new BigDecimal(String.valueOf(cell.getNumericCellValue())).stripTrailingZeros().toPlainString();
        }
        return formatter.formatCellValue(cell, evaluator);
    }

    // Bỏ khoảng trắng, dấu chấm, gạch ngang; số 9 chữ số là số đã bị Excel bỏ mất số 0 đầu
    static String normalizePhone(String raw) {
        String digits = PHONE_SEPARATOR.matcher(raw).replaceAll("");
        return digits.matches("[1-9]\\d{8}") ? "0" + digits : digits;
    }

    static List<String> splitRoles(String rolesText) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : ROLE_SEPARATOR.split(rolesText)) {
            if (!token.isBlank()) {
                tokens.add(token.trim());
            }
        }
        return new ArrayList<>(tokens);
    }

    // So khớp không phân biệt hoa thường và khoảng trắng thừa ("nhân viên  KHO" = "Nhân viên kho")
    private static String key(String text) {
        return text.trim().replaceAll("\\s+", " ").toLowerCase(VIETNAMESE);
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static void setCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static int writeGuideTitle(Sheet sheet, int rowIndex, String title, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        setCell(row, 0, title, style);
        setCell(row, 1, "", style);
        return rowIndex + 1;
    }

    private static int writeGuideRow(Sheet sheet, int rowIndex, String first, String second) {
        Row row = sheet.createRow(rowIndex);
        setCell(row, 0, first, null);
        setCell(row, 1, second, null);
        return rowIndex + 1;
    }

    // Tra vai trò/kho/địa bàn theo mã hoặc theo tên
    private static final class Lookups {
        private final Map<String, Role> roles = new HashMap<>();
        private final Map<String, SelectOption> warehouses = new HashMap<>();
        private final Map<String, SelectOption> regions = new HashMap<>();

        private Lookups(List<Role> roleList, List<SelectOption> warehouseList, List<SelectOption> regionList) {
            for (Role role : roleList) {
                roles.put(key(role.getCode()), role);
                roles.put(key(role.getName()), role);
            }
            index(warehouseList, warehouses);
            index(regionList, regions);
        }

        private static void index(List<SelectOption> options, Map<String, SelectOption> map) {
            for (SelectOption option : options) {
                map.put(key(option.getCode()), option);
                map.put(key(option.getName()), option);
            }
        }
    }
}
