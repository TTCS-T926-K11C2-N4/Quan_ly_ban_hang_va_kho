package com.oms.controller;

import com.oms.service.StockTransferService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// S5-07: thêm / xoá dòng, xuất kho đi, nhận hàng, huỷ phiếu nháp. Mọi thao tác cần INVENTORY_MANAGE (AccessRules).
@WebServlet({"/transfers/line/add", "/transfers/line/remove", "/transfers/ship", "/transfers/receive",
        "/transfers/cancel"})
public class TransferActionServlet extends HttpServlet {

    private final StockTransferService transferService = new StockTransferService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        if (id == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long userId = CurrentUser.get(request).getId();
        String ip = request.getRemoteAddr();
        try {
            StockTransferService.Outcome outcome;
            String done;
            switch (request.getServletPath()) {
                case "/transfers/line/add" -> {
                    Long lot = AccountFormParser.parseId(request.getParameter("lot"));
                    outcome = lot == null ? null : transferService.addLine(userId, id, lot,
                            request.getParameter("qty"), ip);
                    if (outcome == null) {
                        request.getSession().setAttribute(TransferViewServlet.FLASH_ERROR,
                                "Vui lòng chọn lô hàng cần chuyển.");
                        response.sendRedirect(request.getContextPath() + "/transfers/view?id=" + id);
                        return;
                    }
                    done = "Đã thêm dòng hàng vào phiếu ";
                }
                case "/transfers/line/remove" -> {
                    Long item = AccountFormParser.parseId(request.getParameter("item"));
                    outcome = transferService.removeLine(userId, id, item == null ? -1 : item, ip);
                    done = "Đã xoá dòng hàng khỏi phiếu ";
                }
                case "/transfers/ship" -> {
                    outcome = transferService.ship(userId, id, ip);
                    done = "Đã xuất kho đi, hàng đang trên đường. Phiếu ";
                }
                case "/transfers/receive" -> {
                    outcome = transferService.receive(userId, id,
                            StockTransferService.parseIds(request.getParameterValues("itemId")),
                            toList(request.getParameterValues("received")),
                            toList(request.getParameterValues("reason")), ip);
                    done = "Đã xác nhận nhận hàng, tồn kho đến đã được cộng. Phiếu ";
                }
                default -> {
                    outcome = transferService.cancel(userId, id, ip);
                    done = "Đã huỷ phiếu ";
                }
            }
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(TransferViewServlet.FLASH_ERROR, outcome.getError());
            } else {
                request.getSession().setAttribute(TransferViewServlet.FLASH_MESSAGE,
                        done + outcome.getTransferNo() + ".");
            }
            response.sendRedirect(request.getContextPath() + "/transfers/view?id=" + id);
        } catch (SQLException e) {
            log("Không xử lý được phiếu chuyển kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static List<String> toList(String[] values) {
        return values == null ? List.of() : List.of(values);
    }
}
