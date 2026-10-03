package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.PriceList;
import com.oms.model.SessionUser;
import com.oms.service.PriceListService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// S2-10: danh sách bảng giá lọc theo nhóm khách hàng và khoảng ngày; ?id= mở phần chi tiết dòng giá bên dưới
@WebServlet("/price-lists")
public class PriceListListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/price-lists/price-list-list.jsp";

    // Thông báo một lần sau khi thêm/sửa/xoá/tạo phiên bản (Post/Redirect/Get)
    static final String FLASH_MESSAGE = "priceListFlashMessage";
    static final String FLASH_ERROR = "priceListFlashError";

    private final PriceListService priceListService = new PriceListService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!canSeePriceLists(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        boolean canViewCost = user.can(Permission.COST_PRICE_VIEW);
        Long groupId = AccountFormParser.parseId(request.getParameter("groupId"));
        LocalDate from = parseDate(request.getParameter("from"));
        LocalDate to = parseDate(request.getParameter("to"));
        Long selectedId = AccountFormParser.parseId(request.getParameter("id"));
        try {
            request.setAttribute("listPage", priceListService.search(groupId, from, to, parsePage(request)));
            request.setAttribute("groups", priceListService.getGroups());
            if (selectedId != null) {
                PriceList selected = priceListService.find(selectedId);
                if (selected != null) {
                    request.setAttribute("selected", selected);
                    request.setAttribute("selectedItems", priceListService.getItems(selectedId, canViewCost));
                    request.setAttribute("selectedHasNext", priceListService.hasNextVersion(selectedId));
                }
            }
        } catch (SQLException e) {
            log("Không tải được bảng giá", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("groupFilter", groupId);
        request.setAttribute("fromDate", from);
        request.setAttribute("toDate", to);
        request.setAttribute("canViewCost", canViewCost);
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // Đại lý cũng có PRODUCT_VIEW (phạm vi của mình) nhưng không được xem bảng giá, giá sàn của các nhóm khác
    static boolean canSeePriceLists(SessionUser user) {
        return !user.isCustomer();
    }

    static LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
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
