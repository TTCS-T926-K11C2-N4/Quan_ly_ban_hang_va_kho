package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.StockTransfer;
import com.oms.service.StockTransferService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-07: chi tiết phiếu chuyển kho: thêm dòng (nháp), xuất kho đi, nhận hàng ở kho đến
@WebServlet("/transfers/view")
public class TransferViewServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/transfers/transfer-view.jsp";
    static final String FLASH_MESSAGE = "transferViewFlashMessage";
    static final String FLASH_ERROR = "transferViewFlashError";

    private final StockTransferService transferService = new StockTransferService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            StockTransfer transfer = id == null ? null : transferService.find(id);
            if (transfer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            boolean canManage = CurrentUser.get(request).can(Permission.INVENTORY_MANAGE);
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, FLASH_ERROR, "flashError");
            request.setAttribute("transfer", transfer);
            request.setAttribute("canManage", canManage);
            if (canManage && transfer.isDraft()) {
                request.setAttribute("lotOptions", transferService.findLotOptions(transfer.getFromWarehouseId()));
            }
            request.setAttribute("reasonMaxLength", StockTransferService.REASON_MAX_LENGTH);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được phiếu chuyển kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
