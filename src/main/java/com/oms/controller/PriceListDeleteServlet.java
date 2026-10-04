package com.oms.controller;

import com.oms.model.PriceList;
import com.oms.service.PriceListService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

// S2-10: xoá bảng giá chưa có đơn dùng và chưa có phiên bản sau
@WebServlet("/price-lists/delete")
public class PriceListDeleteServlet extends HttpServlet {

    private final PriceListService priceListService = new PriceListService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            PriceList list = id == null ? null : priceListService.find(id);
            if (list == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            HttpSession session = request.getSession();
            String error = priceListService.validateDelete(list);
            if (error == null && !priceListService.delete(list, CurrentUser.get(request).getId(),
                    request.getRemoteAddr())) {
                error = "Bảng giá " + list.getCode() + " vừa có đơn sử dụng, phiên bản sau hoặc lịch sử giá nên không xoá được.";
            }
            if (error != null) {
                session.setAttribute(PriceListListServlet.FLASH_ERROR, error);
                response.sendRedirect(request.getContextPath() + "/price-lists?id=" + list.getId());
                return;
            }
            session.setAttribute(PriceListListServlet.FLASH_MESSAGE, "Đã xoá bảng giá " + list.getCode() + ".");
            response.sendRedirect(request.getContextPath() + "/price-lists");
        } catch (SQLException e) {
            log("Không xoá được bảng giá", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
