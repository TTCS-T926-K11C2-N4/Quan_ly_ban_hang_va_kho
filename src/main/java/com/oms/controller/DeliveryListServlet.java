package com.oms.controller;

import com.oms.model.DeliveryPoint;
import com.oms.model.Permission;
import com.oms.service.DeliveryConfirmService;
import com.oms.service.DeliveryScope;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

// S6-08: "Xác nhận giao hàng" - phiếu xuất theo ngày (mặc định hôm nay) gom theo điểm giao, tiến độ giao trong ngày;
// tìm theo đại lý / địa chỉ / mã đơn, lọc theo trạng thái điểm giao. "Đồng bộ" tải lại số liệu mới nhất.
@WebServlet("/deliveries")
public class DeliveryListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/deliveries/delivery-list.jsp";

    static final String FLASH_MESSAGE = "deliveryFlashMessage";
    static final String FLASH_ERROR = "deliveryFlashError";

    private static final Map<String, String> STATUS_LABELS = new LinkedHashMap<>();

    static {
        STATUS_LABELS.put(DeliveryPoint.PENDING, "Còn đơn chờ giao");
        STATUS_LABELS.put(DeliveryPoint.DELIVERED, "Đã giao xong");
        STATUS_LABELS.put(DeliveryPoint.FAILED, "Có đơn giao không thành công");
    }

    private final DeliveryConfirmService deliveryService = new DeliveryConfirmService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate date = parseDate(request.getParameter("date"));
        String keyword = request.getParameter("keyword");
        String status = STATUS_LABELS.containsKey(request.getParameter("status")) ? request.getParameter("status") : null;
        try {
            DeliveryScope scope = deliveryService.scope(CurrentUser.get(request).getId());
            if (!scope.allowed()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("day", deliveryService.day(date, keyword, status, scope));
        } catch (SQLException e) {
            log("Không tải được danh sách giao hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("keyword", keyword == null ? "" : keyword.trim());
        request.setAttribute("status", status);
        request.setAttribute("statusLabels", STATUS_LABELS);
        request.setAttribute("canManage", CurrentUser.get(request).can(Permission.DELIVERY_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // Ngày không hợp lệ hoặc bỏ trống: hôm nay
    static LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? DateTimeUtil.today() : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return DateTimeUtil.today();
        }
    }
}
