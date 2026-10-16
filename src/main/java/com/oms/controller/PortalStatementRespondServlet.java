package com.oms.controller;

import com.oms.model.SessionUser;
import com.oms.service.ReconciliationStatementService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-06: đại lý xác nhận (decision=confirm) hoặc báo lệch (decision=dispute, kèm ghi chú) biên bản đối chiếu
@WebServlet("/portal/statements/respond")
public class PortalStatementRespondServlet extends HttpServlet {

    private final ReconciliationStatementService statementService = new ReconciliationStatementService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser user = CurrentUser.get(request);
        if (!user.isCustomer()) {
            response.sendRedirect(request.getContextPath() + user.getHomePath());
            return;
        }
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        String decision = request.getParameter("decision");
        if (id == null || !("confirm".equals(decision) || "dispute".equals(decision))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            ReconciliationStatementService.Outcome outcome = statementService.respond(user.getId(), id,
                    "confirm".equals(decision), request.getParameter("note"), request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                request.getSession().setAttribute(PortalStatementServlet.FLASH_ERROR, outcome.getError());
            } else {
                request.getSession().setAttribute(PortalStatementServlet.FLASH_MESSAGE, "confirm".equals(decision)
                        ? "Đã xác nhận biên bản " + outcome.getStatementNo() + "."
                        : "Đã gửi phản hồi lệch số liệu của biên bản " + outcome.getStatementNo() + " cho công ty.");
            }
            response.sendRedirect(request.getContextPath() + "/portal/statements");
        } catch (SQLException e) {
            log("Không ghi nhận được phản hồi biên bản đối chiếu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
