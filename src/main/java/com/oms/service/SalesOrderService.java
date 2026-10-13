package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerCreditDao;
import com.oms.dao.DeliveryAddressDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.SalesOrderDao;
import com.oms.dao.StockDao;
import com.oms.model.Customer;
import com.oms.model.CustomerCredit;
import com.oms.model.DeliveryAddress;
import com.oms.model.DraftOrder;
import com.oms.model.OrderForm;
import com.oms.model.OrderProduct;
import com.oms.model.OrderQuote;
import com.oms.model.OrderStock;
import com.oms.model.Permission;
import com.oms.model.PricingRules;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;
import com.oms.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Tạo đơn hàng cho đại lý (S3-09): chọn đại lý, điểm giao, ngày giao; thêm dòng hàng theo đơn vị tính;
// tính tiền hàng, chiết khấu, tổng phải thu; lưu nháp để mở lại, hoặc gửi đơn (chuyển Chờ duyệt, S4-06).
// Đại lý bị khoá giao dịch (S3-07) hoặc có nợ quá hạn (S4-02) không tạo được đơn mới; đơn nháp đã có vẫn sửa được
// (có cảnh báo). Vượt hạn mức công nợ hoặc dưới giá sàn (S4-01) thì đơn được đánh dấu cần duyệt.
// Gửi đơn thì kiểm và giữ chỗ tồn khả dụng ở kho phục vụ đại lý (S4-03); vượt tồn thì không gửi được.
public class SalesOrderService {

