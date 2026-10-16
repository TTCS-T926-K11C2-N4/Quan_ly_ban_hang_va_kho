package com.oms.controller;

import com.oms.model.GoodsReceiptDetail;
import com.oms.model.Permission;
import com.oms.service.GoodsReceiptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-04: xem phiếu nhập (chỉ đọc). Phiếu đã ghi sổ không sửa được, chỉ "Lập phiếu điều chỉnh";
// phiếu nháp có nút mở form sửa.
@WebServlet("/goods-receipts/view")
public class GoodsReceiptViewServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/goods-receipts/receipt-view.jsp";

    private final GoodsReceiptService receiptService = new GoodsReceiptService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            GoodsReceiptDetail receipt = id == null ? null : receiptService.find(id);
            if (receipt == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, GoodsReceiptListServlet.FLASH_MESSAGE, "flashMessage");
            request.setAttribute("receipt", receipt);
            request.setAttribute("canManage", CurrentUser.get(request).can(Permission.INVENTORY_MANAGE));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được phiếu nhập", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
