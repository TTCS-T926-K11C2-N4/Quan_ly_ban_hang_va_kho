package com.oms.controller;

import com.oms.service.StocktakeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// S5-08: lưu số đếm (/stocktakes/count), chốt phiếu (/stocktakes/finalize), huỷ phiếu (/stocktakes/cancel).
// Quyền từng đường dẫn khai báo ở AccessRules: chốt phiếu cần quyền riêng STOCKTAKE_FINALIZE (chỉ Quản lý kho).
@WebServlet({"/stocktakes/count", "/stocktakes/finalize", "/stocktakes/cancel"})
public class StocktakeActionServlet extends HttpServlet {

    private final StocktakeService stocktakeService = new StocktakeService();

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
        String path = request.getServletPath();
        try {
            StocktakeService.Outcome outcome;
            String done;
            if ("/stocktakes/count".equals(path)) {
                outcome = stocktakeService.saveCounts(userId, id, parseIds(request.getParameterValues("itemId")),
                        toList(request.getParameterValues("counted")), ip);
                done = "Đã lưu số đếm của phiếu ";
            } else if ("/stocktakes/finalize".equals(path)) {
                outcome = stocktakeService.finalizeStocktake(userId, id, request.getParameter("reason"), ip);
                done = "Đã chốt kiểm kê, tồn kho đã được điều chỉnh về số đếm. Phiếu ";
            } else {
                outcome = stocktakeService.cancel(userId, id, ip);
                done = "Đã huỷ phiếu ";
            }
            if ("NOT_FOUND".equals(outcome.getError())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(StocktakeViewServlet.FLASH_ERROR, outcome.getError());
            } else {
                request.getSession().setAttribute(StocktakeViewServlet.FLASH_MESSAGE,
                        done + outcome.getStocktakeNo() + ".");
            }
            response.sendRedirect(request.getContextPath() + "/stocktakes/view?id=" + id);
        } catch (SQLException e) {
            log("Không xử lý được phiếu kiểm kê", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static List<Long> parseIds(String[] values) {
        List<Long> ids = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                Long id = AccountFormParser.parseId(value);
                ids.add(id == null ? -1L : id);
            }
        }
        return ids;
    }

    private static List<String> toList(String[] values) {
        return values == null ? List.of() : List.of(values);
    }
}
