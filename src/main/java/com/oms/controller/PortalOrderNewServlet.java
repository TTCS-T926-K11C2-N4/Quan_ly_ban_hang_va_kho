package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.OrderForm;
import com.oms.model.SessionUser;
import com.oms.service.PortalCheckoutService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

// S4-10: đại lý tự đặt hàng trên cổng. GET mở giỏ hàng (có thể điền sẵn từ "đặt lại đơn cũ", S5-02);
// POST action=preview tính lại tiền và công nợ, action=submit gửi đơn (luôn Chờ duyệt).
@WebServlet("/portal/order/new")
public class PortalOrderNewServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/portal/portal-order-new.jsp";
    // Giỏ điền sẵn do đặt lại đơn cũ, chuyển qua session một lần
    static final String PREFILL_FORM = "portalPrefillForm";
    static final String PREFILL_NOTICES = "portalPrefillNotices";

    private final PortalCheckoutService checkoutService = new PortalCheckoutService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (redirectStaff(user, request, response)) {
            return;
        }
        try {
            Customer customer = checkoutService.findCustomer(user.getId());
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            OrderForm form = OrderForm.empty();
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute(PREFILL_FORM) instanceof OrderForm prefill) {
                form = prefill;
                request.setAttribute("notices", session.getAttribute(PREFILL_NOTICES));
                session.removeAttribute(PREFILL_FORM);
                session.removeAttribute(PREFILL_NOTICES);
            }
            show(request, response, checkoutService.prepare(customer, form, false), form.getLines().isEmpty());
        } catch (SQLException e) {
            log("Không mở được giỏ hàng của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (redirectStaff(user, request, response)) {
            return;
        }
        String action = request.getParameter("action");
        if (!"preview".equals(action) && !"submit".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            Customer customer = checkoutService.findCustomer(user.getId());
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            OrderForm form = OrderFormParser.parse(request);
            if ("preview".equals(action)) {
                show(request, response, checkoutService.prepare(customer, form, false), false);
                return;
            }
            PortalCheckoutService.Result result = checkoutService.submit(customer, form, user.getId(),
                    request.getRemoteAddr());
            if (result.getSaved() == null) {
                show(request, response, result.getCart(), false);
                return;
            }
            request.getSession().setAttribute(PortalOrderViewServlet.FLASH_MESSAGE, "Đã gửi đơn "
                    + result.getSaved().getOrderNo() + ". Đơn đang chờ nhân viên phụ trách xác nhận, bạn sẽ thấy"
                    + " trạng thái cập nhật tại đây.");
            response.sendRedirect(request.getContextPath() + "/portal/orders/view?id=" + result.getSaved().getId());
        } catch (SQLException e) {
            log("Không gửi được đơn của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static boolean redirectStaff(SessionUser user, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (user.isCustomer()) {
            return false;
        }
        response.sendRedirect(request.getContextPath() + user.getHomePath());
        return true;
    }

    private void show(HttpServletRequest request, HttpServletResponse response, PortalCheckoutService.Cart cart,
                      boolean startWithBlankLine) throws ServletException, IOException {
        request.setAttribute("cart", cart);
        request.setAttribute("form", cart.getForm());
        request.setAttribute("errors", cart.getErrors());
        request.setAttribute("quote", cart.getQuote());
        request.setAttribute("credit", cart.getCredit());
        request.setAttribute("overCreditLimit", cart.isOverCreditLimit());
        request.setAttribute("products", cart.getProducts());
        request.setAttribute("addresses", cart.getAddresses());
        request.setAttribute("startWithBlankLine", startWithBlankLine);
        request.setAttribute("today", com.oms.util.DateTimeUtil.today().toString());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
