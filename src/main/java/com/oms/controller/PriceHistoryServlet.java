package com.oms.controller;

import com.oms.model.PriceHistoryFilter;
import com.oms.model.PriceList;
import com.oms.model.PriceListStatus;
import com.oms.model.ProductCategory;
import com.oms.model.SessionUser;
import com.oms.service.PriceHistoryService;
import com.oms.service.PriceListService;
import com.oms.service.ProductCategoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

// S3-02: lịch sử thay đổi giá bán / giá sàn theo sản phẩm, chỉ xem. Ai xem được bảng giá thì xem được lịch sử
// (tài khoản đại lý không). ?priceList=id: mở từ một bảng giá, chỉ hiện lịch sử của bảng giá đó.
@WebServlet("/price-history")
public class PriceHistoryServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/price-lists/price-history.jsp";
    private static final int KEYWORD_MAX_LENGTH = 100;

    private final PriceHistoryService historyService = new PriceHistoryService();
    private final PriceListService priceListService = new PriceListService();
    private final ProductCategoryService categoryService = new ProductCategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!PriceListListServlet.canSeePriceLists(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String keyword = normalize(request.getParameter("keyword"));
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            keyword = keyword.substring(0, KEYWORD_MAX_LENGTH);
        }
        Long categoryId = AccountFormParser.parseId(request.getParameter("categoryId"));
        Long groupId = AccountFormParser.parseId(request.getParameter("groupId"));
        Long priceListId = AccountFormParser.parseId(request.getParameter("priceList"));
        PriceListStatus status = parseStatus(request.getParameter("status"));
        LocalDate from = PriceListListServlet.parseDate(request.getParameter("from"));
        LocalDate to = PriceListListServlet.parseDate(request.getParameter("to"));
        try {
            List<ProductCategory> categories = categoryService.getTree();
            ProductCategory category = categoryId == null ? null
                    : categories.stream().filter(c -> c.getId() == categoryId).findFirst().orElse(null);
            PriceList priceList = priceListId == null ? null : priceListService.find(priceListId);
            PriceHistoryFilter filter = new PriceHistoryFilter(keyword, category == null ? null : category.getPath(),
                    groupId, priceList == null ? null : priceList.getId(), status, from, to);
            request.setAttribute("historyPage", historyService.search(filter, parsePage(request)));
            request.setAttribute("categories", categories);
            request.setAttribute("groups", priceListService.getGroups());
            request.setAttribute("priceList", priceList);
        } catch (SQLException e) {
            log("Không tải được lịch sử thay đổi giá", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("keyword", keyword);
        request.setAttribute("categoryFilter", categoryId);
        request.setAttribute("groupFilter", groupId);
        request.setAttribute("statusFilter", status == null ? null : status.getCode());
        request.setAttribute("fromDate", from);
        request.setAttribute("toDate", to);
        request.setAttribute("statuses", PriceListStatus.values());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static PriceListStatus parseStatus(String value) {
        for (PriceListStatus status : PriceListStatus.values()) {
            if (status.getCode().equals(value)) {
                return status;
            }
        }
        return null;
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
