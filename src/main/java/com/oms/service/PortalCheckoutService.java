package com.oms.service;

import com.oms.dao.CustomerDao;
import com.oms.dao.PortalOrderDao;
import com.oms.model.Customer;
import com.oms.model.CustomerCredit;
import com.oms.model.DeliveryAddress;
import com.oms.model.OrderForm;
import com.oms.model.OrderProduct;
import com.oms.model.OrderQuote;
import com.oms.model.OrderStock;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Đại lý tự đặt hàng trên cổng (S4-10). Đại lý lấy từ tài khoản đăng nhập; chỉ thấy sản phẩm và giá của nhóm khách
// hàng của mình; không sửa được giá. Xem trước tổng tiền và công nợ còn lại, gửi đơn thì luôn ở Chờ duyệt để nhân viên
// phụ trách xác nhận (không tự duyệt). Dùng chung kiểm tra khoá đại lý, nợ quá hạn, tồn khả dụng với màn tạo đơn.
public class PortalCheckoutService {

    private final PortalOrderDao portalOrderDao = new PortalOrderDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final SalesOrderService orderService = new SalesOrderService();

    // Giỏ hàng đã tính: sản phẩm chọn được, điểm giao, tiền, công nợ, lỗi (rỗng nếu hợp lệ)
    public static final class Cart {
        private final Customer customer;
        private final List<OrderProduct> products;
        private final List<DeliveryAddress> addresses;
        private final OrderForm form;
        private final OrderQuote quote;
        private final CustomerCredit credit;
        private final Map<String, String> errors;

        Cart(Customer customer, List<OrderProduct> products, List<DeliveryAddress> addresses, OrderForm form,
             OrderQuote quote, CustomerCredit credit, Map<String, String> errors) {
            this.customer = customer;
            this.products = products;
            this.addresses = addresses;
            this.form = form;
            this.quote = quote;
            this.credit = credit;
            this.errors = errors;
        }

        public Customer getCustomer() {
            return customer;
        }

        public List<OrderProduct> getProducts() {
            return products;
        }

        public List<DeliveryAddress> getAddresses() {
            return addresses;
        }

        public OrderForm getForm() {
            return form;
        }

        public OrderQuote getQuote() {
            return quote;
        }

        public CustomerCredit getCredit() {
            return credit;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public boolean isOverCreditLimit() {
            return credit != null && quote != null && credit.exceeds(quote.getTotal());
        }
    }

    // Kết quả gửi đơn: saved != null là đã gửi; ngược lại cart mang lỗi để hiện lại
    public static final class Result {
        private final SalesOrderService.Saved saved;
        private final Cart cart;

        Result(SalesOrderService.Saved saved, Cart cart) {
            this.saved = saved;
            this.cart = cart;
        }

        public SalesOrderService.Saved getSaved() {
            return saved;
        }

        public Cart getCart() {
            return cart;
        }
    }

    // null nếu tài khoản chưa gắn đại lý
    public Customer findCustomer(long userId) throws SQLException {
        Long customerId = portalOrderDao.findCustomerId(userId);
        return customerId == null ? null : customerDao.findById(customerId);
    }

    public List<DeliveryAddress> findAddresses(Customer customer) throws SQLException {
        return orderService.getDeliveryAddresses(customer.getId());
    }

    // Giỏ điền sẵn từ đơn cũ (S5-02) cùng các dòng bị loại kèm lý do
    public static final class Reorder {
        private final OrderForm form;
        private final List<String> notices;

        Reorder(OrderForm form, List<String> notices) {
            this.form = form;
            this.notices = notices;
        }

        public OrderForm getForm() {
            return form;
        }

        public List<String> getNotices() {
            return notices;
        }
    }

