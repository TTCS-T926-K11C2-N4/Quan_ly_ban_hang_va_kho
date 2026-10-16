package com.oms.controller;

import com.oms.model.PickListDetail;
import com.oms.service.PickListService;
import com.oms.service.PickLotService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-04: thủ kho chọn lô xuất khác với lô hệ thống gợi ý (FEFO) cho một dòng soạn, bắt buộc nhập lý do
@WebServlet("/pick-lists/lot")
public class PickListLotServlet extends HttpServlet {

    private final PickListService pickListService = new PickListService();
    private final PickLotService pickLotService = new PickLotService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        Long line = AccountFormParser.parseId(request.getParameter("line"));
        Long lot = AccountFormParser.parseId(request.getParameter("lot"));
        try {
            long userId = CurrentUser.get(request).getId();
            PickListDetail pickList = id == null ? null : pickListService.find(id, pickListService.scope(userId));
            if (pickList == null || line == null || !pickListService.canUseWarehouse(userId, pickList.getWarehouseId())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (lot == null) {
                request.getSession().setAttribute(PickListListServlet.FLASH_ERROR, "Vui lòng chọn lô muốn xuất.");
            } else {
                pickLotService.changeLot(id, line, lot, request.getParameter("reason"), userId,
                        request.getRemoteAddr());
                request.getSession().setAttribute(PickListListServlet.FLASH_MESSAGE,
                        "Đã đổi lô xuất của dòng soạn trong phiếu " + pickList.getPickNo() + ".");
            }
        } catch (PickLotService.LotChangeException e) {
            request.getSession().setAttribute(PickListListServlet.FLASH_ERROR, e.getMessage());
        } catch (SQLException e) {
            log("Không đổi được lô của dòng soạn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/pick-lists/view?id=" + id);
    }
}
