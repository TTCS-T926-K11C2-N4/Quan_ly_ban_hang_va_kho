package com.oms.controller;

import com.oms.model.ApprovalRequest;
import com.oms.model.OrderDetail;
import com.oms.model.OrderStatus;
import com.oms.model.Permission;
import com.oms.service.OrderApprovalService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// S4-05: màn duyệt một đơn: thông tin đơn, cảnh báo vi phạm, hàng hoá (dòng dưới giá sàn tô đỏ), lịch sử duyệt
// (chỉ xem) và ba hành động Duyệt / Từ chối / Trả lại sửa (POST /approvals/decide).
@WebServlet("/approvals/view")
public class ApprovalViewServlet extends HttpServlet {

    private final OrderApprovalService approvalService = new OrderApprovalService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            OrderDetail order = id == null ? null : approvalService.findOrder(id);
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, ApprovalListServlet.FLASH_ERROR, "flashError");
            show(request, response, order, null, null, null);
        } catch (SQLException e) {
            log("Không tải được đơn cần duyệt", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // decision / comment / error: hành động vừa gửi bị lỗi ý kiến, để mở lại đúng hộp thoại kèm chữ đã gõ
    static void show(HttpServletRequest request, HttpServletResponse response, OrderDetail order, String decision,
                     String comment, String error) throws ServletException, IOException, SQLException {
        OrderApprovalService service = new OrderApprovalService();
        List<ApprovalRequest> history = service.findHistory(order.getId());
        ApprovalRequest pending = history.stream().filter(ApprovalRequest::isPending).findFirst().orElse(null);
        request.setAttribute("order", order);
        request.setAttribute("history", history);
        request.setAttribute("pending", pending);
        request.setAttribute("credit", service.findCredit(order.getCustomerId()));
        request.setAttribute("canDecide", pending != null && OrderStatus.PENDING_APPROVAL.equals(order.getStatus())
                && CurrentUser.get(request).can(Permission.ORDER_APPROVE));
        request.setAttribute("decision", decision);
        request.setAttribute("comment", comment);
        request.setAttribute("decisionError", error);
        request.setAttribute("commentMaxLength", OrderApprovalService.COMMENT_MAX_LENGTH);
        request.getRequestDispatcher("/WEB-INF/views/approvals/approval-view.jsp").forward(request, response);
    }
}
