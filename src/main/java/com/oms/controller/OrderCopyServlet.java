package com.oms.controller;

import com.oms.service.OrderCopyService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S4-09: sao chép đơn hàng thành đơn Nháp mới rồi mở đơn nháp đó để sửa tiếp.
@WebServlet("/orders/copy")
public class OrderCopyServlet extends HttpServlet {

    private final OrderCopyService copyService = new OrderCopyService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            OrderCopyService.Result result = id == null ? null
                    : copyService.copy(userId, id, request.getRemoteAddr());
            if (result == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (result.getError() != null) {
                request.getSession().setAttribute(OrderViewServlet.FLASH_ERROR, result.getError());
                response.sendRedirect(request.getContextPath() + "/orders/view?id=" + id);
                return;
            }
            StringBuilder message = new StringBuilder("Đã sao chép đơn ").append(result.getSourceOrderNo())
                    .append(" thành đơn nháp ").append(result.getSaved().getOrderNo())
                    .append(". Giá và chiết khấu đã tính lại theo bảng giá hiện hành; chọn ngày giao rồi gửi đơn.");
            if (result.isAddressChanged()) {
                message.append(" Điểm giao cũ đã ngừng dùng nên đơn lấy điểm giao mặc định của đại lý.");
            }
            if (!result.getSkipped().isEmpty()) {
                message.append(" Bỏ qua ").append(result.getSkipped().size()).append(" dòng không còn bán được: ")
                        .append(String.join("; ", result.getSkipped())).append(".");
            }
            request.getSession().setAttribute(OrderNewServlet.FLASH_MESSAGE, message.toString());
            response.sendRedirect(request.getContextPath() + "/orders/new?draft=" + result.getSaved().getId());
        } catch (SQLException e) {
            log("Không sao chép được đơn hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
