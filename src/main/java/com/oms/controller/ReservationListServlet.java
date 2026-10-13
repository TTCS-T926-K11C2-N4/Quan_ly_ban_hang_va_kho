package com.oms.controller;

import com.oms.model.ReservationFilter;
import com.oms.service.StockReservationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-06: danh sách đơn đã duyệt đang giữ chỗ tồn, tìm theo mã đơn / đại lý, lọc ngày duyệt và trạng thái đơn.
// Mở từ nút "Xem" của sổ tồn kho thì chỉ hiện đơn giữ chỗ SKU đó tại kho đó (?product=&warehouse=).
// Nhân viên kinh doanh chỉ thấy đơn của đại lý mình phụ trách.
@WebServlet("/stock/reservations")
public class ReservationListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/reservations/reservation-list.jsp";

    private final StockReservationService reservationService = new StockReservationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("keyword");
        ReservationFilter filter = new ReservationFilter(keyword == null ? null : keyword.trim(),
                blankToNull(request.getParameter("date")), request.getParameter("status"),
                AccountFormParser.parseId(request.getParameter("product")),
                AccountFormParser.parseId(request.getParameter("warehouse")));
        long userId = CurrentUser.get(request).getId();
        try {
            request.setAttribute("summary", reservationService.summary(userId, filter));
            request.setAttribute("orderPage", reservationService.search(userId, filter, parsePage(request)));
            request.setAttribute("productLabel", reservationService.getProductWarehouseLabel(filter));
            request.setAttribute("holdHours", reservationService.getHoldHours());
        } catch (SQLException e) {
            log("Không tải được danh sách đơn giữ chỗ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("filter", filter);
        request.setAttribute("statusLabels", ReservationFilter.STATUS_LABELS);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int parsePage(HttpServletRequest request) {
        try {
            String page = request.getParameter("page");
            return page == null ? 1 : Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
