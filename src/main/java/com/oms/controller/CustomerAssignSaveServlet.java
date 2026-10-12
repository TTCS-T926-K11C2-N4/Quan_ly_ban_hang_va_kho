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

    private static final int NAMED_LIMIT = 3;

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
            String repName = CustomerAssignmentService.find(activeReps, toSalesRepId).getName();
            request.getSession().setAttribute(CustomerAssignmentServlet.FLASH_MESSAGE, message(result, repName));
            String tone = !result.conflict().isEmpty() ? "warning" : result.assigned() == 0 ? "info" : null;
            if (tone != null) {
                request.getSession().setAttribute(CustomerAssignmentServlet.FLASH_TONE, tone);
            }
            String query = CustomerAssignmentServlet.listQuery(request);
            response.sendRedirect(request.getContextPath() + "/customers/assignments" + (query.isEmpty() ? "" : "?" + query));
        } catch (SQLException e) {
            log("Không phân công được nhân viên kinh doanh", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Nói rõ đại lý nào được giao mới, chuyển từ ai, bỏ qua vì đã đúng người, chưa đổi được vì người khác vừa đổi
    static String message(CustomerAssignmentService.AssignResult result, String repName) {
        StringBuilder message = new StringBuilder();
        if (result.assigned() == 0 && result.conflict().isEmpty()) {
            message.append("Không có thay đổi. ");
            List<String> unchanged = result.unchanged();
            message.append(unchanged.size() <= NAMED_LIMIT
                    ? String.join(", ", unchanged) + (unchanged.size() == 1 ? " đã" : " đều đã")
                    : "Cả " + unchanged.size() + " đại lý đã chọn đều đã").append(" do ").append(repName)
                    .append(" phụ trách.");
            return message.toString();
        }
        if (result.assigned() > 0) {
            message.append("Đã giao ").append(result.assigned()).append(" đại lý cho ").append(repName);
            List<String> parts = new ArrayList<>();
            if (!result.newlyAssigned().isEmpty() && !result.movedFrom().isEmpty()) {
                parts.add(result.newlyAssigned().size() + " đại lý mới phân công");
            }
            result.movedFrom().forEach((fromName, labels) ->
                    parts.add(describe(labels) + " chuyển từ " + fromName));
            message.append(parts.isEmpty() ? "." : ": " + String.join("; ", parts) + ".");
        }
        if (!result.unchanged().isEmpty()) {
            message.append(" Bỏ qua ").append(describe(result.unchanged())).append(" vì đã do người này phụ trách.");
        }
        if (!result.conflict().isEmpty()) {
            message.append(' ').append(describe(result.conflict()))
                    .append(" vừa được người khác đổi người phụ trách nên chưa đổi, vui lòng tải lại trang và kiểm tra.");
        }
        return message.toString().trim();
    }

    // Ít đại lý thì ghi rõ mã – tên, nhiều thì chỉ ghi số lượng cho thông báo khỏi quá dài
    private static String describe(List<String> labels) {
        return labels.size() <= NAMED_LIMIT ? String.join(", ", labels) : labels.size() + " đại lý";
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
