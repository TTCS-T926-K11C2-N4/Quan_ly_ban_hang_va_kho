package com.oms.controller;

import com.oms.dao.WarehouseDao;
import com.oms.model.Permission;
import com.oms.model.PickListFilter;
import com.oms.model.PickListStatus;
import com.oms.service.DeliveryScope;
import com.oms.service.PickListService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-03: danh sách phiếu soạn hàng. Bảng đơn đã duyệt chờ soạn (tick nhiều đơn cùng kho để gộp một lượt soạn),
// bảng đơn còn nợ hàng chờ soạn bổ sung (S6-05) và bảng phiếu soạn; lọc theo từ khoá, kho, ngày giao, trạng thái.
@WebServlet("/pick-lists")
public class PickListListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/pick-lists/pick-list-list.jsp";

    static final String FLASH_MESSAGE = "pickFlashMessage";
    static final String FLASH_ERROR = "pickFlashError";

    private final PickListService pickListService = new PickListService();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PickListFilter filter = PickListFormParser.parseFilter(request);
        try {
            DeliveryScope scope = pickListService.scope(CurrentUser.get(request).getId());
            if (!scope.allowed()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("overview", pickListService.overview(filter,
                    PickListFormParser.parsePage(request), scope));
            request.setAttribute("warehouses", warehouseDao.findActive());
        } catch (SQLException e) {
            log("Không tải được danh sách phiếu soạn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("filter", filter);
        request.setAttribute("statusLabels", PickListStatus.labels());
        request.setAttribute("dueLabels", PickListFilter.dueLabels());
        request.setAttribute("canManage", CurrentUser.get(request).can(Permission.DELIVERY_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
