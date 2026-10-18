package com.oms.controller;

import com.oms.service.StockTransferService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S5-07: tạo phiếu chuyển kho nháp rồi mở phiếu để thêm dòng hàng
@WebServlet("/transfers/new")
public class TransferCreateServlet extends HttpServlet {

    private final StockTransferService transferService = new StockTransferService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long from = AccountFormParser.parseId(request.getParameter("from"));
        Long to = AccountFormParser.parseId(request.getParameter("to"));
        String listUrl = request.getContextPath() + "/transfers";
        if (from == null || to == null) {
            request.getSession().setAttribute(TransferListServlet.FLASH_ERROR, "Vui lòng chọn kho đi và kho đến.");
            response.sendRedirect(listUrl);
            return;
        }
        try {
            StockTransferService.Outcome outcome = transferService.create(CurrentUser.get(request).getId(), from, to,
                    request.getParameter("note"), request.getRemoteAddr());
            if (outcome.getError() != null) {
                request.getSession().setAttribute(TransferListServlet.FLASH_ERROR, outcome.getError());
                response.sendRedirect(listUrl);
                return;
            }
            request.getSession().setAttribute(TransferViewServlet.FLASH_MESSAGE, "Đã tạo phiếu chuyển kho "
                    + outcome.getTransferNo() + ". Thêm các dòng hàng cần chuyển rồi xuất kho đi.");
            response.sendRedirect(request.getContextPath() + "/transfers/view?id=" + outcome.getId());
        } catch (SQLException e) {
            log("Không tạo được phiếu chuyển kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
