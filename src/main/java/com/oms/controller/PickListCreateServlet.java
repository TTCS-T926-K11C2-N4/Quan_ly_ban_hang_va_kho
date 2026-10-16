package com.oms.controller;

import com.oms.service.PickListService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-03: "Tạo phiếu soạn" từ các đơn được tick (cùng kho xuất). Tạo xong mở màn soạn hàng của phiếu mới;
// lỗi thì quay lại danh sách kèm thông báo.
@WebServlet("/pick-lists/new")
public class PickListCreateServlet extends HttpServlet {

    private final PickListService pickListService = new PickListService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PickListService.Created created = pickListService.create(PickListFormParser.parseOrderIds(request),
                    CurrentUser.get(request).getId(), request.getRemoteAddr());
            request.getSession().setAttribute(PickListListServlet.FLASH_MESSAGE,
                    "Đã tạo phiếu soạn " + created.pickNo() + ".");
            response.sendRedirect(request.getContextPath() + "/pick-lists/view?id=" + created.id());
        } catch (PickListService.PickListException e) {
            request.getSession().setAttribute(PickListListServlet.FLASH_ERROR, e.getMessage());
            response.sendRedirect(request.getContextPath() + "/pick-lists");
        } catch (SQLException e) {
            log("Không tạo được phiếu soạn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
