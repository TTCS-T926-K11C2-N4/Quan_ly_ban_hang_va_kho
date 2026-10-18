package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.GoodsReceiptDao;
import com.oms.dao.WarehouseDao;
import com.oms.model.GoodsReceiptDetail;
import com.oms.model.GoodsReceiptFilter;
import com.oms.model.GoodsReceiptForm;
import com.oms.model.GoodsReceiptRow;
import com.oms.model.PageResult;
import com.oms.model.ReceiptProduct;
import com.oms.model.SelectOption;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Phiếu nhập kho từ nhà cung cấp (S5-04). Nhập theo đơn vị tính bất kỳ của SKU, quy về đơn vị cơ sở bằng hệ số
// (chụp lại hệ số lúc lưu). Phiếu nháp không ảnh hưởng tồn; ghi sổ mới cộng sổ tồn, tồn theo lô và ghi thẻ kho,
// tất cả trong một transaction. Phiếu đã ghi sổ không sửa được, chỉ lập phiếu điều chỉnh (adjusts_receipt_id)
// cho phép số lượng âm để giảm.
public class GoodsReceiptService {

    public static final int PAGE_SIZE = 10;

    private static final Pattern QTY_PATTERN = Pattern.compile("-?\\d{1,12}([.,]\\d{1,3})?");
    // 120000 hoặc 120.000 (dấu chấm ngăn cách hàng nghìn)
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{3}){1,4}|\\d{1,15}");
    private static final Pattern LOT_PATTERN = Pattern.compile("[A-Za-z0-9._/-]{1,50}");
    private static final int DOCUMENT_REF_MAX_LENGTH = 60;
    private static final int NOTE_MAX_LENGTH = 1000;
    private static final int QTY_SCALE = 3;
    private static final String ENTITY = "GOODS_RECEIPT";
    private static final String[] EXPORT_HEADERS = {"Mã phiếu", "Phiếu gốc", "Nhà cung cấp", "Mã kho", "Kho nhận",
        "Ngày nhập", "Tổng tiền", "Trạng thái"};

    private final GoodsReceiptDao receiptDao = new GoodsReceiptDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public record Summary(long total, long draft, long confirmed) {
        public long getTotal() {
            return total;
        }

        public long getDraft() {
            return draft;
        }

        public long getConfirmed() {
            return confirmed;
        }
    }

    // Dữ liệu cho các ô chọn của form: sản phẩm, nhà cung cấp, kho nhận người dùng được lập phiếu, vị trí từng kho
    public record Options(List<ReceiptProduct> products, List<SelectOption> suppliers, List<SelectOption> warehouses,
                          Map<Long, List<SelectOption>> locations) {
        public List<ReceiptProduct> getProducts() {
            return products;
        }

        public List<SelectOption> getSuppliers() {
            return suppliers;
        }

        public List<SelectOption> getWarehouses() {
            return warehouses;
        }

        public Map<Long, List<SelectOption>> getLocations() {
            return locations;
        }
    }

    // Dòng hàng đã kiểm tra, sẵn sàng ghi
    record PreparedLine(ReceiptProduct product, ReceiptProduct.Unit unit, BigDecimal qty, BigDecimal qtyBase,
                        BigDecimal unitPrice, Long locationId, String lotNo, LocalDate expiryDate) {
    }

    // Phiếu đã lưu (mã phiếu cấp khi tạo mới)
    public record Saved(long id, String receiptNo) {
    }

    // Ghi sổ làm tồn thực tế âm hoặc nhỏ hơn lượng đang giữ chỗ (chỉ xảy ra với phiếu điều chỉnh giảm)
    public static class StockShortageException extends Exception {
        public StockShortageException(String message) {
            super(message);
        }
    }

    public PageResult<GoodsReceiptRow> search(GoodsReceiptFilter filter, int requestedPage) throws SQLException {
        long total = receiptDao.count(filter);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        return new PageResult<>(receiptDao.findPage(filter, (page - 1) * PAGE_SIZE, PAGE_SIZE), page, PAGE_SIZE, total);
    }

    public Summary summary() throws SQLException {
        long[] counts = receiptDao.countByStatus();
        return new Summary(counts[0], counts[1], counts[2]);
    }

    public GoodsReceiptDetail find(long id) throws SQLException {
        return receiptDao.findDetail(id);
    }

    public String previewNextReceiptNo() throws SQLException {
        return receiptDao.previewNextReceiptNo();
    }

    // Người được gán kho phụ trách (S1-09) chỉ lập phiếu cho các kho đó; không gán kho nào thì lập cho mọi kho
    public Options getOptions(long userId) throws SQLException {
        Set<Long> assigned = receiptDao.findUserWarehouseIds(userId);
        List<SelectOption> warehouses = warehouseDao.findActive().stream()
                .filter(warehouse -> assigned.isEmpty() || assigned.contains(warehouse.getId())).toList();
        return new Options(receiptDao.findProducts(), receiptDao.findActiveSuppliers(), warehouses,
                receiptDao.findActiveLocations());
    }

    // Người dùng được lập/sửa phiếu của kho này
    public boolean canUseWarehouse(Options options, long warehouseId) {
        return options.warehouses().stream().anyMatch(warehouse -> warehouse.getId() == warehouseId);
    }

    // Lỗi đầu phiếu theo tên ô; lỗi từng dòng ghi vào line.getErrors() và thêm khoá "lines".
    // Rỗng nghĩa là hợp lệ. editing: phiếu nháp đang sửa (null khi tạo mới); original: phiếu gốc khi là phiếu điều chỉnh.
    public Map<String, String> validate(GoodsReceiptForm form, Options options, GoodsReceiptDetail editing,
                                        GoodsReceiptDetail original) throws SQLException {
        Map<String, String> errors = validateHeader(form, options, editing, original);
        LocalDate receiptDate = parseDate(form.getReceiptDate());
        Map<Long, ReceiptProduct> products = options.products().stream()
                .collect(Collectors.toMap(ReceiptProduct::getId, Function.identity()));
        List<SelectOption> locations = form.getWarehouseId() == null ? List.of()
                : options.locations().getOrDefault(parseId(form.getWarehouseId()), List.of());
        int filled = 0;
        boolean lineErrors = false;
        for (GoodsReceiptForm.Line line : form.getLines()) {
            if (line.isBlank()) {
                continue;
            }
            filled++;
            validateLine(line, products, locations, receiptDate, form.isAdjustment());
            lineErrors |= !line.getErrors().isEmpty();
        }
        if (filled == 0) {
            errors.put("lines", "Thêm ít nhất một dòng hàng nhập.");
        } else if (lineErrors) {
            errors.put("lines", "Kiểm tra lại các dòng hàng được đánh dấu đỏ.");
        }
        return errors;
    }

    // Phiếu điều chỉnh giữ nguyên nhà cung cấp, kho của phiếu gốc; phiếu nháp giữ được nhà cung cấp đã chọn trước
    // dù nhà cung cấp đó vừa ngừng giao dịch
    private Map<String, String> validateHeader(GoodsReceiptForm form, Options options, GoodsReceiptDetail editing,
                                               GoodsReceiptDetail original) {
        Map<String, String> errors = new LinkedHashMap<>();
        Long supplierId = parseId(form.getSupplierId());
        boolean keptSupplier = supplierId != null && ((editing != null && supplierId == editing.getSupplierId())
                || (original != null && supplierId == original.getSupplierId()));
        if (original != null && (supplierId == null || supplierId != original.getSupplierId()
                || !String.valueOf(original.getWarehouseId()).equals(form.getWarehouseId()))) {
            errors.put("supplierId", "Phiếu điều chỉnh phải cùng nhà cung cấp và kho với phiếu gốc "
                    + original.getReceiptNo() + ".");
        }
        if (supplierId == null) {
            errors.putIfAbsent("supplierId", "Vui lòng chọn nhà cung cấp.");
        } else if (!keptSupplier && options.suppliers().stream().noneMatch(s -> s.getId() == supplierId)) {
            errors.put("supplierId", "Nhà cung cấp không còn giao dịch; chọn nhà cung cấp khác.");
        }
        Long warehouseId = parseId(form.getWarehouseId());
        if (warehouseId == null) {
            errors.put("warehouseId", "Vui lòng chọn kho nhận.");
        } else if (!canUseWarehouse(options, warehouseId)) {
            errors.put("warehouseId", "Bạn chỉ lập phiếu nhập cho kho đang hoạt động mà mình phụ trách.");
        }
        LocalDate receiptDate = parseDate(form.getReceiptDate());
        if (form.getReceiptDate() == null) {
            errors.put("receiptDate", "Vui lòng chọn ngày nhập.");
        } else if (receiptDate == null) {
            errors.put("receiptDate", "Ngày nhập không hợp lệ.");
        } else if (receiptDate.isAfter(DateTimeUtil.today())) {
            errors.put("receiptDate", "Ngày nhập không được sau hôm nay.");
        }
        if (form.getDocumentRef() != null && form.getDocumentRef().length() > DOCUMENT_REF_MAX_LENGTH) {
            errors.put("documentRef", "Số chứng từ tối đa " + DOCUMENT_REF_MAX_LENGTH + " ký tự.");
        }
        if (form.getNote() != null && form.getNote().length() > NOTE_MAX_LENGTH) {
            errors.put("note", "Ghi chú tối đa " + NOTE_MAX_LENGTH + " ký tự.");
        }
        return errors;
    }

    private void validateLine(GoodsReceiptForm.Line line, Map<Long, ReceiptProduct> products,
                              List<SelectOption> locations, LocalDate receiptDate, boolean adjustment)
            throws SQLException {
        Map<String, String> errors = line.getErrors();
        Long productId = parseId(line.getProductId());
        ReceiptProduct product = productId == null ? null : products.get(productId);
        if (product == null) {
            errors.put("productId", "Chọn sản phẩm trong danh sách gợi ý.");
            return;
        }
        Long unitId = parseId(line.getUnitId());
        ReceiptProduct.Unit unit = unitId == null ? null : product.findUnit(unitId);
        if (unit == null) {
            errors.put("unitId", "Chọn đơn vị tính đã khai báo của sản phẩm.");
        }
        BigDecimal qty = parseQty(line.getQty(), adjustment);
        if (qty == null) {
            errors.put("qty", adjustment ? "Số lượng khác 0, tối đa 3 số lẻ; nhập số âm để giảm."
                    : "Số lượng phải lớn hơn 0, tối đa 3 số lẻ.");
        } else if (unit != null && toBase(qty, unit).signum() == 0) {
            errors.put("qty", "Số lượng quá nhỏ, quy về đơn vị cơ sở bằng 0.");
        }
        if (line.getUnitPrice() == null) {
            errors.put("unitPrice", "Nhập đơn giá nhập.");
        } else if (parseMoney(line.getUnitPrice()) == null) {
            errors.put("unitPrice", "Đơn giá là số tiền không âm, vd 120.000.");
        }
        Long locationId = parseId(line.getLocationId());
        if (line.getLocationId() != null && (locationId == null
                || locations.stream().noneMatch(location -> location.getId() == locationId))) {
            errors.put("locationId", "Vị trí không thuộc kho nhận.");
        }
        if (product.isLotTracked()) {
            validateLot(line, product, receiptDate, errors);
        }
    }

    // SKU quản lý lô: bắt buộc số lô; hạn sử dụng (nếu có) sau ngày nhập và khớp hạn đã ghi của lô đó
    private void validateLot(GoodsReceiptForm.Line line, ReceiptProduct product, LocalDate receiptDate,
                             Map<String, String> errors) throws SQLException {
        if (line.getLotNo() == null) {
            errors.put("lotNo", "Nhập số lô cho mặt hàng quản lý lô.");
            return;
        }
        if (!LOT_PATTERN.matcher(line.getLotNo()).matches()) {
            errors.put("lotNo", "Số lô tối đa 50 ký tự: chữ không dấu, số, dấu chấm, gạch ngang, gạch chéo.");
            return;
        }
        LocalDate expiry = parseDate(line.getExpiryDate());
        if (line.getExpiryDate() != null && expiry == null) {
            errors.put("expiryDate", "Hạn sử dụng không hợp lệ.");
            return;
        }
        if (expiry != null && receiptDate != null && !expiry.isAfter(receiptDate)) {
            errors.put("expiryDate", "Hạn sử dụng phải sau ngày nhập.");
            return;
        }
        GoodsReceiptDao.ExistingLot existing = receiptDao.findLot(product.getId(), line.getLotNo());
        if (existing != null && existing.expiryDate() != null && expiry != null
                && !existing.expiryDate().equals(expiry)) {
            errors.put("expiryDate", "Lô " + line.getLotNo() + " đã ghi hạn sử dụng "
                    + DateTimeUtil.formatDay(existing.expiryDate()) + ".");
        }
    }

    // Gọi validate trước (không lỗi). Dòng trống bị bỏ qua.
    List<PreparedLine> prepare(GoodsReceiptForm form, Options options) {
        Map<Long, ReceiptProduct> products = options.products().stream()
                .collect(Collectors.toMap(ReceiptProduct::getId, Function.identity()));
        List<PreparedLine> lines = new ArrayList<>();
        for (GoodsReceiptForm.Line line : form.getLines()) {
            if (line.isBlank()) {
                continue;
            }
            ReceiptProduct product = products.get(parseId(line.getProductId()));
            ReceiptProduct.Unit unit = product.findUnit(parseId(line.getUnitId()));
            BigDecimal qty = parseQty(line.getQty(), form.isAdjustment());
            boolean tracked = product.isLotTracked();
            lines.add(new PreparedLine(product, unit, qty, toBase(qty, unit), parseMoney(line.getUnitPrice()),
                    parseId(line.getLocationId()), tracked ? line.getLotNo() : null,
                    tracked ? parseDate(line.getExpiryDate()) : null));
        }
        return lines;
    }

    // Gọi validate trước. post = true: lưu rồi ghi sổ luôn trong cùng transaction.
    // null nếu phiếu đang sửa không còn là nháp (vừa được ghi sổ ở nơi khác).
    public Saved save(GoodsReceiptForm form, Options options, GoodsReceiptDetail editing, boolean post,
                      long actorUserId, String ipAddress) throws SQLException, StockShortageException {
        List<PreparedLine> lines = prepare(form, options);
        GoodsReceiptDao.Header header = new GoodsReceiptDao.Header(parseId(form.getSupplierId()),
                parseId(form.getWarehouseId()), form.getDocumentRef(), parseDate(form.getReceiptDate()), form.getNote(),
                parseId(form.getAdjustsReceiptId()));
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long id;
                String receiptNo;
                if (editing == null) {
                    receiptNo = receiptDao.nextReceiptNo(connection);
                    id = receiptDao.insertHeader(connection, receiptNo, header, actorUserId);
                } else {
                    id = editing.getId();
                    receiptNo = editing.getReceiptNo();
                    if (!receiptDao.updateDraftHeader(connection, id, header, actorUserId)) {
                        connection.rollback();
                        return null;
                    }
                    receiptDao.deleteItems(connection, id);
                }
                for (PreparedLine line : lines) {
                    long lotId = receiptDao.findOrCreateLot(connection, line.product().getId(), line.lotNo(),
                            line.expiryDate(), header.supplierId(), actorUserId);
                    receiptDao.insertItem(connection, id, new GoodsReceiptDao.ItemRow(line.product().getId(), lotId,
                            line.unit().getId(), line.unit().getFactor(), line.qty(), line.qtyBase(), line.unitPrice(),
                            line.locationId()));
                }
                Map<String, Object> values = values(receiptNo, header, lines);
                auditLogDao.insert(connection, actorUserId, editing == null ? "GOODS_RECEIPT_CREATE"
                        : "GOODS_RECEIPT_UPDATE", ENTITY, id, editing == null ? null
                        : JsonUtil.object(Map.of("receiptNo", receiptNo, "lines", editing.getItems().size())),
                        JsonUtil.object(values), null, ipAddress);
                if (post) {
                    post(connection, id, receiptNo, header, actorUserId, ipAddress);
                }
                connection.commit();
                return new Saved(id, receiptNo);
            } catch (SQLException | StockShortageException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Ghi sổ: khoá phiếu, khoá sổ tồn theo thứ tự SKU rồi lô (tránh khoá chéo), cộng tồn, ghi thẻ kho, chuyển trạng thái
    private void post(Connection connection, long receiptId, String receiptNo, GoodsReceiptDao.Header header,
                      long actorUserId, String ipAddress) throws SQLException, StockShortageException {
        if (!GoodsReceiptRow.DRAFT.equals(receiptDao.lockStatus(connection, receiptId))) {
            throw new StockShortageException("Phiếu " + receiptNo + " đã được ghi sổ trước đó.");
        }
        List<GoodsReceiptDao.PostingItem> items = receiptDao.findPostingItems(connection, receiptId);
        long warehouseId = header.warehouseId();
        Map<Long, BigDecimal[]> balances = new TreeMap<>();
        for (GoodsReceiptDao.PostingItem item : items) {
            balances.putIfAbsent(item.productId(), null);
        }
        for (Long productId : balances.keySet()) {
            balances.put(productId, receiptDao.lockBalance(connection, warehouseId, productId));
        }
        Map<Long, BigDecimal> lotBalances = new TreeMap<>();
        for (GoodsReceiptDao.PostingItem item : items) {
            lotBalances.putIfAbsent(item.lotId(), null);
        }
        for (Long lotId : lotBalances.keySet()) {
            lotBalances.put(lotId, receiptDao.lockLotBalance(connection, warehouseId, lotId));
        }
        boolean adjustment = header.adjustsReceiptId() != null;
        String movementType = adjustment ? "ADJUSTMENT" : "RECEIPT";
        String note = adjustment ? "Phiếu điều chỉnh " + receiptNo : "Nhập kho theo phiếu " + receiptNo;
        BigDecimal totalBase = BigDecimal.ZERO;
        for (GoodsReceiptDao.PostingItem item : items) {
            BigDecimal[] balance = balances.get(item.productId());
            BigDecimal onHand = balance[0].add(item.qtyBase());
            checkShortage(item, onHand, balance[1]);
            BigDecimal lotOnHand = lotBalances.get(item.lotId()).add(item.qtyBase());
            if (lotOnHand.signum() < 0) {
                throw new StockShortageException("SKU " + item.sku() + ": lô " + item.lotNo()
                        + " không đủ tồn để giảm " + item.qtyBase().negate().stripTrailingZeros().toPlainString() + ".");
            }
            balance[0] = onHand;
            lotBalances.put(item.lotId(), lotOnHand);
            receiptDao.updateBalance(connection, warehouseId, item.productId(), onHand, item.locationId());
            receiptDao.updateLotBalance(connection, warehouseId, item.lotId(), lotOnHand);
            receiptDao.insertMovement(connection, warehouseId, item, movementType, onHand, receiptId, note, actorUserId);
            totalBase = totalBase.add(item.qtyBase());
        }
        receiptDao.confirm(connection, receiptId, actorUserId);
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("receiptNo", receiptNo);
        newValues.put("status", GoodsReceiptRow.CONFIRMED);
        newValues.put("warehouseId", warehouseId);
        newValues.put("totalQtyBase", totalBase);
        auditLogDao.insert(connection, actorUserId, "GOODS_RECEIPT_POST", ENTITY, receiptId,
                JsonUtil.object(Map.of("receiptNo", receiptNo, "status", GoodsReceiptRow.DRAFT)),
                JsonUtil.object(newValues), null, ipAddress);
    }

    // Tồn thực tế sau ghi sổ không được âm, cũng không nhỏ hơn lượng đang giữ chỗ cho đơn đã duyệt
    static void checkShortage(GoodsReceiptDao.PostingItem item, BigDecimal onHand, BigDecimal reserved)
            throws StockShortageException {
        if (onHand.signum() < 0) {
            throw new StockShortageException("SKU " + item.sku() + ": tồn kho không đủ để giảm "
                    + item.qtyBase().negate().stripTrailingZeros().toPlainString() + " (đơn vị cơ sở).");
        }
        if (onHand.compareTo(reserved) < 0) {
            throw new StockShortageException("SKU " + item.sku() + ": tồn sau điều chỉnh ("
                    + onHand.stripTrailingZeros().toPlainString() + ") nhỏ hơn lượng đang giữ chỗ cho đơn đã duyệt ("
                    + reserved.stripTrailingZeros().toPlainString() + ").");
        }
    }

    // File Excel danh sách phiếu nhập theo bộ lọc đang chọn
    public ByteArrayOutputStream export(GoodsReceiptFilter filter) throws SQLException, IOException {
        List<GoodsReceiptRow> rows = receiptDao.findPage(filter, 0, 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Phiếu nhập kho");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (GoodsReceiptRow receipt : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(receipt.getReceiptNo());
                row.createCell(1).setCellValue(receipt.getAdjustsReceiptNo() == null ? "" : receipt.getAdjustsReceiptNo());
                row.createCell(2).setCellValue(receipt.getSupplierName());
                row.createCell(3).setCellValue(receipt.getWarehouseCode());
                row.createCell(4).setCellValue(receipt.getWarehouseName());
                row.createCell(5).setCellValue(DateTimeUtil.formatDay(receipt.getReceiptDate()));
                row.createCell(6).setCellValue(receipt.getTotalAmount().doubleValue());
                row.getCell(6).setCellStyle(moneyStyle);
                row.createCell(7).setCellValue(receipt.getStatusLabel());
            }
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out;
        }
    }

    static BigDecimal toBase(BigDecimal qty, ReceiptProduct.Unit unit) {
        return qty.multiply(unit.getFactor()).setScale(QTY_SCALE, RoundingMode.HALF_UP);
    }

    // Số lượng tối đa 3 số lẻ, dấu thập phân "." hoặc ","; phiếu thường phải dương, phiếu điều chỉnh khác 0
    static BigDecimal parseQty(String value, boolean allowNegative) {
        if (value == null || !QTY_PATTERN.matcher(value).matches()) {
            return null;
        }
        BigDecimal qty = new BigDecimal(value.replace(',', '.'));
        return qty.signum() > 0 || (allowNegative && qty.signum() < 0) ? qty : null;
    }

    static BigDecimal parseMoney(String value) {
        if (value == null || !MONEY_PATTERN.matcher(value).matches()) {
            return null;
        }
        return new BigDecimal(value.replace(".", ""));
    }

    static LocalDate parseDate(String value) {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static Long parseId(String value) {
        return value == null || !value.matches("\\d{1,18}") ? null : Long.valueOf(value);
    }

    private static Map<String, Object> values(String receiptNo, GoodsReceiptDao.Header header, List<PreparedLine> lines) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("receiptNo", receiptNo);
        values.put("supplierId", header.supplierId());
        values.put("warehouseId", header.warehouseId());
        values.put("receiptDate", header.receiptDate().toString());
        values.put("documentRef", header.documentRef());
        values.put("adjustsReceiptId", header.adjustsReceiptId());
        values.put("lines", lines.size());
        values.put("totalQtyBase", lines.stream().map(PreparedLine::qtyBase).reduce(BigDecimal.ZERO, BigDecimal::add));
        values.put("totalAmount", lines.stream().map(line -> line.qty().multiply(line.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return values;
    }
}
