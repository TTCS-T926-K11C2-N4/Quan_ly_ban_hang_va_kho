package com.oms.service;

import com.oms.dao.SalesOrderDao;
import com.oms.model.Customer;
import com.oms.model.DeliveryAddress;
import com.oms.model.OrderForm;
import com.oms.model.OrderProduct;
import com.oms.model.OrderQuote;
import com.oms.model.Permission;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Sao chép đơn hàng (S4-09): từ một đơn có sẵn tạo đơn Nháp mới cùng đại lý, điểm giao, ghi chú và các dòng hàng.
// Giá và chiết khấu luôn tính lại theo bảng giá, chính sách đang hiệu lực (không thừa hưởng giá cũ hay giá sửa tay);
// ngày giao không sao chép. Dòng đã ngừng bán hoặc chưa có giá thì bỏ qua và được báo lại cho người dùng.
public class OrderCopyService {

    private final SalesOrderDao orderDao = new SalesOrderDao();
    private final SalesOrderService orderService = new SalesOrderService();
    private final CustomerService customerService = new CustomerService();

    // Kết quả một lần sao chép: hoặc đơn nháp mới (kèm các dòng bị bỏ), hoặc lý do không sao chép được
    public static final class Result {
        private final SalesOrderService.Saved saved;
        private final String sourceOrderNo;
        private final List<String> skipped;
        private final boolean addressChanged;
        private final String error;

        private Result(SalesOrderService.Saved saved, String sourceOrderNo, List<String> skipped,
                       boolean addressChanged, String error) {
            this.saved = saved;
            this.sourceOrderNo = sourceOrderNo;
            this.skipped = skipped;
            this.addressChanged = addressChanged;
            this.error = error;
        }

        public SalesOrderService.Saved getSaved() {
            return saved;
        }

        public String getSourceOrderNo() {
            return sourceOrderNo;
        }

        public List<String> getSkipped() {
            return skipped;
        }

        public boolean isAddressChanged() {
            return addressChanged;
        }

        public String getError() {
            return error;
        }
    }

    // Dòng sao chép được (còn bán, có giá) và danh sách dòng bị bỏ kèm lý do
    static final class Split {
        private final List<OrderForm.Line> kept = new ArrayList<>();
        private final List<String> skipped = new ArrayList<>();

        List<OrderForm.Line> getKept() {
            return kept;
        }

        List<String> getSkipped() {
            return skipped;
        }
    }

    // Không đọc CSDL nên test được: quote là kết quả tính giá đúng thứ tự với lines
    static Split split(List<OrderForm.Line> lines, OrderQuote quote) {
        Split split = new Split();
        for (int i = 0; i < lines.size(); i++) {
            String error = quote.getLines().get(i).getError();
            if (error == null) {
                split.kept.add(lines.get(i));
            } else {
                split.skipped.add(lines.get(i).getProductText() + ": " + error);
            }
        }
        return split;
    }

    // null nếu không có đơn này hoặc người dùng không được quản lý đơn của đại lý đó (trả 404 như nhau)
    public Result copy(long userId, long orderId, String ipAddress) throws SQLException {
        SalesOrderDao.CopyTemplate source = orderDao.findCopyTemplate(orderId);
        if (source == null) {
            return null;
        }
        Customer customer = customerService.findVisible(userId, Permission.ORDER_MANAGE, source.getCustomerId());
        if (customer == null) {
            return null;
        }
        List<DeliveryAddress> addresses = orderService.getDeliveryAddresses(customer.getId());
        // Điểm giao cũ đã ngừng dùng thì lấy điểm giao mặc định hiện tại (danh sách đã xếp mặc định lên đầu)
        boolean sameAddress = addresses.stream().anyMatch(a -> a.getId() == source.getDeliveryAddressId());
        long addressId = sameAddress ? source.getDeliveryAddressId()
                : addresses.isEmpty() ? 0 : addresses.get(0).getId();

        List<OrderProduct> products = orderService.getProducts();
        Split split = split(source.getLines(), orderService.quote(customer, source.getLines(), products));
        if (split.getKept().isEmpty()) {
            return failure(source, "Không sao chép được đơn " + source.getOrderNo() + ": mọi dòng hàng đều đã ngừng"
                    + " kinh doanh hoặc chưa có giá trong bảng giá hiện hành.");
        }
        OrderForm form = new OrderForm(String.valueOf(customer.getId()),
                addressId == 0 ? null : String.valueOf(addressId), null, source.getNote(), null, null,
                split.getKept());
        OrderQuote quote = orderService.quote(customer, form.getLines(), products);
        Map<String, String> errors = orderService.validate(form, customer, null, false, addresses, quote,
                orderService.credit(customer), null);
        if (!errors.isEmpty()) {
            return failure(source, "Không sao chép được đơn " + source.getOrderNo() + ": "
                    + String.join(" ", errors.values()));
        }
        try {
            SalesOrderService.Saved saved = orderService.save(form, customer, null, false, quote, userId, ipAddress,
                    source);
            return new Result(saved, source.getOrderNo(), split.getSkipped(), !sameAddress, null);
        } catch (SalesOrderService.StockShortageException e) {
            // Lưu nháp không đụng tới tồn nên không xảy ra; vẫn báo lỗi thay vì nuốt
            return failure(source, "Không sao chép được đơn " + source.getOrderNo() + ": "
                    + String.join(" ", e.getErrors().values()));
        }
    }

    private static Result failure(SalesOrderDao.CopyTemplate source, String error) {
        return new Result(null, source.getOrderNo(), List.of(), false, error);
    }
}
