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
import java.util.LinkedHashMap;
import java.util.Map;

// S8-04: Quản lý kinh doanh đặt chỉ tiêu doanh số tháng cho từng nhân viên và khu vực, theo dõi tiến độ.
// GET /targets?month=yyyy-MM xem, POST /targets lưu (tháng đã qua phải có lý do).
@WebServlet("/targets")
public class TargetServlet extends HttpServlet {

    static final String FLASH_MESSAGE = "targetFlashMessage";
    static final String FLASH_ERROR = "targetFlashError";
    private static final String VIEW = "/WEB-INF/views/targets/target-list.jsp";

    private final SalesTargetService targetService = new SalesTargetService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate month = SalesTargetService.parseMonth(request.getParameter("month"), DateTimeUtil.today());
        try {
            request.setAttribute("reps", targetService.repTargets(month, null));
            request.setAttribute("regions", targetService.regionTargets(month));
        } catch (SQLException e) {
            log("Không tải được chỉ tiêu doanh số", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("month", month.toString().substring(0, 7));
        request.setAttribute("locked", SalesTargetService.isLocked(month, DateTimeUtil.today()));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LocalDate month = SalesTargetService.parseMonth(request.getParameter("month"), DateTimeUtil.today());
        Map<String, String> entries = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            if ((name.startsWith("u:") || name.startsWith("r:")) && values.length > 0) {
                entries.put(name, values[0]);
            }
        });
        String monthText = month.toString().substring(0, 7);
        try {
            SalesTargetService.Outcome outcome = targetService.save(CurrentUser.get(request).getId(), month, entries,
                    request.getParameter("reason"), request.getRemoteAddr());
            if (outcome.getError() != null) {
                request.getSession().setAttribute(FLASH_ERROR, outcome.getError());
            } else {
                request.getSession().setAttribute(FLASH_MESSAGE, outcome.getChanged() == 0
                        ? "Không có chỉ tiêu nào thay đổi." : "Đã lưu " + outcome.getChanged() + " chỉ tiêu.");
            }
            response.sendRedirect(request.getContextPath() + "/targets?month=" + monthText);
        } catch (SQLException e) {
            log("Không lưu được chỉ tiêu doanh số", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
