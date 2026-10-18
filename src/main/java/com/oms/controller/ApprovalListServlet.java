package com.oms.controller;

import com.oms.service.OrderApprovalService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S4-05 AC1: danh sách đơn chờ duyệt, ghi rõ lý do cần duyệt và mức vi phạm; cũ nhất trước để duyệt theo thứ tự gửi
@WebServlet("/approvals")
public class ApprovalListServlet extends HttpServlet {

    static final String FLASH_MESSAGE = "approvalFlashMessage";
    static final String FLASH_ERROR = "approvalFlashError";
    private static final int KEYWORD_MAX_LENGTH = 100;

    private final OrderApprovalService approvalService = new OrderApprovalService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("keyword");
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            keyword = keyword.substring(0, KEYWORD_MAX_LENGTH);
        }
        try {
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, FLASH_ERROR, "flashError");
            request.setAttribute("keyword", keyword);
            request.setAttribute("rows", approvalService.findQueue(keyword));
            request.getRequestDispatcher("/WEB-INF/views/approvals/approval-list.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Không tải được danh sách đơn chờ duyệt", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
