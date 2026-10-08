package com.oms.controller;

import com.oms.dao.PriceListDao;
import com.oms.dao.RegionDao;
import com.oms.model.CustomerFilter;
import com.oms.model.CustomerListStatus;
import com.oms.model.SessionUser;
import com.oms.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// S3-08: danh sách đại lý, tìm nhanh theo mã, tên, số điện thoại và lọc theo khu vực, nhóm khách hàng,
// người phụ trách, trạng thái. Lọc bằng GET để giữ được link và nút Quay lại của trình duyệt.
@WebServlet("/customers")
public class CustomerListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/customers/customer-list.jsp";
    private static final int KEYWORD_MAX_LENGTH = 100;

    private final CustomerService customerService = new CustomerService();
    private final RegionDao regionDao = new RegionDao();
    private final PriceListDao priceListDao = new PriceListDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Tài khoản đại lý có CUSTOMER_VIEW phạm vi OWN nhưng không dùng màn nội bộ này: về trang chủ của đại lý (S1-01)
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
        Long regionId = AccountFormParser.parseId(request.getParameter("region"));
        Long groupId = AccountFormParser.parseId(request.getParameter("group"));
        Long salesRepId = AccountFormParser.parseId(request.getParameter("salesRep"));
        CustomerListStatus status = CustomerListStatus.fromCode(normalize(request.getParameter("status")));

        try {
            boolean canViewAll = customerService.canViewAll(userId);
            // Nhân viên kinh doanh chỉ thấy đại lý của mình nên bỏ qua ô Người phụ trách nếu có trên link
            CustomerFilter filter = new CustomerFilter(keyword, regionId, groupId, canViewAll ? salesRepId : null,
                    status);
            request.setAttribute("customerPage", customerService.search(userId, filter, parsePage(request)));
            request.setAttribute("filter", filter);
            request.setAttribute("canViewAll", canViewAll);
            request.setAttribute("regions", regionDao.findActive());
            request.setAttribute("groups", priceListDao.findActiveGroups());
            request.setAttribute("salesReps", canViewAll ? customerService.getSalesRepOptions() : List.of());
        } catch (SQLException e) {
            log("Không tải được danh sách đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("statuses", CustomerListStatus.values());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int parsePage(HttpServletRequest request) {
        try {
            String page = request.getParameter("page");
            return page == null ? 1 : Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