    public static final int MAX_LINES = 200;
    public static final int NOTE_MAX_LENGTH = 1000;
    private static final int DRAFT_LIST_LIMIT = 50;
    private static final int ORDER_NO_ATTEMPTS = 3;
    private static final String ORDER_NO_PREFIX = "DH";
    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    // Số lượng theo đơn vị đã chọn: số dương, tối đa 3 chữ số thập phân như cột qty decimal(18,3)
    private static final Pattern QTY_PATTERN = Pattern.compile("\\d{1,12}([.,]\\d{1,3})?");
    // Đơn giá sửa tay (S4-01): số nguyên VNĐ, có thể ngăn nghìn bằng dấu chấm, tối đa 13 chữ số như decimal(18,2)
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{3}){1,4}|\\d{1,13}");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String AUDIT_ENTITY = "ORDER";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";

    private final SalesOrderDao orderDao = new SalesOrderDao();
    private final DeliveryAddressDao deliveryAddressDao = new DeliveryAddressDao();
    private final CustomerService customerService = new CustomerService();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();
    private final CustomerCreditDao creditDao = new CustomerCreditDao();
    private final StockDao stockDao = new StockDao();

    public List<OrderProduct> getProducts() throws SQLException {
        return orderDao.findOrderableProducts();
    }

    public List<DeliveryAddress> getDeliveryAddresses(long customerId) throws SQLException {
        return deliveryAddressDao.findDeliveryAddresses(customerId);
    }

    // Đơn nháp cho hộp "Mở lại": Nhân viên kinh doanh chỉ thấy đơn của đại lý mình phụ trách
    public List<DraftOrder> findVisibleDrafts(long userId) throws SQLException {
        String scope = permissionDao.findScope(userId, Permission.ORDER_MANAGE);
        if ("ALL".equals(scope)) {
            return orderDao.findDrafts(null, DRAFT_LIST_LIMIT);
        }
        return "ASSIGNED".equals(scope) ? orderDao.findDrafts(userId, DRAFT_LIST_LIMIT) : List.of();
    }

    // null nếu không có đơn nháp này, đơn đã được gửi, hoặc người dùng không được xem đại lý của đơn
    public SalesOrderDao.Draft findVisibleDraft(long userId, long orderId) throws SQLException {
        SalesOrderDao.Draft draft = orderDao.findDraft(orderId);
        if (draft == null
                || customerService.findVisible(userId, Permission.ORDER_MANAGE, draft.getCustomerId()) == null) {
            return null;
        }
        return draft;
    }

    // Giá theo bảng giá đang hiệu lực hôm nay của nhóm khách hàng mà đại lý thuộc về
    public OrderQuote quote(Customer customer, List<OrderForm.Line> lines, List<OrderProduct> products)
            throws SQLException {
        PricingRules rules = orderDao.findPricingRules(customer.getCustomerGroupId(), DateTimeUtil.today());
        return price(lines, products.stream().collect(Collectors.toMap(OrderProduct::getId, Function.identity())),
                rules);
    }

    // Công nợ hiện tại của đại lý (S4-02), null nếu chưa chọn đại lý
    public CustomerCredit credit(Customer customer) throws SQLException {
        return customer == null ? null : creditDao.find(customer.getId(), DateTimeUtil.today());
    }

    // Lỗi theo tên ô ("lines.i" là dòng thứ i, "form" là lỗi chung); rỗng nghĩa là hợp lệ.
    // draft: đơn nháp đang mở lại (null khi tạo mới); submit: bấm "Tạo đơn hàng" (true) hay "Lưu nháp".
    public Map<String, String> validate(OrderForm form, Customer customer, SalesOrderDao.Draft draft,
                                        boolean submit, List<DeliveryAddress> addresses, OrderQuote quote,
                                        CustomerCredit credit, OrderStock stock) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (customer == null) {
            errors.put("customerId", "Vui lòng chọn đại lý.");
        } else if (draft == null || draft.getCustomerId() != customer.getId()) {
            // AC1 của S3-07: đại lý bị khoá không tạo được đơn mới (kể cả trên cổng đặt hàng)
            if (customer.isBlocked()) {
                errors.put("customerId", "Đại lý đang bị khoá giao dịch nên không tạo được đơn mới.");
            } else if (!customer.isActive()) {
                errors.put("customerId", "Đại lý đã ngừng giao dịch.");
            } else if (credit != null && credit.isOverdue()) {
                // AC3 của S4-02: có khoản quá hạn thì chặn hẳn đơn mới, kể cả lưu nháp
                errors.put("customerId", overdueMessage(credit) + " nên không tạo được đơn mới.");
            }
        } else if (submit && credit != null && credit.isOverdue()) {
            // Đơn nháp đã có vẫn lưu được (như đại lý bị khoá) nhưng chưa gửi được cho tới khi trả khoản quá hạn
            errors.put("form", overdueMessage(credit) + " nên chưa gửi được đơn. Đơn nháp vẫn lưu được;"
                    + " gửi lại sau khi đại lý thanh toán khoản quá hạn.");
        }
        if (customer != null && customer.getDefaultWarehouseId() == null) {
            errors.put("form", "Đại lý chưa có kho phục vụ mặc định nên chưa tạo được đơn. Vui lòng cập nhật"
                    + " hồ sơ đại lý.");
        }

        Long addressId = parseId(form.getDeliveryAddressId());
        if (addressId == null) {
            errors.put("deliveryAddressId", "Vui lòng chọn điểm giao.");
        } else if (addresses.stream().noneMatch(address -> address.getId() == addressId)) {
            errors.put("deliveryAddressId", "Điểm giao không thuộc đại lý đã chọn.");
        }

        if (form.getRequestedDate() == null) {
            if (submit) {
                errors.put("requestedDate", "Vui lòng chọn ngày giao mong muốn.");
            }
        } else {
            LocalDate date = parseDate(form.getRequestedDate());
            if (date == null) {
                errors.put("requestedDate", "Ngày giao không hợp lệ.");
            } else if (date.isBefore(DateTimeUtil.today())) {
                errors.put("requestedDate", "Ngày giao không được trước hôm nay.");
            }
        }

        if (form.getNote() != null && form.getNote().length() > NOTE_MAX_LENGTH) {
            errors.put("note", "Ghi chú tối đa " + NOTE_MAX_LENGTH + " ký tự.");
        }

        if (form.getLines().size() > MAX_LINES) {
            errors.put("lines", "Một đơn tối đa " + MAX_LINES + " dòng hàng.");
        } else if (submit && form.getLines().isEmpty()) {
            errors.put("lines", "Đơn hàng cần ít nhất một dòng hàng.");
        }
        if (quote != null) {
            for (int i = 0; i < quote.getLines().size(); i++) {
                String error = quote.getLines().get(i).getError();
                if (error != null) {
                    errors.put("lines." + i, error);
                }
            }
        }
        // AC3 của S4-03: chỉ chặn khi gửi đơn; lưu nháp vẫn được, dòng vượt tồn chỉ hiện cảnh báo
        if (submit && stock != null) {
            errors.putAll(shortageErrors(stock));
        }
        return errors;
    }

    // Tồn khả dụng ở kho phục vụ đại lý cho các dòng đang gõ (S4-03), null nếu chưa chọn đại lý hoặc đại lý chưa có kho
    public OrderStock stock(Customer customer, OrderQuote quote) throws SQLException {
        if (customer == null || quote == null || customer.getDefaultWarehouseId() == null) {
            return null;
        }
        long warehouseId = customer.getDefaultWarehouseId();
        return checkStock(quote, stockDao.findLevels(warehouseId), stockDao.findWarehouseName(warehouseId));
    }

    // Đối chiếu số đặt với tồn (không đọc CSDL nên test được). Cộng chung các dòng cùng SKU theo đơn vị cơ sở.
    static OrderStock checkStock(OrderQuote quote, Map<Long, OrderStock.Level> levels, String warehouseName) {
        Map<Long, BigDecimal> requested = new HashMap<>();
        for (OrderQuote.Line line : quote.getLines()) {
            if (line.getError() == null) {
                requested.merge(line.getProduct().getId(), line.getQtyBase(), BigDecimal::add);
            }
        }
        List<OrderStock.Line> lines = new ArrayList<>();
        for (OrderQuote.Line line : quote.getLines()) {
            if (line.getError() != null) {
                lines.add(OrderStock.Line.unchecked());
                continue;
            }
            OrderStock.Level level = levels.get(line.getProduct().getId());
            if (level == null) {
                lines.add(OrderStock.Line.untracked());
                continue;
            }
            BigDecimal total = requested.get(line.getProduct().getId());
            BigDecimal room = level.getAvailable().subtract(total.subtract(line.getQtyBase()));
            lines.add(OrderStock.Line.tracked(level.getAvailable(), room, total.compareTo(level.getAvailable()) > 0,
                    line.getProduct(), line.getUnit()));
        }
        return new OrderStock(warehouseName, lines);
    }

    private static Map<String, String> shortageErrors(OrderStock stock) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (int i = 0; i < stock.getLines().size(); i++) {
            OrderStock.Line line = stock.getLines().get(i);
            if (line.isShortage()) {
                errors.put("lines." + i, line.getShortageMessage(stock.getWarehouseName()));
            }
        }
        return errors;
    }

    // AC4 của S4-03: lúc gửi đơn, sau khi khoá dòng tồn mới thấy hết hàng (có đơn khác vừa gửi cùng SKU)
    public static final class StockShortageException extends Exception {
        private final Map<String, String> errors;

        StockShortageException(Map<String, String> errors) {
            super("Không đủ tồn khả dụng");
            this.errors = errors;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }

    public static String overdueMessage(CustomerCredit credit) {
        return "Đại lý có " + MoneyUtil.format(credit.getOverdueAmount()) + " ₫ nợ quá hạn (hạn trả từ "
                + credit.getOldestDueDateText() + ")";
    }

    // Đơn vừa lưu: id để mở lại đơn nháp, mã đơn để báo cho người dùng
    public static final class Saved {
        private final long id;
        private final String orderNo;

        Saved(long id, String orderNo) {
            this.id = id;
            this.orderNo = orderNo;
        }

        public long getId() {
            return id;
        }

        public String getOrderNo() {
            return orderNo;
        }
    }

    // Gọi validate trước. null nếu đơn nháp vừa được sửa hoặc gửi ở nơi khác (version đã đổi).
    // Gửi đơn thì giữ chỗ tồn cho từng dòng (S4-03); StockShortageException nếu tồn vừa bị đơn khác giữ mất.
    public Saved save(OrderForm form, Customer customer, SalesOrderDao.Draft draft, boolean submit,
                       OrderQuote quote, long actorUserId, String ipAddress)
            throws SQLException, StockShortageException {
        Long warehouseId = customer.getDefaultWarehouseId();
        Pending pending = new Pending(form, customer, submit, quote, ipAddress,
                warehouseId == null ? null : stockDao.findWarehouseName(warehouseId));
        if (draft != null) {
            return updateDraft(draft, parseVersion(form.getVersion()), pending, actorUserId)
                    ? new Saved(draft.getForm().getDraftId(), draft.getOrderNo()) : null;
        }
        String prefix = ORDER_NO_PREFIX + DateTimeUtil.today().format(ORDER_NO_DATE) + "-";
        for (int attempt = 1; ; attempt++) {
            String orderNo = prefix + String.format("%04d", orderDao.findMaxOrderNumber(prefix) + 1);
            try {
                return new Saved(insert(orderNo, pending, actorUserId), orderNo);
            } catch (SQLIntegrityConstraintViolationException e) {
                // Hai người cùng tạo đơn một lúc lấy trùng số: thử lại với số kế tiếp
                if (attempt == ORDER_NO_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    // Đơn sắp ghi. Cờ cần duyệt chỉ chốt được trong transaction, sau khi khoá đại lý và cộng lại công nợ (S4-02)
    private static final class Pending {
        private final OrderForm form;
        private final Customer customer;
        private final boolean submit;
        private final OrderQuote quote;
        private final String ipAddress;
        private final String warehouseName;

        Pending(OrderForm form, Customer customer, boolean submit, OrderQuote quote, String ipAddress,
                String warehouseName) {
            this.form = form;
            this.customer = customer;
            this.submit = submit;
            this.quote = quote;
            this.ipAddress = ipAddress;
            this.warehouseName = warehouseName;
        }

        SalesOrderDao.Header header(boolean overCreditLimit) {
            return new SalesOrderDao.Header(customer.getId(), parseId(form.getDeliveryAddressId()),
                    customer.getDefaultWarehouseId(), customer.getSalesRepId(), submit,
                    form.getRequestedDate() == null ? null : parseDate(form.getRequestedDate()), quote.getSubtotal(),
                    quote.getDiscount(), quote.getTotal(), form.getNote(),
                    quote.isNeedsApproval() || overCreditLimit);
        }

        String newValues(String orderNo, boolean overCreditLimit, Map<Long, OrderStock.Level> reserving) {
            Map<String, Object> values = orderValues(orderNo, customer.getCode(), submit, form, quote,
                    overCreditLimit);
            values.put("reservedLines", quote.getLines().stream()
                    .filter(line -> reserving.containsKey(line.getProduct().getId())).count());
            return JsonUtil.object(values);
        }
    }

    // S4-03: gửi đơn thì khoá dòng tồn các SKU của đơn và kiểm lại; trả về tồn của các SKU sẽ được giữ chỗ
    // (SKU chưa theo dõi tồn ở kho thì không giữ chỗ). Lưu nháp không đụng tới tồn.
    private Map<Long, OrderStock.Level> lockStock(Connection connection, Pending pending)
            throws SQLException, StockShortageException {
        Long warehouseId = pending.customer.getDefaultWarehouseId();
        if (!pending.submit || warehouseId == null) {
            return Map.of();
        }
        List<Long> productIds = pending.quote.getLines().stream().map(line -> line.getProduct().getId()).toList();
        Map<Long, OrderStock.Level> levels = stockDao.lockLevels(connection, warehouseId, productIds);
        Map<String, String> errors = shortageErrors(checkStock(pending.quote, levels, pending.warehouseName));
        if (!errors.isEmpty()) {
            errors.put("form", "Tồn kho vừa thay đổi do có đơn khác được gửi cùng lúc. Vui lòng xem lại số lượng"
                    + " các dòng báo đỏ.");
            throw new StockShortageException(errors);
        }
        return levels;
    }

    private void reserve(Connection connection, Pending pending, List<Long> itemIds,
                         Map<Long, OrderStock.Level> reserving, long actorUserId) throws SQLException {
        for (int i = 0; i < itemIds.size(); i++) {
            OrderQuote.Line line = pending.quote.getLines().get(i);
            if (reserving.containsKey(line.getProduct().getId())) {
                stockDao.reserve(connection, itemIds.get(i), pending.customer.getDefaultWarehouseId(),
                        line.getProduct().getId(), line.getQtyBase(), actorUserId);
            }
        }
    }

    // AC2 của S4-02: khoá dòng đại lý rồi mới so tổng đơn + công nợ với hạn mức. Gọi trước khi ghi đơn,
    // để đơn nháp đang gửi chưa bị tính vào "đơn chưa xuất hoá đơn".
    private boolean overCreditLimit(Connection connection, Pending pending) throws SQLException {
        CustomerCredit credit = creditDao.lock(connection, pending.customer.getId(), DateTimeUtil.today());
        return credit != null && credit.exceeds(pending.quote.getTotal());
    }

    private long insert(String orderNo, Pending pending, long actorUserId)
            throws SQLException, StockShortageException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean overLimit = overCreditLimit(connection, pending);
                Map<Long, OrderStock.Level> reserving = lockStock(connection, pending);
                SalesOrderDao.Header header = pending.header(overLimit);
                AuditEntry audit = new AuditEntry("ORDER_CREATE", null,
                        pending.newValues(orderNo, overLimit, reserving), pending.ipAddress);
                long orderId = orderDao.insert(connection, orderNo, header, actorUserId);
                reserve(connection, pending, insertItems(connection, orderId, pending.quote, actorUserId), reserving,
                        actorUserId);
                // Vòng đời đơn (S4-06): ghi bước đầu tiên ngay khi tạo
                orderDao.insertStatusHistory(connection, orderId, null,
                        header.isSubmitted() ? STATUS_PENDING_APPROVAL : STATUS_DRAFT,
                        header.isSubmitted() ? "Tạo đơn và gửi duyệt" : "Tạo đơn nháp", actorUserId);
                insertAudit(connection, actorUserId, orderId, audit);
                connection.commit();
                return orderId;
            } catch (SQLException | StockShortageException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private boolean updateDraft(SalesOrderDao.Draft draft, long version, Pending pending, long actorUserId)
            throws SQLException, StockShortageException {
        long orderId = draft.getForm().getDraftId();
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean overLimit = overCreditLimit(connection, pending);
                Map<Long, OrderStock.Level> reserving = lockStock(connection, pending);
                SalesOrderDao.Header header = pending.header(overLimit);
                AuditEntry audit = new AuditEntry(pending.submit ? "ORDER_SUBMIT" : "ORDER_UPDATE",
                        JsonUtil.object(draftValues(draft)), pending.newValues(draft.getOrderNo(), overLimit, reserving),
                        pending.ipAddress);
                if (!orderDao.updateDraft(connection, orderId, version, header, actorUserId)) {
                    connection.rollback();
                    return false;
                }
                orderDao.deleteItems(connection, orderId);
                reserve(connection, pending, insertItems(connection, orderId, pending.quote, actorUserId), reserving,
                        actorUserId);
                if (header.isSubmitted()) {
                    orderDao.insertStatusHistory(connection, orderId, STATUS_DRAFT, STATUS_PENDING_APPROVAL,
                            "Gửi duyệt", actorUserId);
                }
                insertAudit(connection, actorUserId, orderId, audit);
                connection.commit();
                return true;
            } catch (SQLException | StockShortageException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Nhật ký thao tác trên đơn (S2-04), ghi cùng transaction với lần lưu đơn
    private static final class AuditEntry {
        private final String action;
        private final String oldValues;
        private final String newValues;
        private final String ipAddress;

        AuditEntry(String action, String oldValues, String newValues, String ipAddress) {
            this.action = action;
            this.oldValues = oldValues;
            this.newValues = newValues;
            this.ipAddress = ipAddress;
        }
    }

    private void insertAudit(Connection connection, long actorUserId, long orderId, AuditEntry audit)
            throws SQLException {
        auditLogDao.insert(connection, actorUserId, audit.action, AUDIT_ENTITY, orderId, audit.oldValues,
                audit.newValues, null, audit.ipAddress);
    }

    // Giá trị ghi nhật ký: phần đầu đơn và tổng tiền; dòng hàng chỉ ghi số dòng (chi tiết đã có ở sales_order_items),
    // không có giá vốn
    static Map<String, Object> orderValues(String orderNo, String customerCode, boolean submit, OrderForm form,
                                           OrderQuote quote, boolean overCreditLimit) {
        Map<String, Object> values = values(orderNo, customerCode, submit ? STATUS_PENDING_APPROVAL : STATUS_DRAFT,
                form.getDeliveryAddressId(), form.getRequestedDate(), quote.getLines().size(), quote.getSubtotal(),
                quote.getDiscount(), quote.getTotal());
        values.put("manualPriceLines", quote.getLines().stream().filter(OrderQuote.Line::isManualPrice).count());
        values.put("belowFloorLines", quote.getBelowFloorLines().size());
        values.put("overCreditLimit", overCreditLimit);
        values.put("needsApproval", quote.isNeedsApproval() || overCreditLimit);
        return values;
    }

    static Map<String, Object> draftValues(SalesOrderDao.Draft draft) {
        OrderForm form = draft.getForm();
        Map<String, Object> values = values(draft.getOrderNo(), draft.getCustomerCode(), STATUS_DRAFT,
                form.getDeliveryAddressId(), form.getRequestedDate(), form.getLines().size(), draft.getSubtotal(),
                draft.getDiscount(), draft.getTotal());
        values.put("manualPriceLines", form.getLines().stream().filter(line -> line.getPrice() != null).count());
        return values;
    }

    private static Map<String, Object> values(String orderNo, String customerCode, String status,
                                              String deliveryAddressId, String requestedDate, int lineCount,
                                              BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("orderNo", orderNo);
        values.put("customerCode", customerCode);
        values.put("status", status);
        values.put("deliveryAddressId", parseId(deliveryAddressId));
        values.put("requestedDate", requestedDate);
        values.put("lineCount", lineCount);
        values.put("subtotal", moneyText(subtotal));
        values.put("discount", moneyText(discount));
        values.put("total", moneyText(total));
        return values;
    }

    private static String moneyText(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros().toPlainString();
    }

    // id các dòng vừa ghi, cùng thứ tự với quote.getLines()
    private List<Long> insertItems(Connection connection, long orderId, OrderQuote quote, long actorUserId)
            throws SQLException {
        List<Long> ids = new ArrayList<>();
        int lineNo = 1;
        for (OrderQuote.Line line : quote.getLines()) {
            ids.add(orderDao.insertItem(connection, orderId, lineNo++, line, actorUserId));
        }
        return ids;
    }

    // Tính tiền từng dòng (không đọc CSDL nên test được). Giá lấy đúng đơn vị đã chọn; bảng giá chỉ khai báo đơn vị
    // cơ sở thì giá theo đơn vị lớn = giá cơ sở x hệ số quy đổi. Tiền VNĐ làm tròn tới đồng.
    static OrderQuote price(List<OrderForm.Line> lines, Map<Long, OrderProduct> products, PricingRules rules) {
        List<OrderQuote.Line> priced = new ArrayList<>();
        for (OrderForm.Line line : lines) {
            priced.add(priceLine(line, products, rules));
        }
        return new OrderQuote(priced);
    }

    private static OrderQuote.Line priceLine(OrderForm.Line line, Map<Long, OrderProduct> products,
                                             PricingRules rules) {
        Long productId = parseId(line.getProductId());
        if (productId == null) {
            return new OrderQuote.Line("Chọn sản phẩm trong danh sách gợi ý theo mã hoặc tên hàng.");
        }
        OrderProduct product = products.get(productId);
        if (product == null) {
            return new OrderQuote.Line("Sản phẩm đã ngừng kinh doanh hoặc không còn tồn tại.");
        }
        Long unitId = parseId(line.getUnitId());
        OrderProduct.Unit unit = unitId == null ? null : product.findUnit(unitId);
        if (unit == null) {
            return new OrderQuote.Line("Chọn đơn vị tính đã khai báo cho " + product.getSku() + ".");
        }
        BigDecimal qty = parseQty(line.getQty());
        if (qty == null) {
            return new OrderQuote.Line("Số lượng phải lớn hơn 0, tối đa 3 chữ số thập phân.");
        }
        UnitConversionService.Converted converted = UnitConversionService.convert(qty, unit.getFactor());

        PricingRules.PriceItem item = rules.findPrice(product.getId(), unit.getId());
        BigDecimal multiplier = BigDecimal.ONE;
        OrderProduct.Unit base = product.getBaseUnit();
        if (item == null && base != null && !unit.isBase()) {
            item = rules.findPrice(product.getId(), base.getId());
            multiplier = unit.getFactor();
        }
        if (item == null) {
            // S4-01: không có bảng giá hiệu lực cho SKU thì chặn dòng hàng kèm lý do rõ ràng
            return new OrderQuote.Line("Chưa có giá của " + product.getSku()
                    + " trong bảng giá đang hiệu lực của nhóm khách hàng.");
        }
        BigDecimal listPrice = item.getPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal floorPrice = item.getFloorPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        // S4-01: sửa giá thủ công được; dưới giá sàn thì vẫn lưu nhưng dòng bị đánh dấu, đơn phải qua duyệt
        BigDecimal unitPrice = listPrice;
        if (line.getPrice() != null) {
            BigDecimal manual = parseMoney(line.getPrice());
            if (manual == null) {
                return new OrderQuote.Line("Đơn giá phải là số tiền lớn hơn 0, vd 15.500.");
            }
            unitPrice = manual;
        }
        boolean manualPrice = unitPrice.compareTo(listPrice) != 0;
        BigDecimal amount = money(unitPrice.multiply(qty));

        Long bestPolicy = null;
        BigDecimal bestDiscount = BigDecimal.ZERO.setScale(2);
        for (PricingRules.DiscountPolicy policy : rules.getDiscountPolicies()) {
            BigDecimal discount = discountOf(policy, product, converted.getQtyBase(), amount);
            // Nhiều chính sách cùng khớp: lấy chính sách có lợi nhất cho khách (AC3 của S3-01)
            if (discount != null && discount.compareTo(bestDiscount) > 0) {
                bestDiscount = discount;
                bestPolicy = policy.getId();
            }
        }
        return new OrderQuote.Line(product, unit, qty, converted.getQtyBase(), listPrice, unitPrice, floorPrice,
                manualPrice, item.getId(), bestPolicy, amount, bestDiscount);
    }

    // null nếu chính sách không áp cho dòng này (khác SKU/nhóm hàng, hoặc chưa đạt bậc số lượng nhỏ nhất)
    private static BigDecimal discountOf(PricingRules.DiscountPolicy policy, OrderProduct product, BigDecimal qtyBase,
                                         BigDecimal amount) {
        boolean matches = policy.getProductId() != null ? policy.getProductId() == product.getId()
                : policy.coversCategory(product.getCategoryId());
        if (!matches) {
            return null;
        }
        PricingRules.Tier tier = null;
        for (PricingRules.Tier candidate : policy.getTiers()) {
            if (candidate.getMinQtyBase().compareTo(qtyBase) <= 0) {
                tier = candidate;
            }
        }
        if (tier == null) {
            return null;
        }
        BigDecimal discount = policy.isPercent()
                ? amount.multiply(tier.getValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : tier.getValue().multiply(qtyBase);
        return money(discount.min(amount));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).setScale(2);
    }

    // Đơn giá sửa tay: "15500" hoặc "15.500"; null nếu sai định dạng hoặc không lớn hơn 0
    static BigDecimal parseMoney(String text) {
        if (text == null || !MONEY_PATTERN.matcher(text).matches()) {
            return null;
        }
        BigDecimal money = new BigDecimal(text.replace(".", "")).setScale(2);
        return money.signum() > 0 ? money : null;
    }

    static BigDecimal parseQty(String text) {
        if (text == null || !QTY_PATTERN.matcher(text).matches()) {
            return null;
        }
        BigDecimal qty = new BigDecimal(text.replace(',', '.'));
        return qty.signum() > 0 ? qty : null;
    }

    private static Long parseId(String text) {
        try {
            long id = text == null ? 0 : Long.parseLong(text);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Ô ngày gửi dạng yyyy-MM-dd (input type="date")
    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Version sai định dạng coi như đã cũ để không ghi đè
    private static long parseVersion(String text) {
        try {
            return text == null ? -1 : Long.parseLong(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
