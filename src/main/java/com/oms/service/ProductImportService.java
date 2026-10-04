package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.ProductDao;
import com.oms.dao.UnitDao;
import com.oms.model.Product;
import com.oms.model.ProductCategory;
import com.oms.model.ProductForm;
import com.oms.model.ProductImportBatch;
import com.oms.model.ProductImportRow;
import com.oms.model.ProductUnitConversion;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

// Nhập danh mục sản phẩm hàng loạt từ Excel (S2-08): tệp mẫu, đọc + kiểm tra từng dòng, thêm mới hoặc cập nhật
// theo SKU, báo cáo lỗi. Kiểm tra dùng cùng giới hạn với form sản phẩm (ProductService) nhưng tra nhóm hàng, đơn vị,
// SKU có sẵn một lần cho cả file thay vì hỏi CSDL từng dòng.
public class ProductImportService {

    // Đưa được 5.000 mã hàng hiện có trong một lần (user story S2-08)
    public static final int MAX_ROWS = 5000;

    private static final String[] COLUMNS = {
            "Mã SKU *", "Tên sản phẩm *", "Nhóm hàng *", "Đơn vị tính cơ sở *", "Quy cách đóng gói", "Giá vốn",
            "Trạng thái", "Mô tả", "Quy đổi đơn vị"};
    private static final int SKU = 0;
    private static final int NAME = 1;
    private static final int CATEGORY = 2;
    private static final int BASE_UNIT = 3;
    private static final int PACKAGING = 4;
    private static final int COST = 5;
    private static final int STATUS = 6;
    private static final int DESCRIPTION = 7;
    private static final int CONVERSIONS = 8;

    private static final String DATA_SHEET = "Sản phẩm";
    private static final String ACTIVE_LABEL = "Đang kinh doanh";
    private static final String DISCONTINUED_LABEL = "Ngừng kinh doanh";
    private static final String ENTITY = "PRODUCT";
    // Hệ số có thể dùng dấu phẩy thập phân (0,5) nên các cặp quy đổi chỉ tách bằng dấu chấm phẩy hoặc xuống dòng
    private static final Pattern CONVERSION_SEPARATOR = Pattern.compile("[;\\n]");
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

    private final ProductDao productDao = new ProductDao();
    private final ProductCategoryService categoryService = new ProductCategoryService();
    private final UnitDao unitDao = new UnitDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Lỗi của cả file (sai mẫu, không đọc được, quá nhiều dòng...), message hiện thẳng cho người dùng
    public static class InvalidFileException extends Exception {
        public InvalidFileException(String message) {
            super(message);
        }
    }

    // Kết quả đọc ô "Quy đổi đơn vị": hệ số theo id đơn vị và lỗi (nếu có)
    static final class ParsedConversions {
        final Map<Long, BigDecimal> factors = new LinkedHashMap<>();
        final List<String> errors = new ArrayList<>();
    }

    // Cột Giá vốn luôn có trong mẫu để file của mọi người giống nhau; sheet hướng dẫn ghi rõ ai được điền
    public void writeTemplate(OutputStream out, boolean canEditCost) throws IOException, SQLException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle header = headerStyle(workbook);
            CellStyle text = workbook.createCellStyle();
            // Định dạng chữ để Excel không đổi SKU toàn số sang dạng 1.23E+10 hay bỏ số 0 đầu
            text.setDataFormat(workbook.createDataFormat().getFormat("@"));

