package com.oms.controller;

import com.oms.service.SalesTargetService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

// S8-04: nhân viên kinh doanh xem tiến độ chỉ tiêu của chính mình theo thời gian thực
@WebServlet("/targets/my")
public class TargetMyServlet extends HttpServlet {

    private final SalesTargetService targetService = new SalesTargetService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate month = SalesTargetService.parseMonth(request.getParameter("month"), DateTimeUtil.today());
        try {
            request.setAttribute("mine", targetService.repTargets(month, CurrentUser.get(request).getId()));
        } catch (SQLException e) {
            log("Không tải được tiến độ chỉ tiêu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("month", month.toString().substring(0, 7));
        request.getRequestDispatcher("/WEB-INF/views/targets/target-my.jsp").forward(request, response);
    }
}
