package com.oms.controller;

import com.oms.model.DiscountPolicyForm;
import com.oms.model.DiscountPolicyRow;
import com.oms.model.DiscountPolicyStatus;
import com.oms.model.Permission;
import com.oms.model.SessionUser;
import com.oms.service.DiscountPolicyService;
import com.oms.service.PriceListService;
import com.oms.service.ProductCategoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// S3-01: danh sách chính sách chiết khấu theo sản lượng, khung thêm/sửa bên phải (theo thiết kế).
// ?edit=new mở khung thêm mới, ?edit=id mở khung sửa. Ai xem được bảng giá thì xem được (đại lý không);
// thêm/sửa/xoá cần PRODUCT_MANAGE như bảng giá.
@WebServlet("/discount-policies")
public class DiscountPolicyServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/price-lists/discount-policies.jsp";
    static final String FLASH_MESSAGE = "discountPolicyFlashMessage";
    static final String NEW = "new";
    // Điều kiện lọc đi kèm form POST để quay lại đúng danh sách đang xem
    static final List<String> LIST_PARAMS = List.of("keyword", "groupId", "status", "page");
    private static final int KEYWORD_MAX_LENGTH = 100;

    private static final DiscountPolicyService POLICY_SERVICE = new DiscountPolicyService();
    private static final PriceListService PRICE_LIST_SERVICE = new PriceListService();
    private static final ProductCategoryService CATEGORY_SERVICE = new ProductCategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = CurrentUser.get(request);
        if (!PriceListListServlet.canSeePriceLists(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            String edit = request.getParameter("edit");
            DiscountPolicyForm form = null;
            DiscountPolicyRow editing = null;
            if (user.can(Permission.PRODUCT_MANAGE) && NEW.equals(edit)) {
                form = DiscountPolicyForm.empty();
            } else if (user.can(Permission.PRODUCT_MANAGE) && edit != null) {
                Long id = AccountFormParser.parseId(edit);
                editing = id == null ? null : POLICY_SERVICE.find(id);
                if (editing == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                form = DiscountPolicyForm.of(editing);
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            show(request, response, form, editing, Map.of());
        } catch (SQLException e) {
            log("Không tải được chính sách chiết khấu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // form = null: không mở khung thêm/sửa; editing = null: khung thêm mới
    static void show(HttpServletRequest request, HttpServletResponse response, DiscountPolicyForm form,
                     DiscountPolicyRow editing, Map<String, String> errors)
            throws ServletException, IOException, SQLException {
        String keyword = normalize(request.getParameter("keyword"));
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            keyword = keyword.substring(0, KEYWORD_MAX_LENGTH);
        }
        Long groupId = AccountFormParser.parseId(request.getParameter("groupId"));
        if (groupId == null && "0".equals(request.getParameter("groupId"))) {
            groupId = 0L;
        }
        DiscountPolicyStatus status = DiscountPolicyStatus.fromCode(normalize(request.getParameter("status")));
        boolean canManage = CurrentUser.get(request).can(Permission.PRODUCT_MANAGE);

        request.setAttribute("policyPage", POLICY_SERVICE.search(keyword, groupId, status, parsePage(request)));
        request.setAttribute("keyword", keyword);
        request.setAttribute("groupFilter", groupId);
        request.setAttribute("statusFilter", status == null ? null : status.getCode());
        request.setAttribute("statuses", DiscountPolicyStatus.values());
        request.setAttribute("groups", PRICE_LIST_SERVICE.getGroups());
        request.setAttribute("canManage", canManage);
        request.setAttribute("form", form);
        request.setAttribute("editing", editing);
        request.setAttribute("errors", errors);
        request.setAttribute("listQuery", QueryString.of(request, LIST_PARAMS));
        if (form != null) {
            request.setAttribute("categories", CATEGORY_SERVICE.getTree());
            request.setAttribute("products", POLICY_SERVICE.getProductOptions());
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
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
