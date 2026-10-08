package com.oms.controller;

import com.oms.dao.PriceListDao;
import com.oms.dao.RegionDao;
import com.oms.model.Customer;
import com.oms.model.CustomerFilter;
import com.oms.model.Permission;
import com.oms.model.SessionUser;
import com.oms.service.CustomerAssignmentService;
import com.oms.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// S3-06: phân công nhân viên kinh doanh theo đại lý và khu vực. Danh sách đại lý (lọc như S3-08, thêm "Chưa phân
// công"), người có CUSTOMER_ASSIGN tick đại lý để giao cho một nhân viên, hoặc chuyển giao hàng loạt khi nhân viên
// nghỉ. ?history=idĐạiLý mở khung Lịch sử chuyển giao; ?transfer=1 mở khung chuyển giao hàng loạt.
@WebServlet("/customers/assignments")
public class CustomerAssignmentServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/customers/assignments.jsp";
    static final String FLASH_MESSAGE = "assignmentFlashMessage";
    static final String UNASSIGNED = "none";
    // Điều kiện lọc đi kèm các form POST để quay lại đúng danh sách đang xem
    static final List<String> LIST_PARAMS = List.of("keyword", "region", "group", "salesRep", "page");
    private static final int KEYWORD_MAX_LENGTH = 100;

    private static final CustomerService CUSTOMER_SERVICE = new CustomerService();
    private static final CustomerAssignmentService ASSIGNMENT_SERVICE = new CustomerAssignmentService();
    private static final RegionDao REGION_DAO = new RegionDao();
    private static final PriceListDao PRICE_LIST_DAO = new PriceListDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Tài khoản đại lý có CUSTOMER_VIEW phạm vi OWN nhưng không dùng màn nội bộ này (S1-01)
        SessionUser user = CurrentUser.get(request);
        if (user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        request.setAttribute("transferOpen", request.getParameter("transfer") != null);
        try {
            show(request, response);
        } catch (SQLException e) {
            log("Không tải được màn phân công nhân viên kinh doanh", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Phân công / chuyển giao chỉ cho người xem được mọi đại lý (Quản lý kinh doanh, Admin)
    static boolean canAssign(HttpServletRequest request) throws SQLException {
        SessionUser user = CurrentUser.get(request);
        return user.can(Permission.CUSTOMER_ASSIGN) && CUSTOMER_SERVICE.canViewAll(user.getId());
    }

    // Đọc điều kiện lọc từ request (GET hoặc trường ẩn của form POST) rồi hiện trang; servlet POST đặt sẵn lỗi,
    // giá trị đã nhập, kết quả xem trước vào request trước khi gọi
    static void show(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        long userId = CurrentUser.get(request).getId();
        String keyword = normalize(request.getParameter("keyword"));
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            keyword = keyword.substring(0, KEYWORD_MAX_LENGTH);
        }
        boolean canViewAll = CUSTOMER_SERVICE.canViewAll(userId);
        String salesRepParam = request.getParameter("salesRep");
        boolean unassigned = canViewAll && UNASSIGNED.equals(salesRepParam);
        Long salesRepId = canViewAll ? AccountFormParser.parseId(salesRepParam) : null;
        CustomerFilter filter = new CustomerFilter(keyword, AccountFormParser.parseId(request.getParameter("region")),
                AccountFormParser.parseId(request.getParameter("group")), salesRepId, unassigned, null);
        boolean canAssign = canAssign(request);

        request.setAttribute("customerPage", CUSTOMER_SERVICE.search(userId, filter, parsePage(request)));
        request.setAttribute("filter", filter);
        request.setAttribute("canViewAll", canViewAll);
        request.setAttribute("canAssign", canAssign);
        request.setAttribute("regions", REGION_DAO.findActive());
        request.setAttribute("groups", PRICE_LIST_DAO.findActiveGroups());
        request.setAttribute("salesReps", canViewAll ? CUSTOMER_SERVICE.getSalesRepOptions() : List.of());
        request.setAttribute("activeReps", canAssign ? ASSIGNMENT_SERVICE.getActiveSalesReps() : List.of());
        request.setAttribute("listQuery", listQuery(request));
        if (request.getAttribute("assignErrors") == null) {
            request.setAttribute("assignErrors", Map.of());
        }
        if (request.getAttribute("transferErrors") == null) {
            request.setAttribute("transferErrors", Map.of());
        }

        Long historyId = AccountFormParser.parseId(request.getParameter("history"));
        Customer historyCustomer = historyId == null ? null
                : CUSTOMER_SERVICE.findVisible(userId, Permission.CUSTOMER_VIEW, historyId);
        if (historyCustomer != null) {
            request.setAttribute("historyCustomer", historyCustomer);
            request.setAttribute("history", ASSIGNMENT_SERVICE.getHistory(historyCustomer.getId()));
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // "keyword=...&region=..." của điều kiện lọc đang xem (đã mã hoá URL), để giữ khi mở lịch sử hoặc sau khi lưu
    static String listQuery(HttpServletRequest request) {
        StringBuilder query = new StringBuilder();
        for (String name : LIST_PARAMS) {
            String value = normalize(request.getParameter(name));
            if (value != null) {
                query.append(query.length() == 0 ? "" : "&").append(name).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return query.toString();
    }

    static String normalize(String value) {
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
