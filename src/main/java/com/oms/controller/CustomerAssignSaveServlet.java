package com.oms.controller;

import com.oms.model.SelectOption;
import com.oms.service.CustomerAssignmentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// S3-06 AC1: giao các đại lý đã tick cho một nhân viên kinh doanh phụ trách chính
@WebServlet("/customers/assignments/assign")
public class CustomerAssignSaveServlet extends HttpServlet {

    private final CustomerAssignmentService assignmentService = new CustomerAssignmentService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (!CustomerAssignmentServlet.canAssign(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            List<Long> customerIds = parseIds(request.getParameterValues("customerIds"));
            Long toSalesRepId = AccountFormParser.parseId(request.getParameter("toSalesRepId"));
            String reason = CustomerAssignmentServlet.normalize(request.getParameter("reason"));
            List<SelectOption> activeReps = assignmentService.getActiveSalesReps();
            Map<String, String> errors = assignmentService.validateAssign(customerIds, toSalesRepId, reason,
                    activeReps);
            if (!errors.isEmpty()) {
                request.setAttribute("assignErrors", errors);
                request.setAttribute("selectedIds", Set.copyOf(customerIds));
                request.setAttribute("assignToId", toSalesRepId);
                request.setAttribute("assignReason", reason);
                CustomerAssignmentServlet.show(request, response);
                return;
            }
            CustomerAssignmentService.AssignResult result = assignmentService.assign(customerIds, toSalesRepId,
                    reason, CurrentUser.get(request).getId(), request.getRemoteAddr());
            StringBuilder message = new StringBuilder("Đã giao ").append(result.assigned()).append(" đại lý cho ")
                    .append(CustomerAssignmentService.find(activeReps, toSalesRepId).getName()).append('.');
            if (result.unchanged() > 0) {
                message.append(' ').append(result.unchanged()).append(" đại lý đã do người này phụ trách từ trước.");
            }
            if (result.conflict() > 0) {
                message.append(' ').append(result.conflict())
                        .append(" đại lý vừa được người khác đổi người phụ trách nên chưa đổi, vui lòng xem lại.");
            }
            request.getSession().setAttribute(CustomerAssignmentServlet.FLASH_MESSAGE, message.toString());
            String query = CustomerAssignmentServlet.listQuery(request);
            response.sendRedirect(request.getContextPath() + "/customers/assignments" + (query.isEmpty() ? "" : "?" + query));
        } catch (SQLException e) {
            log("Không phân công được nhân viên kinh doanh", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static List<Long> parseIds(String[] values) {
        Set<Long> ids = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                Long id = AccountFormParser.parseId(value);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return new ArrayList<>(ids);
    }
}
