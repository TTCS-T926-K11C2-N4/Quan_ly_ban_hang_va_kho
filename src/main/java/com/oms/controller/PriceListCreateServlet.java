package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.PriceList;
import com.oms.model.PriceListForm;
import com.oms.service.PriceListService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;

// S2-10: tạo bảng giá mới (mã tự sinh BGnnn)
@WebServlet("/price-lists/new")
public class PriceListCreateServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/price-lists/price-list-form.jsp";

    private final PriceListService priceListService = new PriceListService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            showForm(request, response, "create", null,
                    new PriceListForm(null, null, null, null, List.of()), Map.of(), priceListService);
        } catch (SQLException e) {
            log("Không tải được form bảng giá", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PriceListForm form = PriceListFormParser.read(request);
        try {
            Map<String, String> errors = priceListService.validate(form, null, null);
            if (!errors.isEmpty()) {
                showForm(request, response, "create", null, form, errors, priceListService);
                return;
            }
            long id;
            try {
                id = priceListService.create(form, CurrentUser.get(request).getId(), request.getRemoteAddr());
            } catch (SQLIntegrityConstraintViolationException e) {
                // Người khác vừa tạo bảng giá cùng lúc (trùng mã tự sinh): thử lại một lần
                id = priceListService.create(form, CurrentUser.get(request).getId(), request.getRemoteAddr());
            }
            request.getSession().setAttribute(PriceListListServlet.FLASH_MESSAGE,
                    "Đã tạo bảng giá \"" + form.getName() + "\".");
            response.sendRedirect(request.getContextPath() + "/price-lists?id=" + id);
        } catch (SQLException e) {
            log("Không tạo được bảng giá", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // mode: create | edit | version. source: bảng đang sửa hoặc bản cũ khi tạo phiên bản.
    static void showForm(HttpServletRequest request, HttpServletResponse response, String mode, PriceList source,
                         PriceListForm form, Map<String, String> errors, PriceListService service)
            throws ServletException, IOException, SQLException {
        boolean canViewCost = CurrentUser.get(request).can(Permission.COST_PRICE_VIEW);
        request.setAttribute("mode", mode);
        request.setAttribute("source", source);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("groups", service.getGroups());
        request.setAttribute("products", service.getPricingProducts(canViewCost));
        request.setAttribute("canViewCost", canViewCost);
        if ("version".equals(mode)) {
            request.setAttribute("earliestStart", PriceListService.earliestVersionStart(source));
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
