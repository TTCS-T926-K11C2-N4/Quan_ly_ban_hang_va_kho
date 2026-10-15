package com.oms.controller;

import com.oms.model.PickListDetail;
import com.oms.service.PickListService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

// S6-03: ghi nhận soạn hàng. action=save lưu tiến độ (phiếu chuyển "Đang soạn"); action=complete hoàn tất khi mọi
// dòng đã soạn. Soạn thiếu phải chọn lý do; có lỗi thì hiện lại form với số đã nhập.
@WebServlet("/pick-lists/pick")
public class PickListPickServlet extends HttpServlet {

    private final PickListService pickListService = new PickListService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        boolean complete = "complete".equals(request.getParameter("action"));
        try {
            PickListDetail pickList = id == null ? null
                    : pickListService.find(id, pickListService.scope(CurrentUser.get(request).getId()));
            if (pickList == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Map<Long, PickListService.LineInput> inputs = PickListFormParser.parseLines(request, pickList);
            Map<Long, String> errors = PickListService.validate(pickList, inputs, complete);
            if (!errors.isEmpty()) {
                request.setAttribute("flashError", complete ? "Chưa hoàn tất được: kiểm tra các dòng được đánh dấu đỏ."
                        : "Chưa lưu được: kiểm tra các dòng được đánh dấu đỏ.");
                PickListViewServlet.forward(request, response, pickList, inputs, errors);
                return;
            }
            pickListService.savePicking(id, inputs, complete, CurrentUser.get(request).getId(),
                    request.getRemoteAddr());
            request.getSession().setAttribute(PickListListServlet.FLASH_MESSAGE, complete
                    ? "Đã hoàn tất soạn phiếu " + pickList.getPickNo() + ". Xác nhận xuất kho cho từng đơn bên dưới."
                    : "Đã lưu tiến độ soạn phiếu " + pickList.getPickNo() + ".");
        } catch (PickListService.PickListException e) {
            request.getSession().setAttribute(PickListListServlet.FLASH_ERROR, e.getMessage());
        } catch (SQLException e) {
            log("Không ghi nhận được soạn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/pick-lists/view?id=" + id);
    }
}
