package com.oms.controller;

import com.oms.service.PickListService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-03: huỷ phiếu soạn chưa hoàn tất; các đơn quay về chờ soạn
@WebServlet("/pick-lists/cancel")
public class PickListCancelServlet extends HttpServlet {

    private final PickListService pickListService = new PickListService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        if (id == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            String pickNo = pickListService.cancel(id, CurrentUser.get(request).getId(), request.getRemoteAddr());
            request.getSession().setAttribute(PickListListServlet.FLASH_MESSAGE,
                    "Đã huỷ phiếu soạn " + pickNo + "; các đơn quay về chờ soạn.");
            response.sendRedirect(request.getContextPath() + "/pick-lists");
        } catch (PickListService.PickListException e) {
            request.getSession().setAttribute(PickListListServlet.FLASH_ERROR, e.getMessage());
            response.sendRedirect(request.getContextPath() + "/pick-lists/view?id=" + id);
        } catch (SQLException e) {
            log("Không huỷ được phiếu soạn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
