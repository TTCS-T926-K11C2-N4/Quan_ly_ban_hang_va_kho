package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.PriceListDao;
import com.oms.model.PageResult;
import com.oms.model.PriceList;
import com.oms.model.PriceListForm;
import com.oms.model.PriceListItem;
import com.oms.model.SelectOption;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Bảng giá theo nhóm khách hàng và thời gian hiệu lực (S2-10). Mỗi nhóm mỗi ngày chỉ có một bảng giá để đại lý
// luôn nhận đúng giá; mỗi dòng có giá bán và giá sàn (giá sàn <= giá bán). Bảng đã có đơn dùng thì khoá,
// muốn đổi giá phải tạo phiên bản mới: chép dòng giá, bản cũ kết thúc hôm trước ngày bản mới bắt đầu.
// Mọi thay đổi ghi audit_logs (đối tượng PRICE, S2-04) và price_history.
public class PriceListService {

    public static final int PAGE_SIZE = 10;
    public static final int MAX_LINES = 1000;
    private static final int NAME_MAX_LENGTH = 150;
    private static final String CODE_PREFIX = "BG";
    private static final String ENTITY = "PRICE";

    private final PriceListDao priceListDao = new PriceListDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public List<SelectOption> getGroups() throws SQLException {
        return priceListDao.findActiveGroups();
    }

    public PageResult<PriceList> search(Long groupId, LocalDate from, LocalDate to, int requestedPage)
            throws SQLException {
        long total = priceListDao.count(groupId, from, to);
        int totalPages = Math.max(1, (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE));
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        return new PageResult<>(priceListDao.findPage(groupId, from, to, (page - 1) * PAGE_SIZE, PAGE_SIZE), page,
                PAGE_SIZE, total);
    }

    public PriceList find(long id) throws SQLException {
        return priceListDao.findById(id);
    }

    public List<PriceListItem> getItems(long priceListId, boolean canViewCost) throws SQLException {
        return priceListDao.findItems(priceListId, canViewCost);
    }

    public List<PriceListItem> getPricingProducts(boolean canViewCost) throws SQLException {
        return priceListDao.findPricingProducts(canViewCost);
    }

    public boolean hasNextVersion(long id) throws SQLException {
        return priceListDao.hasNextVersion(id);
    }

    // Form sửa / tạo phiên bản điền sẵn từ bảng giá đang có
    public PriceListForm toForm(PriceList list, String validFrom, String validTo) throws SQLException {
        List<PriceListForm.Line> lines = new ArrayList<>();
        for (PriceListItem item : priceListDao.findItems(list.getId(), false)) {
            lines.add(new PriceListForm.Line(item.getProductId(), money(item.getPrice()), money(item.getFloorPrice())));
        }
        return new PriceListForm(list.getCustomerGroupId(), list.getName(), validFrom, validTo, lines);
    }

    // Ngày bắt đầu sớm nhất của phiên bản mới: sau hôm nay và sau ngày bắt đầu của bản cũ
    public static LocalDate earliestVersionStart(PriceList previous) {
        LocalDate tomorrow = DateTimeUtil.today().plusDays(1);
        LocalDate afterPrevious = previous.getValidFrom().plusDays(1);
        return tomorrow.isAfter(afterPrevious) ? tomorrow : afterPrevious;
    }

