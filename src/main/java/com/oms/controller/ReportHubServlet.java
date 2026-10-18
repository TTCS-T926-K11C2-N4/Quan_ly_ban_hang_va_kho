package com.oms.controller;

import com.oms.dao.PermissionDao;
import com.oms.model.Permission;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// Trang tổng hợp các báo cáo (REPORT_VIEW); mỗi báo cáo kiểm quyền riêng của nó.
// Bảng điều hành chỉ mở được với phạm vi ALL (ExecutiveDashboardServlet) nên chỉ hiện ô đó khi có phạm vi này.
@WebServlet("/reports")
public class ReportHubServlet extends HttpServlet {

    private final PermissionDao permissionDao = new PermissionDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("canViewCompany",
                    "ALL".equals(permissionDao.findScope(CurrentUser.get(request).getId(), Permission.REPORT_VIEW)));
        } catch (SQLException e) {
            log("Không tải được quyền xem báo cáo", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/reports/report-hub.jsp").forward(request, response);
    }
}
