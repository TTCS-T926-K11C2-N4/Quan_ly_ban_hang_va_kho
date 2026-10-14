package com.oms.controller;

import com.oms.dao.WarehouseDao;
import com.oms.model.GoodsReceiptFilter;
import com.oms.model.GoodsReceiptRow;
import com.oms.model.Permission;
import com.oms.service.GoodsReceiptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-04: danh sách phiếu nhập kho có ba thẻ số liệu, lọc theo mã phiếu / nhà cung cấp, trạng thái, kho nhận,
// ngày nhập. Phiếu nháp mở form sửa, phiếu đã ghi sổ chỉ xem.
@WebServlet("/goods-receipts")
public class GoodsReceiptListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/goods-receipts/receipt-list.jsp";

    static final String FLASH_MESSAGE = "receiptFlashMessage";
    static final String FLASH_ERROR = "receiptFlashError";

    private final GoodsReceiptService receiptService = new GoodsReceiptService();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        GoodsReceiptFilter filter = GoodsReceiptFormParser.parseFilter(request);
        try {
            request.setAttribute("summary", receiptService.summary());
            request.setAttribute("receiptPage", receiptService.search(filter, GoodsReceiptFormParser.parsePage(request)));
            request.setAttribute("warehouses", warehouseDao.findActive());
        } catch (SQLException e) {
            log("Không tải được danh sách phiếu nhập", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.setAttribute("filter", filter);
        request.setAttribute("statusLabels", GoodsReceiptRow.STATUS_LABELS);
        request.setAttribute("canManage", CurrentUser.get(request).can(Permission.INVENTORY_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