            Sheet data = workbook.createSheet(DATA_SHEET);
            Row headerRow = data.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                setCell(headerRow, i, COLUMNS[i], header);
                data.setDefaultColumnStyle(i, text);
                data.setColumnWidth(i, (i == NAME || i == DESCRIPTION || i == CONVERSIONS ? 34 : 20) * 256);
            }
            data.createFreezePane(0, 1);

            Sheet guide = workbook.createSheet("Hướng dẫn");
            guide.setColumnWidth(0, 24 * 256);
            guide.setColumnWidth(1, 90 * 256);
            int r = writeGuideTitle(guide, 0, "Cách điền sheet \"" + DATA_SHEET + "\"", header);
            String[][] notes = {
                    {"Mã SKU *", "2–50 ký tự: chữ không dấu, số, dấu chấm, gạch dưới, gạch ngang (tự đổi sang chữ hoa). "
                            + "SKU đã có trong hệ thống thì dòng đó cập nhật sản phẩm, không tạo mới."},
                    {"Tên sản phẩm *", "Tối đa " + ProductService.NAME_MAX_LENGTH + " ký tự."},
                    {"Nhóm hàng *", "Mã hoặc tên nhóm ở bảng dưới (nhóm đang ngừng hoạt động không chọn được)."},
                    {"Đơn vị tính cơ sở *", "Mã hoặc tên đơn vị ở bảng dưới, là đơn vị nhỏ nhất để ghi sổ, vd Lon."},
                    {"Quy cách đóng gói", "Tối đa " + ProductService.PACKAGING_MAX_LENGTH + " ký tự, vd Thùng 24 lon x 330ml."},
                    {"Giá vốn", canEditCost
                            ? "Số tiền nguyên, không âm; được gõ dấu phân cách hàng nghìn (30.000). Trống khi thêm mới là 0."
                            : "Bạn không có quyền giá vốn nên phải để trống cột này; dòng có giá vốn sẽ báo lỗi."},
                    {"Trạng thái", "\"" + ACTIVE_LABEL + "\" hoặc \"" + DISCONTINUED_LABEL + "\"; trống khi thêm mới là "
                            + ACTIVE_LABEL + "."},
                    {"Mô tả", "Tối đa " + ProductService.DESCRIPTION_MAX_LENGTH + " ký tự."},
                    {"Quy đổi đơn vị", "Các cặp đơn vị = hệ số về đơn vị cơ sở, cách nhau bằng dấu chấm phẩy, "
                            + "vd Lốc=6; Thùng=24. Hệ số là số dương khác 1, tối đa 4 chữ số thập phân (vd 0,5). "
                            + "Có giá trị thì thay toàn bộ quy đổi đang có của SKU."},
                    {"Ô để trống", "Với SKU đã có: ô trống nghĩa là giữ nguyên giá trị đang lưu, không xoá đi. "
                            + "Muốn bỏ quy đổi hoặc xoá mô tả thì sửa trên màn hình sản phẩm."},
                    {"Giới hạn", "Tối đa " + MAX_ROWS + " dòng mỗi file. Dòng lỗi bị bỏ qua, dòng hợp lệ vẫn được nhập."}};
            for (String[] note : notes) {
                r = writeGuideRow(guide, r, note[0], note[1]);
            }

            r = writeGuideTitle(guide, r + 1, "Nhóm hàng (mã – tên)", header);
            for (ProductCategory category : categoryService.getTree()) {
                if (category.isActive()) {
                    r = writeGuideRow(guide, r, category.getCode(), "  ".repeat(category.getLevel() - 1) + category.getName());
                }
            }
            r = writeGuideTitle(guide, r + 1, "Đơn vị tính (mã – tên)", header);
            for (SelectOption unit : unitDao.findAll()) {
                r = writeGuideRow(guide, r, unit.getCode(), unit.getName());
            }
            workbook.write(out);
        }
    }

    // Đọc sheet đầu tiên và kiểm tra từng dòng; không ghi gì vào CSDL
    public ProductImportBatch read(InputStream in, String fileName, boolean canEditCost)
            throws IOException, SQLException, InvalidFileException {
        List<List<String>> values = new ArrayList<>();
        List<Integer> excelRowNumbers = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(VIETNAMESE);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            checkHeader(sheet.getRow(sheet.getFirstRowNum()), formatter, evaluator);

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                List<String> cells = readCells(sheet.getRow(r), formatter, evaluator);
                if (cells.stream().allMatch(String::isEmpty)) {
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

        Set<String> skus = new LinkedHashSet<>();
        for (List<String> cells : values) {
            if (!cells.get(SKU).isEmpty()) {
                skus.add(cells.get(SKU).toUpperCase(Locale.ROOT));
            }
        }
        Map<String, Product> existingBySku = productDao.findBySkus(skus, canEditCost);
        List<Long> existingIds = existingBySku.values().stream().map(Product::getId).toList();
        Lookups lookups = new Lookups(categoryService.getTree(), unitDao.findAll(),
                productDao.findConversions(existingIds));

        List<ProductImportRow> rows = new ArrayList<>();
        Map<String, Integer> seenSkus = new HashMap<>();
        for (int i = 0; i < values.size(); i++) {
            ProductImportRow row = buildRow(i + 1, excelRowNumbers.get(i), values.get(i), lookups, existingBySku,
                    canEditCost);
            if (!row.getSku().isEmpty()) {
                // Báo theo số dòng Excel (khớp cột "Dòng trong file gốc" của báo cáo lỗi) để tìm được dòng kia
                Integer firstRow = seenSkus.putIfAbsent(row.getSku(), row.getExcelRowNumber());
                if (firstRow != null) {
                    row.addError("Trùng mã SKU với dòng " + firstRow + " trong file Excel.");
                }
            }
            rows.add(row);
        }
        return new ProductImportBatch(fileName, rows);
    }

    // Thêm mới hoặc cập nhật các dòng hợp lệ, mỗi dòng một transaction: dòng lỗi không ảnh hưởng các dòng khác.
    // Cả file dùng chung một Connection vì ứng dụng chưa có connection pool, mở lại vài nghìn lần sẽ rất chậm.
    public void importValidRows(ProductImportBatch batch, long actorUserId, String ipAddress) throws SQLException {
        String reason = "Nhập từ Excel: " + batch.getFileName();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            for (ProductImportRow row : batch.getRows()) {
                if (!row.isValid()) {
                    continue;
                }
                try {
                    boolean saved = row.isUpdate()
                            ? updateOne(connection, row, actorUserId, ipAddress, reason)
                            : createOne(connection, row, actorUserId, ipAddress, reason);
                    if (saved) {
                        connection.commit();
                        row.markImported();
                    } else {
                        connection.rollback();
                    }
                } catch (SQLIntegrityConstraintViolationException e) {
                    // Người khác vừa tạo trùng SKU, hoặc xoá nhóm hàng/đơn vị sau bước xem trước
                    connection.rollback();
                    row.addError("Dữ liệu vừa bị người khác thay đổi nên chưa lưu được dòng này. Hãy nhập lại.");
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            }
        }
    }

    // File Excel gồm các dòng bị lỗi (giữ nguyên dữ liệu gốc) kèm cột lý do để sửa rồi nhập lại
    public void writeErrorReport(List<ProductImportRow> rows, OutputStream out) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle header = headerStyle(workbook);
            Sheet sheet = workbook.createSheet(DATA_SHEET);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                setCell(headerRow, i, COLUMNS[i], header);
                sheet.setColumnWidth(i, 20 * 256);
            }
            setCell(headerRow, COLUMNS.length, "Dòng trong file gốc", header);
            setCell(headerRow, COLUMNS.length + 1, "Lỗi", header);
            sheet.setColumnWidth(COLUMNS.length, 18 * 256);
            sheet.setColumnWidth(COLUMNS.length + 1, 70 * 256);

            int r = 1;
            for (ProductImportRow row : rows) {
                Row excelRow = sheet.createRow(r++);
                for (int i = 0; i < row.getCells().size(); i++) {
                    setCell(excelRow, i, row.getCells().get(i), null);
                }
                excelRow.createCell(COLUMNS.length).setCellValue(row.getExcelRowNumber());
                setCell(excelRow, COLUMNS.length + 1, row.getErrorText(), null);
            }
            workbook.write(out);
        }
    }

    private ProductImportRow buildRow(int index, int excelRowNumber, List<String> cells, Lookups lookups,
                                      Map<String, Product> existingBySku, boolean canEditCost) throws SQLException {
        String sku = cells.get(SKU).toUpperCase(Locale.ROOT);
        Product existing = existingBySku.get(sku);
        List<String> errors = new ArrayList<>();

        String skuError = ProductService.skuError(sku.isEmpty() ? null : sku);
        if (skuError != null) {
            errors.add(skuError);
        }

        String name = keepIfEmpty(cells.get(NAME), existing == null ? null : existing.getName());
        if (name == null) {
            errors.add("Vui lòng nhập tên sản phẩm.");
        } else if (name.length() > ProductService.NAME_MAX_LENGTH) {
            errors.add("Tên sản phẩm tối đa " + ProductService.NAME_MAX_LENGTH + " ký tự.");
        }

        Long categoryId = existing == null ? null : existing.getCategoryId();
        String categoryText = cells.get(CATEGORY);
        if (categoryText.isEmpty()) {
            if (existing == null) {
                errors.add("Vui lòng nhập nhóm hàng.");
            }
        } else if (lookups.ambiguousCategories.contains(key(categoryText))) {
            errors.add("Có nhiều nhóm hàng tên \"" + categoryText + "\", hãy ghi mã nhóm.");
        } else {
            ProductCategory category = lookups.categories.get(key(categoryText));
            if (category == null) {
                errors.add("Không tìm thấy nhóm hàng: " + categoryText + ".");
            } else if (!category.isActive() && (existing == null || existing.getCategoryId() != category.getId())) {
                errors.add("Nhóm \"" + category.getName() + "\" đang ngừng hoạt động.");
            } else {
                categoryId = category.getId();
            }
        }

        Long baseUnitId = existing == null ? null : existing.getBaseUnitId();
        String unitText = cells.get(BASE_UNIT);
        if (unitText.isEmpty()) {
            if (existing == null) {
                errors.add("Vui lòng nhập đơn vị tính cơ sở.");
            }
        } else {
            SelectOption unit = lookups.units.get(key(unitText));
            if (unit == null) {
                errors.add("Không tìm thấy đơn vị tính: " + unitText + ".");
            } else {
                baseUnitId = unit.getId();
            }
        }
        boolean baseUnitChanged = existing != null && baseUnitId != null && existing.getBaseUnitId() != baseUnitId;
        if (baseUnitChanged && productDao.hasTransactions(existing.getId())) {
            // Sổ tồn và chứng từ cũ lưu số lượng theo đơn vị cơ sở cũ
            errors.add("Sản phẩm đã phát sinh giao dịch nên không đổi được đơn vị tính cơ sở.");
        }

        String packagingSpec = keepIfEmpty(cells.get(PACKAGING), existing == null ? null : existing.getPackagingSpec());
        if (packagingSpec != null && packagingSpec.length() > ProductService.PACKAGING_MAX_LENGTH) {
            errors.add("Quy cách đóng gói tối đa " + ProductService.PACKAGING_MAX_LENGTH + " ký tự.");
        }

        String costText = cells.get(COST);
        BigDecimal costPrice = null;
        boolean costChanged = false;
        if (!canEditCost) {
            if (!costText.isEmpty()) {
                errors.add("Bạn không có quyền nhập giá vốn, hãy để trống cột Giá vốn.");
            }
        } else if (!costText.isEmpty() || existing == null) {
            costPrice = ProductService.parseCostPrice(costText.isEmpty() ? null : costText);
            if (costPrice == null) {
                errors.add("Giá vốn là số tiền nguyên, không âm, tối đa " + ProductService.COST_MAX_DIGITS + " chữ số.");
            } else if (existing != null && existing.getCostPrice() != null) {
                costChanged = costPrice.compareTo(existing.getCostPrice()) != 0;
            }
        }

        String status = existing == null ? Product.ACTIVE : existing.getStatus();
        String statusText = cells.get(STATUS);
        if (!statusText.isEmpty()) {
            String parsed = parseStatus(statusText);
            if (parsed == null) {
                errors.add("Trạng thái không hợp lệ: " + statusText + ". Chỉ nhận \"" + ACTIVE_LABEL + "\" hoặc \""
                        + DISCONTINUED_LABEL + "\".");
            } else {
                status = parsed;
            }
        }

        String description = keepIfEmpty(cells.get(DESCRIPTION), existing == null ? null : existing.getDescription());
        if (description != null && description.length() > ProductService.DESCRIPTION_MAX_LENGTH) {
            errors.add("Mô tả tối đa " + ProductService.DESCRIPTION_MAX_LENGTH + " ký tự.");
        }

        // Ô trống của SKU đã có thì giữ quy đổi đang lưu; vẫn kiểm tra lại với đơn vị cơ sở mới (nếu đổi)
        List<ProductUnitConversion> oldConversions = existing == null ? List.of()
                : lookups.conversions.getOrDefault(existing.getId(), List.of());
        String conversionCell = cells.get(CONVERSIONS);
        Map<Long, BigDecimal> factors = new LinkedHashMap<>();
        if (conversionCell.isEmpty()) {
            for (ProductUnitConversion conversion : oldConversions) {
                factors.put(conversion.getUnitId(), conversion.getFactor());
                if (baseUnitId != null && conversion.getUnitId() == baseUnitId) {
                    errors.add("Đơn vị cơ sở mới trùng đơn vị quy đổi \"" + conversion.getUnitName()
                            + "\" đang có; hãy ghi lại cột Quy đổi đơn vị.");
                }
            }
        } else {
            ParsedConversions parsed = parseConversions(conversionCell, lookups.units);
            errors.addAll(parsed.errors);
            factors = parsed.factors;
            if (baseUnitId != null && factors.containsKey(baseUnitId)) {
                errors.add("Quy đổi không được chứa đơn vị tính cơ sở.");
            }
        }

        List<ProductForm.Conversion> conversions = new ArrayList<>();
        factors.forEach((unitId, factor) -> conversions.add(new ProductForm.Conversion(unitId, text(factor))));
        ProductForm form = new ProductForm(sku.isEmpty() ? null : sku, name, categoryId, baseUnitId, packagingSpec,
                costPrice == null ? null : costPrice.toPlainString(), status, description,
                existing == null ? null : existing.getVersion(), conversions);

        String oldValuesJson = null;
        if (existing != null) {
            Map<String, Object> oldValues = ProductService.toValues(existing);
            Map<Long, BigDecimal> oldFactors = new LinkedHashMap<>();
            oldConversions.forEach(c -> oldFactors.put(c.getUnitId(), c.getFactor()));
            oldValues.put("conversions", auditTexts(oldFactors));
            oldValuesJson = JsonUtil.object(oldValues);
        }
        boolean rewriteUnits = existing == null || !conversionCell.isEmpty() || baseUnitChanged;
        ProductImportRow row = new ProductImportRow(index, excelRowNumber, cells,
                existing == null ? null : existing.getId(), form, costPrice, costChanged,
                lookups.categoryName(categoryId), lookups.unitName(baseUnitId), displayText(factors, lookups),
                factors, rewriteUnits, oldValuesJson);
        errors.forEach(row::addError);
        return row;
    }

    private boolean createOne(Connection connection, ProductImportRow row, long actorUserId, String ipAddress,
                              String reason) throws SQLException {
        ProductForm form = row.getForm();
        long id = productDao.insert(connection, form, row.getCostPrice(), actorUserId);
        productDao.replaceBaseUnit(connection, id, form.getBaseUnitId(), actorUserId);
        productDao.replaceConversions(connection, id, row.getFactors(), actorUserId);
        boolean costSet = row.getCostPrice() != null && row.getCostPrice().signum() != 0;
        Map<String, Object> values = ProductService.toValues(form, costSet);
        values.put("conversions", auditTexts(row.getFactors()));
        auditLogDao.insert(connection, actorUserId, "PRODUCT_CREATE", ENTITY, id, null, JsonUtil.object(values),
                reason, ipAddress);
        return true;
    }

    // false khi người khác vừa sửa sản phẩm sau bước xem trước (version đã đổi): dòng này báo lỗi, không ghi gì
    private boolean updateOne(Connection connection, ProductImportRow row, long actorUserId, String ipAddress,
                              String reason) throws SQLException {
        ProductForm form = row.getForm();
        long id = row.getExistingId();
        if (!productDao.update(connection, id, form.getVersion(), form, row.getCostPrice(), actorUserId)) {
            row.addError("Sản phẩm vừa được người khác sửa nên chưa cập nhật. Hãy tải file lên và nhập lại.");
            return false;
        }
        if (row.isRewriteUnits()) {
            // Xoá quy đổi cũ trước khi đổi đơn vị cơ sở: đơn vị cơ sở mới có thể là đơn vị quy đổi cũ
            // (UNIQUE product_id + unit_id), giống ProductService.update
            productDao.replaceConversions(connection, id, Map.of(), actorUserId);
            productDao.replaceBaseUnit(connection, id, form.getBaseUnitId(), actorUserId);
            productDao.replaceConversions(connection, id, row.getFactors(), actorUserId);
        }
        Map<String, Object> values = ProductService.toValues(form, row.isCostChanged());
        values.put("conversions", auditTexts(row.getFactors()));
        auditLogDao.insert(connection, actorUserId, "PRODUCT_UPDATE", ENTITY, id, row.getOldValuesJson(),
                JsonUtil.object(values), reason, ipAddress);
        return true;
    }

    // "Lốc=6; Thùng=24": đơn vị ghi mã hoặc tên; cùng quy tắc hệ số với form sản phẩm (S2-07)
    static ParsedConversions parseConversions(String text, Map<String, SelectOption> units) {
        ParsedConversions result = new ParsedConversions();
        for (String token : CONVERSION_SEPARATOR.split(text)) {
            if (token.isBlank()) {
                continue;
            }
            int equals = token.indexOf('=');
            if (equals < 0) {
                result.errors.add("Quy đổi \"" + token.trim() + "\" sai dạng, hãy ghi như Lốc=6; Thùng=24.");
                continue;
            }
            String unitText = token.substring(0, equals).trim();
            String factorText = token.substring(equals + 1).trim();
            SelectOption unit = units.get(key(unitText));
            BigDecimal factor = UnitConversionService.parseFactor(factorText);
            if (unit == null) {
                result.errors.add("Không tìm thấy đơn vị quy đổi: " + unitText + ".");
            } else if (result.factors.containsKey(unit.getId())) {
                result.errors.add("Đơn vị quy đổi \"" + unit.getName() + "\" bị ghi hai lần.");
            } else if (factor == null) {
                result.errors.add("Hệ số của \"" + unit.getName()
                        + "\" là số dương khác 1, tối đa 4 chữ số thập phân (vd 24 hoặc 0,5).");
            } else {
                result.factors.put(unit.getId(), factor);
            }
        }
        if (result.factors.size() > ProductService.MAX_CONVERSIONS) {
            result.errors.add("Một sản phẩm tối đa " + ProductService.MAX_CONVERSIONS + " đơn vị quy đổi.");
        }
        return result;
    }

    // null nếu không nhận ra trạng thái
    static String parseStatus(String text) {
        return switch (key(text)) {
            case "đang kinh doanh", "kinh doanh", "active" -> Product.ACTIVE;
            case "ngừng kinh doanh", "ngưng kinh doanh", "discontinued" -> Product.DISCONTINUED;
            default -> null;
        };
    }

    // Nhật ký ghi quy đổi theo id đơn vị, xếp theo id như ProductService để trước/sau so được với nhau
    private static List<String> auditTexts(Map<Long, BigDecimal> factors) {
        Map<Long, String> texts = new TreeMap<>();
        factors.forEach((unitId, factor) -> texts.put(unitId, unitId + "=" + text(factor)));
        return new ArrayList<>(texts.values());
    }

    private static String displayText(Map<Long, BigDecimal> factors, Lookups lookups) {
        List<String> parts = new ArrayList<>();
        factors.forEach((unitId, factor) -> parts.add(lookups.unitName(unitId) + " = " + text(factor)));
        return String.join(", ", parts);
    }

    private static String keepIfEmpty(String cell, String current) {
        return cell.isEmpty() ? current : cell;
    }

    private static String text(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
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
        // SKU hoặc giá vốn gõ vào ô kiểu Number bị Excel hiển thị dạng 1.23E+10: lấy đủ chữ số
        if (cell.getCellType() == CellType.NUMERIC) {
            return new BigDecimal(String.valueOf(cell.getNumericCellValue())).stripTrailingZeros().toPlainString();
        }
        return formatter.formatCellValue(cell, evaluator);
    }

    // So khớp không phân biệt hoa thường và khoảng trắng thừa ("  thùng " = "Thùng")
    private static String key(String text) {
        return text.trim().replaceAll("\\s+", " ").toLowerCase(VIETNAMESE);
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

    // Tra nhóm hàng/đơn vị theo mã hoặc tên. Nhóm hàng nhiều cấp có thể trùng tên giữa các nhánh: khi đó bắt ghi mã.
    private static final class Lookups {
        private final Map<String, ProductCategory> categories = new HashMap<>();
        private final Set<String> ambiguousCategories = new HashSet<>();
        private final Map<Long, String> categoryNames = new HashMap<>();
        private final Map<String, SelectOption> units = new HashMap<>();
        private final Map<Long, String> unitNames = new HashMap<>();
        private final Map<Long, List<ProductUnitConversion>> conversions;

        private Lookups(List<ProductCategory> categoryList, List<SelectOption> unitList,
                        Map<Long, List<ProductUnitConversion>> conversions) {
            this.conversions = conversions;
            for (ProductCategory category : categoryList) {
                categoryNames.put(category.getId(), category.getName());
                if (categories.putIfAbsent(key(category.getName()), category) != null) {
                    ambiguousCategories.add(key(category.getName()));
                }
            }
            // Mã là duy nhất nên tra theo mã luôn đúng, kể cả khi mã trùng tên của nhóm khác
            for (ProductCategory category : categoryList) {
                categories.put(key(category.getCode()), category);
                ambiguousCategories.remove(key(category.getCode()));
            }
            // Tên đơn vị là duy nhất (UnitService) nên mã và tên đều tra thẳng được
            for (SelectOption unit : unitList) {
                unitNames.put(unit.getId(), unit.getName());
                units.put(key(unit.getName()), unit);
                units.put(key(unit.getCode()), unit);
            }
        }

        private String categoryName(Long id) {
            return id == null ? "" : categoryNames.getOrDefault(id, "");
        }

        private String unitName(Long id) {
            return id == null ? "" : unitNames.getOrDefault(id, "");
        }
    }
}
