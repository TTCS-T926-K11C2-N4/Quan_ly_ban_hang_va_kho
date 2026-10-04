package com.oms.controller;

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
import java.util.Map;

// S2-10: sửa bảng giá chưa có đơn dùng; đã khoá thì chuyển sang tạo phiên bản
@WebServlet("/price-lists/edit")
public class PriceListEditServlet extends HttpServlet {

    private final PriceListService priceListService = new PriceListService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            PriceList list = find(request);
            if (list == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (redirectIfLocked(request, response, list)) {
                return;
            }
            PriceListForm form = priceListService.toForm(list, list.getValidFrom().toString(),
                    list.getValidTo() == null ? null : list.getValidTo().toString());
            PriceListCreateServlet.showForm(request, response, "edit", list, form, Map.of(), priceListService);
        } catch (SQLException e) {
            log("Không tải được bảng giá", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            PriceList list = find(request);
            if (list == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (redirectIfLocked(request, response, list)) {
                return;
            }
            PriceListForm form = PriceListFormParser.read(request);
            Map<String, String> errors = priceListService.validate(form, list, null);
            if (!errors.isEmpty()) {
                PriceListCreateServlet.showForm(request, response, "edit", list, form, errors, priceListService);
                return;
            }
            if (!priceListService.update(list, form, CurrentUser.get(request).getId(), request.getRemoteAddr())) {
                // Vừa có đơn dùng bảng giá trong lúc đang sửa
                redirectIfLocked(request, response, priceListService.find(list.getId()));
                return;
            }
            request.getSession().setAttribute(PriceListListServlet.FLASH_MESSAGE,
                    "Đã cập nhật bảng giá " + list.getCode() + ".");
            response.sendRedirect(request.getContextPath() + "/price-lists?id=" + list.getId());
        } catch (SQLException e) {
            log("Không cập nhật được bảng giá", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private PriceList find(HttpServletRequest request) throws SQLException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        return id == null ? null : priceListService.find(id);
    }

    private static boolean redirectIfLocked(HttpServletRequest request, HttpServletResponse response, PriceList list)
            throws IOException {
        if (list == null || !list.isLocked()) {
            return false;
        }
        request.getSession().setAttribute(PriceListListServlet.FLASH_ERROR, "Bảng giá " + list.getCode()
                + " đã có đơn sử dụng nên không sửa được. Hãy tạo phiên bản mới để thay đổi giá.");
        response.sendRedirect(request.getContextPath() + "/price-lists?id=" + list.getId());
        return true;
    }
}