    // Lỗi theo tên ô (customerGroupId, name, validFrom, validTo, lines, lines.N); rỗng = hợp lệ.
    // editing: bảng đang sửa (null khi thêm). previous: bản cũ khi tạo phiên bản mới (null nếu không phải).
    public Map<String, String> validate(PriceListForm form, PriceList editing, PriceList previous)
            throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getCustomerGroupId() == null) {
            errors.put("customerGroupId", "Vui lòng chọn nhóm khách hàng.");
        } else if (!priceListDao.isActiveGroup(form.getCustomerGroupId())) {
            errors.put("customerGroupId", "Nhóm khách hàng không hợp lệ.");
        }

        if (form.getName() == null) {
            errors.put("name", "Vui lòng nhập tên bảng giá.");
        } else if (form.getName().length() > NAME_MAX_LENGTH) {
            errors.put("name", "Tên bảng giá tối đa " + NAME_MAX_LENGTH + " ký tự.");
        }

        LocalDate from = parseDate(form.getValidFrom());
        LocalDate to = parseDate(form.getValidTo());
        if (from == null) {
            errors.put("validFrom", "Vui lòng nhập ngày bắt đầu hiệu lực.");
        } else if (previous != null && from.isBefore(earliestVersionStart(previous))) {
            errors.put("validFrom", "Phiên bản mới phải bắt đầu từ ngày "
                    + DateTimeUtil.formatDay(earliestVersionStart(previous)) + " trở đi.");
        }
        if (to == null) {
            errors.put("validTo", "Vui lòng nhập ngày kết thúc hiệu lực.");
        } else if (from != null && to.isBefore(from)) {
            errors.put("validTo", "Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }

        if (!errors.containsKey("customerGroupId") && from != null && to != null && !errors.containsKey("validFrom")
                && !errors.containsKey("validTo")) {
            List<Long> exclude = new ArrayList<>();
            if (editing != null) {
                exclude.add(editing.getId());
            }
            if (previous != null && previous.getCustomerGroupId() == form.getCustomerGroupId()) {
                // Bản cũ sẽ được rút ngắn để kết thúc hôm trước ngày bản mới bắt đầu
                exclude.add(previous.getId());
            }
            PriceList overlap = priceListDao.findOverlap(form.getCustomerGroupId(), from, to, exclude);
            if (overlap != null) {
                errors.put("validFrom", "Nhóm này đã có bảng giá " + overlap.getCode() + " hiệu lực "
                        + overlap.getValidFromText() + " – " + overlap.getValidToText()
                        + ". Mỗi nhóm mỗi ngày chỉ có một bảng giá.");
            }
        }

        validateLines(form.getLines(), errors);
        return errors;
    }

    private void validateLines(List<PriceListForm.Line> lines, Map<String, String> errors) throws SQLException {
        if (lines.isEmpty()) {
            errors.put("lines", "Vui lòng nhập giá cho ít nhất một sản phẩm.");
            return;
        }
        if (lines.size() > MAX_LINES) {
            errors.put("lines", "Một bảng giá tối đa " + MAX_LINES + " dòng.");
            return;
        }
        Set<Long> products = new HashSet<>();
        Set<Long> pricing = new HashSet<>();
        for (PriceListItem product : priceListDao.findPricingProducts(false)) {
            pricing.add(product.getProductId());
        }
        for (int i = 0; i < lines.size(); i++) {
            PriceListForm.Line line = lines.get(i);
            String key = "lines." + i;
            if (line.getProductId() == null) {
                errors.put(key, "Chọn sản phẩm.");
                continue;
            }
            if (!pricing.contains(line.getProductId())) {
                errors.put(key, "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.");
                continue;
            }
            if (!products.add(line.getProductId())) {
                errors.put(key, "Sản phẩm này đã có ở dòng khác.");
                continue;
            }
            BigDecimal price = parseMoney(line.getPrice());
            BigDecimal floor = parseMoney(line.getFloorPrice());
            if (price == null) {
                errors.put(key, "Giá bán là số tiền nguyên, không âm.");
            } else if (floor == null) {
                errors.put(key, "Giá sàn là số tiền nguyên, không âm.");
            } else if (floor.compareTo(price) > 0) {
                errors.put(key, "Giá sàn không được cao hơn giá bán.");
            }
        }
    }

    // Gọi validate trước. Mã tự sinh BG001, BG002...
    public long create(PriceListForm form, long actorUserId, String ipAddress) throws SQLException {
        String code = CODE_PREFIX + String.format("%03d", priceListDao.findMaxCodeNumber() + 1);
        Map<Long, PriceListItem> products = productsById();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LocalDate from = parseDate(form.getValidFrom());
                long id = priceListDao.insert(connection, code, form.getName(), form.getCustomerGroupId(), 1, null,
                        from, parseDate(form.getValidTo()), actorUserId);
                insertLines(connection, id, form.getLines(), products, null, from, actorUserId);
                auditLogDao.insert(connection, actorUserId, "PRICE_LIST_CREATE", ENTITY, id, null,
                        toJson(code, form, products), null, ipAddress);
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Gọi validate trước. Trả về false nếu bảng vừa bị khoá (đã có đơn dùng) nên không sửa được.
    public boolean update(PriceList editing, PriceListForm form, long actorUserId, String ipAddress)
            throws SQLException {
        Map<Long, PriceListItem> products = productsById();
        Map<Long, PriceListItem> oldItems = itemsByProduct(priceListDao.findItems(editing.getId(), false));
        String oldJson = toJson(editing, oldItems.values());
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LocalDate from = parseDate(form.getValidFrom());
                if (!priceListDao.update(connection, editing.getId(), form.getName(), form.getCustomerGroupId(), from,
                        parseDate(form.getValidTo()), actorUserId)) {
                    connection.rollback();
                    return false;
                }
                priceListDao.deleteItems(connection, editing.getId());
                insertLines(connection, editing.getId(), form.getLines(), products, oldItems, from, actorUserId);
                auditLogDao.insert(connection, actorUserId, "PRICE_LIST_UPDATE", ENTITY, editing.getId(), oldJson,
                        toJson(editing.getCode(), form, products), null, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Gọi validate (kèm previous) trước. Mã phiên bản: mã gốc + "-Vn" (BG001-V2).
    public long createVersion(PriceList previous, PriceListForm form, long actorUserId, String ipAddress)
            throws SQLException {
        Map<Long, PriceListItem> products = productsById();
        Map<Long, PriceListItem> oldItems = itemsByProduct(priceListDao.findItems(previous.getId(), false));
        int versionNo = previous.getVersionNo() + 1;
        String code = versionCode(previous.getCode(), versionNo);
        while (priceListDao.existsCode(code)) {
            versionNo++;
            code = versionCode(previous.getCode(), versionNo);
        }
        LocalDate from = parseDate(form.getValidFrom());
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Rút ngắn bản cũ trước khi thêm bản mới: trigger trg_price_lists_no_overlap_* chặn hai bảng
                // ACTIVE cùng nhóm trùng ngày ngay ở từng câu lệnh, kể cả trong cùng transaction
                LocalDate dayBefore = from.minusDays(1);
                boolean shortened = previous.getValidTo() == null || previous.getValidTo().isAfter(dayBefore);
                if (shortened) {
                    priceListDao.updateValidTo(connection, previous.getId(), dayBefore, actorUserId);
                }
                long id = priceListDao.insert(connection, code, form.getName(), form.getCustomerGroupId(), versionNo,
                        previous.getId(), from, parseDate(form.getValidTo()), actorUserId);
                insertLines(connection, id, form.getLines(), products, oldItems, from, actorUserId);
                Map<String, Object> created = new LinkedHashMap<>(toValues(code, form, products));
                created.put("previousCode", previous.getCode());
                auditLogDao.insert(connection, actorUserId, "PRICE_LIST_VERSION", ENTITY, id,
                        toJson(previous, oldItems.values()), JsonUtil.object(created), null, ipAddress);
                if (shortened) {
                    auditLogDao.insert(connection, actorUserId, "PRICE_LIST_SHORTEN", ENTITY, previous.getId(),
                            JsonUtil.object(Map.of("validTo", String.valueOf(previous.getValidTo()))),
                            JsonUtil.object(Map.of("validTo", dayBefore.toString(), "nextCode", code)),
                            null, ipAddress);
                }
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // null nếu xoá được; ngược lại là lý do hiện cho người dùng
    public String validateDelete(PriceList list) throws SQLException {
        if (list.isLocked()) {
            return "Bảng giá " + list.getCode() + " đã có đơn sử dụng nên không xoá được.";
        }
        if (priceListDao.hasNextVersion(list.getId())) {
            return "Bảng giá " + list.getCode() + " đã có phiên bản sau nên không xoá được.";
        }
        if (priceListDao.hasHistory(list.getId())) {
            // price_history chỉ được thêm, không xoá (trigger), nên bảng đã đổi giá phải giữ lại
            return "Bảng giá " + list.getCode() + " đã có lịch sử thay đổi giá nên không xoá được. "
                    + "Hãy sửa ngày kết thúc nếu không dùng nữa.";
        }
        return null;
    }

    // Trả về false nếu ngay lúc xoá bảng giá đã bị khoá hoặc có phiên bản sau
    public boolean delete(PriceList list, long actorUserId, String ipAddress) throws SQLException {
        String oldJson = toJson(list, priceListDao.findItems(list.getId(), false));
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!priceListDao.delete(connection, list.getId())) {
                    connection.rollback();
                    return false;
                }
                auditLogDao.insert(connection, actorUserId, "PRICE_LIST_DELETE", ENTITY, list.getId(), oldJson, null,
                        null, ipAddress);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Ghi dòng giá + lịch sử giá cho dòng mới hoặc đổi giá so với oldItems. oldItems = null khi tạo bảng mới:
    // giá ban đầu đã nằm trong dòng giá, chưa ghi lịch sử để bảng tạo nhầm còn xoá được.
    private void insertLines(Connection connection, long priceListId, List<PriceListForm.Line> lines,
                             Map<Long, PriceListItem> products, Map<Long, PriceListItem> oldItems,
                             LocalDate effectiveFrom, long actorUserId) throws SQLException {
        for (PriceListForm.Line line : lines) {
            PriceListItem product = products.get(line.getProductId());
            BigDecimal price = parseMoney(line.getPrice());
            BigDecimal floor = parseMoney(line.getFloorPrice());
            priceListDao.insertItem(connection, priceListId, product.getProductId(), product.getUnitId(), price, floor,
                    actorUserId);
            if (oldItems == null) {
                continue;
            }
            PriceListItem old = oldItems.get(product.getProductId());
            boolean changed = old == null || old.getPrice().compareTo(price) != 0
                    || old.getFloorPrice().compareTo(floor) != 0;
            if (changed) {
                priceListDao.insertHistory(connection, priceListId, product.getProductId(), product.getUnitId(),
                        old == null ? null : old.getPrice(), price, old == null ? null : old.getFloorPrice(), floor,
                        effectiveFrom, actorUserId);
            }
        }
    }

    private Map<Long, PriceListItem> productsById() throws SQLException {
        return itemsByProduct(priceListDao.findPricingProducts(false));
    }

    private static Map<Long, PriceListItem> itemsByProduct(List<PriceListItem> items) {
        Map<Long, PriceListItem> byId = new LinkedHashMap<>();
        items.forEach(item -> byId.put(item.getProductId(), item));
        return byId;
    }

    static String versionCode(String code, int versionNo) {
        String base = code.replaceFirst("-V\\d+$", "");
        return base + "-V" + versionNo;
    }

    static LocalDate parseDate(String value) {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Tiền đồng, số nguyên; chấp nhận dấu phân cách hàng nghìn. Ô trống = không hợp lệ (giá phải nhập).
    static BigDecimal parseMoney(String value) {
        return value == null ? null : ProductService.parseCostPrice(value);
    }

    private static String money(BigDecimal value) {
        return value == null ? null : value.toBigInteger().toString();
    }

    // Giá trị ghi nhật ký (S2-04): dòng giá ghi theo SKU để đọc được khi sản phẩm đã đổi tên
    private static String toJson(String code, PriceListForm form, Map<Long, PriceListItem> products) {
        return JsonUtil.object(toValues(code, form, products));
    }

    private static Map<String, Object> toValues(String code, PriceListForm form, Map<Long, PriceListItem> products) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("name", form.getName());
        values.put("customerGroupId", form.getCustomerGroupId());
        values.put("validFrom", form.getValidFrom());
        values.put("validTo", form.getValidTo());
        List<String> lines = new ArrayList<>();
        for (PriceListForm.Line line : form.getLines()) {
            lines.add(products.get(line.getProductId()).getSku() + ": " + parseMoney(line.getPrice()) + " / sàn "
                    + parseMoney(line.getFloorPrice()));
        }
        values.put("items", lines);
        return values;
    }

    private static String toJson(PriceList list, Iterable<PriceListItem> items) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", list.getCode());
        values.put("name", list.getName());
        values.put("customerGroupId", list.getCustomerGroupId());
        values.put("validFrom", String.valueOf(list.getValidFrom()));
        values.put("validTo", String.valueOf(list.getValidTo()));
        List<String> lines = new ArrayList<>();
        for (PriceListItem item : items) {
            lines.add(item.getSku() + ": " + item.getPrice().toBigInteger() + " / sàn "
                    + item.getFloorPrice().toBigInteger());
        }
        values.put("items", lines);
        return JsonUtil.object(values);
    }
}
