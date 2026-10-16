package com.oms.controller;

import com.oms.dao.PermissionDao;
import com.oms.model.Permission;
import com.oms.service.ExecutiveDashboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S8-05: dashboard điều hành bán hàng và kho cho người xem được báo cáo toàn công ty (REPORT_VIEW phạm vi ALL)
@WebServlet("/executive")
public class ExecutiveDashboardServlet extends HttpServlet {

    private final ExecutiveDashboardService dashboardService = new ExecutiveDashboardService();
    private final PermissionDao permissionDao = new PermissionDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (!"ALL".equals(permissionDao.findScope(CurrentUser.get(request).getId(), Permission.REPORT_VIEW))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("board", dashboardService.load());
        } catch (SQLException e) {
            log("Không tải được dashboard điều hành", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/reports/executive-dashboard.jsp").forward(request, response);
    }
}
