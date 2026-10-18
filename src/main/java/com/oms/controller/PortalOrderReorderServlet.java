package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.SessionUser;
import com.oms.service.PortalCheckoutService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

// S5-02: đại lý đặt lại đơn đã mua, toàn bộ hoặc một phần dòng. Dựng giỏ hàng điền sẵn (giá áp lại theo bảng giá
// hiện hành, hàng ngừng kinh doanh bị loại kèm thông báo) rồi mở màn đặt hàng để đại lý xem lại trước khi gửi.
@WebServlet("/portal/orders/reorder")
public class PortalOrderReorderServlet extends HttpServlet {

    private final PortalCheckoutService checkoutService = new PortalCheckoutService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            Customer customer = checkoutService.findCustomer(user.getId());
            PortalCheckoutService.Reorder reorder = customer == null || id == null ? null
                    : checkoutService.buildReorder(customer, id, parseSelected(request));
            if (reorder == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (reorder.getForm().getLines().isEmpty()) {
                request.getSession().setAttribute(PortalOrderViewServlet.FLASH_ERROR, "Không có mặt hàng nào đặt lại"
                        + " được. " + String.join(" ", reorder.getNotices()));
                response.sendRedirect(request.getContextPath() + "/portal/orders/view?id=" + id);
                return;
            }
            request.getSession().setAttribute(PortalOrderNewServlet.PREFILL_FORM, reorder.getForm());
            request.getSession().setAttribute(PortalOrderNewServlet.PREFILL_NOTICES, reorder.getNotices());
            response.sendRedirect(request.getContextPath() + "/portal/order/new");
        } catch (SQLException e) {
            log("Không đặt lại được đơn của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Số thứ tự các dòng được tick; rỗng hoặc bấm "Đặt lại cả đơn" thì lấy toàn bộ
    private static Set<Integer> parseSelected(HttpServletRequest request) {
        Set<Integer> selected = new HashSet<>();
        if (request.getParameter("all") != null || request.getParameterValues("line") == null) {
            return selected;
        }
        for (String value : request.getParameterValues("line")) {
            try {
                selected.add(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                // bỏ qua giá trị lạ
            }
        }
        return selected;
    }
}