    // Đặt lại toàn bộ (selected rỗng) hoặc một phần dòng của đơn cũ. Mặt hàng đã ngừng kinh doanh / không còn trong
    // bảng giá, hoặc đơn vị không còn dùng, bị loại kèm thông báo. Giá không lấy từ đơn cũ: giỏ tính lại theo bảng giá
    // hiện hành. null nếu đơn không thuộc đại lý.
    public Reorder buildReorder(Customer customer, long orderId, Set<Integer> selected) throws SQLException {
        PortalOrderDao.Reorderable source = portalOrderDao.findReorderable(customer.getId(), orderId);
        if (source == null) {
            return null;
        }
        Map<Long, OrderProduct> catalog = orderService.getProductsPricedFor(customer).stream()
                .collect(Collectors.toMap(OrderProduct::getId, Function.identity()));
        List<DeliveryAddress> addresses = orderService.getDeliveryAddresses(customer.getId());
        List<OrderForm.Line> lines = new ArrayList<>();
        List<String> notices = new ArrayList<>();
        for (PortalOrderDao.Reorderable.Line line : source.getLines()) {
            if (!selected.isEmpty() && !selected.contains(line.getLineNo())) {
                continue;
            }
            String label = line.getSku() + " - " + line.getName();
            OrderProduct product = catalog.get(line.getProductId());
            if (product == null) {
                notices.add(label + " đã ngừng kinh doanh hoặc chưa có giá, nên không đưa vào đơn mới.");
            } else if (product.findUnit(line.getUnitId()) == null) {
                notices.add(label + " không còn bán theo đơn vị cũ, nên không đưa vào đơn mới.");
            } else {
                lines.add(new OrderForm.Line(String.valueOf(line.getProductId()), label,
                        String.valueOf(line.getUnitId()), line.getQty().stripTrailingZeros().toPlainString(), null));
            }
        }
        boolean addressActive = addresses.stream().anyMatch(a -> a.getId() == source.getDeliveryAddressId());
        if (!addressActive) {
            notices.add("Điểm giao của đơn cũ không còn dùng, vui lòng chọn lại điểm giao.");
        }
        notices.add("Giá và chiết khấu được áp lại theo bảng giá hiện hành.");
        OrderForm form = new OrderForm(String.valueOf(customer.getId()),
                addressActive ? String.valueOf(source.getDeliveryAddressId()) : null, null, null, null, null, lines);
        return new Reorder(form, notices);
    }

    // Làm sạch form của đại lý: đại lý theo tài khoản, không giá sửa tay, không đơn nháp
    static OrderForm sanitize(OrderForm form, Customer customer) {
        List<OrderForm.Line> lines = new ArrayList<>();
        for (OrderForm.Line line : form.getLines()) {
            lines.add(new OrderForm.Line(line.getProductId(), line.getProductText(), line.getUnitId(), line.getQty(),
                    null));
        }
        return new OrderForm(String.valueOf(customer.getId()), form.getDeliveryAddressId(), form.getRequestedDate(),
                form.getNote(), null, null, lines);
    }

    // validate: true khi người dùng bấm gửi đơn (kiểm tra đầy đủ); false khi chỉ xem trước (chỉ lỗi từng dòng)
    public Cart prepare(Customer customer, OrderForm rawForm, boolean validate) throws SQLException {
        OrderForm form = sanitize(rawForm, customer);
        List<OrderProduct> products = orderService.getProductsPricedFor(customer);
        List<DeliveryAddress> addresses = orderService.getDeliveryAddresses(customer.getId());
        OrderQuote quote = orderService.quote(customer, form.getLines(), products);
        CustomerCredit credit = orderService.credit(customer);
        OrderStock stock = validate ? orderService.stock(customer, quote) : null;
        Map<String, String> errors = new LinkedHashMap<>();
        if (validate) {
            errors.putAll(orderService.validate(form, customer, null, true, addresses, quote, credit, stock));
        } else {
            for (int i = 0; i < quote.getLines().size(); i++) {
                String error = quote.getLines().get(i).getError();
                if (error != null) {
                    errors.put("lines." + i, error);
                }
            }
        }
        return new Cart(customer, products, addresses, form, quote, credit, errors);
    }

    // Gửi đơn: luôn Chờ duyệt. Lỗi thì trả cart kèm lỗi.
    public Result submit(Customer customer, OrderForm rawForm, long userId, String ipAddress) throws SQLException {
        Cart cart = prepare(customer, rawForm, true);
        if (!cart.getErrors().isEmpty()) {
            return new Result(null, cart);
        }
        try {
            SalesOrderService.Saved saved = orderService.savePortal(cart.getForm(), customer, cart.getQuote(), userId,
                    ipAddress);
            return new Result(saved, cart);
        } catch (SalesOrderService.StockShortageException e) {
            Map<String, String> errors = new LinkedHashMap<>(e.getErrors());
            return new Result(null, new Cart(customer, cart.getProducts(), cart.getAddresses(), cart.getForm(),
                    cart.getQuote(), cart.getCredit(), errors));
        }
    }
}
