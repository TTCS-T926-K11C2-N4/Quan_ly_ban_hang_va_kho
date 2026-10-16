package com.oms.controller;

import com.oms.dao.RegionDao;
import com.oms.model.OrderListFilter;
import com.oms.model.OrderStatus;
import com.oms.model.Permission;
import com.oms.model.SessionUser;
import com.oms.service.CustomerService;
import com.oms.service.OrderListService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

// S4-07: danh sách đơn hàng, lọc theo trạng thái, đại lý, nhân viên, khu vực, khoảng ngày tạo; hiện tổng tiền của
// tập đang lọc. Lọc bằng GET để giữ được link và nút Quay lại. Nhân viên kinh doanh chỉ thấy đơn của đại lý mình.
@WebServlet("/orders")
public class OrderListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/orders/order-list.jsp";
    private static final int KEYWORD_MAX_LENGTH = 100;

    private final OrderListService listService = new OrderListService();
    private final CustomerService customerService = new CustomerService();
    private final RegionDao regionDao = new RegionDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Tài khoản đại lý xem đơn của mình ở cổng đại lý (S5-01), không dùng màn nội bộ này
        SessionUser user = CurrentUser.get(request);
        if (user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        long userId = user.getId();
        String keyword = normalize(request.getParameter("keyword"));
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            keyword = keyword.substring(0, KEYWORD_MAX_LENGTH);
        }
        try {
            boolean canViewAll = listService.canViewAll(userId);
            OrderListFilter filter = new OrderListFilter(keyword, normalize(request.getParameter("status")),
                    AccountFormParser.parseId(request.getParameter("customer")),
                    canViewAll ? AccountFormParser.parseId(request.getParameter("salesRep")) : null,
                    AccountFormParser.parseId(request.getParameter("region")),
                    parseDate(request.getParameter("from")), parseDate(request.getParameter("to")),
                    normalize(request.getParameter("sort")), parseInt(request.getParameter("size"), 0));
            OrderListService.Result result = listService.search(userId, filter,
                    parseInt(request.getParameter("page"), 1));
            request.setAttribute("filter", filter);
            request.setAttribute("rows", result.getPage());
            request.setAttribute("totals", result.getTotals());
            request.setAttribute("canViewAll", canViewAll);
            request.setAttribute("customers", customerService.findVisibleCustomers(userId, Permission.ORDER_VIEW));
            request.setAttribute("salesReps", canViewAll ? customerService.getSalesRepOptions() : List.of());
            request.setAttribute("regions", regionDao.findActive());
        } catch (SQLException e) {
            log("Không tải được danh sách đơn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("statusCodes", OrderStatus.codes());
        request.setAttribute("statusLabels", OrderStatus.codes().stream()
                .collect(java.util.stream.Collectors.toMap(code -> code, OrderStatus::label)));
        request.setAttribute("sorts", OrderListFilter.getSorts());
        request.setAttribute("pageSizes", OrderListFilter.PAGE_SIZES);
        request.setAttribute("canCreate", user.can(Permission.ORDER_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Ngày dạng yyyy-MM-dd từ ô type=date; sai định dạng thì coi như không lọc
    private static LocalDate parseDate(String value) {
        try {
            String text = normalize(value);
            return text == null ? null : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
