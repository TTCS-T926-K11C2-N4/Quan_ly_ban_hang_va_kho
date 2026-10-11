package com.oms.controller;

import com.oms.model.DiscountPolicyForm;
import com.oms.model.DiscountPolicyRow;
import com.oms.service.DiscountPolicyService;
import com.oms.service.PriceListService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// S3-01: lưu khung thêm mới (không có id) hoặc khung sửa một chính sách chiết khấu cùng các bậc
@WebServlet("/discount-policies/save")
public class DiscountPolicySaveServlet extends HttpServlet {

    private final DiscountPolicyService policyService = new DiscountPolicyService();
    private final PriceListService priceListService = new PriceListService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            DiscountPolicyRow editing = id == null ? null : policyService.find(id);
            if (id != null && editing == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            DiscountPolicyForm form = parse(request, id);
            DiscountPolicyService.Checked checked = policyService.validate(form, priceListService.getGroups());
            if (!checked.isValid()) {
                DiscountPolicyServlet.show(request, response, form, editing, checked.errors());
                return;
            }
            long userId = CurrentUser.get(request).getId();
            String message;
            if (editing == null) {
                policyService.create(checked, userId, request.getRemoteAddr());
                message = "Đã thêm chính sách chiết khấu \"" + form.getName() + "\".";
            } else {
                policyService.update(editing, checked, userId, request.getRemoteAddr());
                message = "Đã cập nhật chính sách " + editing.getCode() + ". Đơn đã tạo trước đó giữ nguyên chiết khấu đã tính.";
            }
            request.getSession().setAttribute(DiscountPolicyServlet.FLASH_MESSAGE, message);
            String query = QueryString.of(request, DiscountPolicyServlet.LIST_PARAMS);
            response.sendRedirect(request.getContextPath() + "/discount-policies" + (query.isEmpty() ? "" : "?" + query));
        } catch (SQLException e) {
            log("Không lưu được chính sách chiết khấu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static DiscountPolicyForm parse(HttpServletRequest request, Long id) {
        String[] minQty = values(request, "tierMinQty");
        String[] tierValue = values(request, "tierValue");
        List<DiscountPolicyForm.TierLine> tiers = new ArrayList<>();
        for (int i = 0; i < Math.max(minQty.length, tierValue.length); i++) {
            tiers.add(new DiscountPolicyForm.TierLine(at(minQty, i), at(tierValue, i)));
        }
        return new DiscountPolicyForm(id, DiscountPolicyServlet.normalize(request.getParameter("name")),
                DiscountPolicyServlet.normalize(request.getParameter("customerGroupId")),
                DiscountPolicyServlet.normalize(request.getParameter("scopeType")),
                DiscountPolicyServlet.normalize(request.getParameter("productSku")),
                DiscountPolicyServlet.normalize(request.getParameter("categoryId")),
                DiscountPolicyServlet.normalize(request.getParameter("discountType")),
                DiscountPolicyServlet.normalize(request.getParameter("validFrom")),
                DiscountPolicyServlet.normalize(request.getParameter("validTo")),
                request.getParameter("active") != null, tiers);
    }

    private static String[] values(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? new String[0] : values;
    }

    private static String at(String[] values, int index) {
        return index < values.length ? DiscountPolicyServlet.normalize(values[index]) : null;
    }
}
