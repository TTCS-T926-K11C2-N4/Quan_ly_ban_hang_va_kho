package com.oms.controller;

import com.oms.dao.SalesOrderDao;
import com.oms.model.Customer;
import com.oms.model.DeliveryAddress;
import com.oms.model.OrderForm;
import com.oms.model.OrderProduct;
import com.oms.model.OrderQuote;
import com.oms.model.Permission;
import com.oms.service.CustomerService;
import com.oms.service.SalesOrderService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// S3-09: màn tạo đơn hàng. GET mở form trống hoặc mở lại đơn nháp (?draft=id);
// POST action=draft lưu nháp, action=submit gửi đơn (chuyển Chờ duyệt).
@WebServlet("/orders/new")
public class OrderNewServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/orders/order-form.jsp";
    static final String FLASH_MESSAGE = "orderFlashMessage";

    private final CustomerService customerService = new CustomerService();
    private final SalesOrderService orderService = new SalesOrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        try {
            OrderForm form = OrderForm.empty();
            SalesOrderDao.Draft draft = null;
            String draftParam = request.getParameter("draft");
            if (draftParam != null) {
                Long draftId = AccountFormParser.parseId(draftParam);
                draft = draftId == null ? null : orderService.findVisibleDraft(userId, draftId);
                if (draft == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                form = draft.getForm();
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            show(request, response, form, draft, Map.of());
        } catch (SQLException e) {
            log("Không mở được màn tạo đơn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (!"draft".equals(action) && !"submit".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        boolean submit = "submit".equals(action);
        long userId = CurrentUser.get(request).getId();
        OrderForm form = OrderFormParser.parse(request);
        try {
            SalesOrderDao.Draft draft = null;
            if (form.getDraftId() != null) {
                draft = orderService.findVisibleDraft(userId, form.getDraftId());
                if (draft == null) {
                    show(request, response, form, null, Map.of("form", "Đơn nháp này không còn để sửa (đã được gửi"
                            + " hoặc không còn tồn tại). Mở lại đơn nháp khác hoặc tạo đơn mới."));
                    return;
                }
            }
            Long customerId = AccountFormParser.parseId(form.getCustomerId());
            Customer customer = customerId == null ? null
                    : customerService.findVisible(userId, Permission.ORDER_MANAGE, customerId);
            List<DeliveryAddress> addresses = customer == null ? List.of()
                    : orderService.getDeliveryAddresses(customer.getId());
            OrderQuote quote = customer == null ? null
                    : orderService.quote(customer, form.getLines(), orderService.getProducts());
            Map<String, String> errors = orderService.validate(form, customer, draft, submit, addresses, quote);
            if (!errors.isEmpty()) {
                show(request, response, form, draft, errors);
                return;
            }
            SalesOrderService.Saved saved = orderService.save(form, customer, draft, submit, quote, userId,
                    request.getRemoteAddr());
            if (saved == null) {
                show(request, response, form, draft, Map.of("form", "Đơn nháp vừa được cập nhật ở nơi khác."
                        + " Mở lại đơn nháp để xem bản mới nhất trước khi sửa tiếp."));
                return;
            }
            if (submit) {
                request.getSession().setAttribute(FLASH_MESSAGE, "Đã tạo đơn hàng " + saved.getOrderNo() + " cho \""
                        + customer.getName() + "\" và chuyển sang chờ duyệt.");
                response.sendRedirect(request.getContextPath() + "/orders/new");
            } else {
                request.getSession().setAttribute(FLASH_MESSAGE, "Đã lưu nháp đơn " + saved.getOrderNo()
                        + ". Có thể bấm \"Mở lại\" để gõ tiếp bất cứ lúc nào.");
                response.sendRedirect(request.getContextPath() + "/orders/new?draft=" + saved.getId());
            }
        } catch (SQLException e) {
            log("Không lưu được đơn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, OrderForm form,
                      SalesOrderDao.Draft draft, Map<String, String> errors)
            throws ServletException, IOException, SQLException {
        long userId = CurrentUser.get(request).getId();
        // Đại lý đã ngừng giao dịch không chọn được cho đơn mới; đơn nháp cũ của đại lý đó vẫn hiện đại lý để mở lại
        List<Customer> customers = customerService.findVisibleCustomers(userId, Permission.ORDER_MANAGE).stream()
                .filter(c -> c.isActive() || draft != null && c.getId() == draft.getCustomerId())
                .toList();
        List<OrderProduct> products = orderService.getProducts();
        Long customerId = AccountFormParser.parseId(form.getCustomerId());
        Customer customer = customerId == null ? null
                : customers.stream().filter(c -> c.getId() == customerId).findFirst().orElse(null);
        request.setAttribute("customers", customers);
        request.setAttribute("products", products);
        request.setAttribute("productsById", products.stream()
                .collect(Collectors.toMap(product -> String.valueOf(product.getId()), Function.identity())));
        request.setAttribute("selectedCustomer", customer);
        request.setAttribute("addresses", customer == null ? List.of()
                : orderService.getDeliveryAddresses(customer.getId()));
        request.setAttribute("quote", customer == null ? null
                : orderService.quote(customer, form.getLines(), products));
        request.setAttribute("form", form);
        request.setAttribute("draft", draft);
        request.setAttribute("drafts", orderService.findVisibleDrafts(userId));
        request.setAttribute("errors", errors);
        request.setAttribute("today", DateTimeUtil.today().toString());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
