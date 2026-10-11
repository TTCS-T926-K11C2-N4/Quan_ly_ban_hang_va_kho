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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// S3-06 AC3: chuyển giao hàng loạt khi nhân viên nghỉ. action=preview hiện số đại lý, địa bàn sẽ chuyển;
// action=confirm chuyển thật (đại lý + địa bàn, lọc theo khu vực nếu chọn), có ghi lịch sử từng đại lý.
@WebServlet("/customers/assignments/transfer")
public class CustomerTransferServlet extends HttpServlet {

    private final CustomerAssignmentService assignmentService = new CustomerAssignmentService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (!"preview".equals(action) && !"confirm".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if (!CustomerAssignmentServlet.canAssign(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            Long fromId = AccountFormParser.parseId(request.getParameter("fromSalesRepId"));
            Long toId = AccountFormParser.parseId(request.getParameter("toSalesRepId"));
            Long regionId = AccountFormParser.parseId(request.getParameter("transferRegionId"));
            String reason = CustomerAssignmentServlet.normalize(request.getParameter("reason"));
            List<SelectOption> activeReps = assignmentService.getActiveSalesReps();
            Map<String, String> errors = assignmentService.validateTransfer(fromId, toId, reason, activeReps);
            CustomerAssignmentService.TransferPreview preview = fromId == null ? null
                    : assignmentService.previewTransfer(fromId, regionId);
            if (errors.isEmpty() && preview.customers().isEmpty() && preview.regions().isEmpty()) {
                errors = new LinkedHashMap<>(errors);
                errors.put("fromSalesRepId", "Nhân viên này không còn đại lý hay địa bàn nào"
                        + (regionId == null ? "" : " trong khu vực đã chọn") + " để chuyển giao.");
            }
            if (!errors.isEmpty() || "preview".equals(action)) {
                request.setAttribute("transferOpen", true);
                request.setAttribute("transferErrors", errors);
                request.setAttribute("transferFromId", fromId);
                request.setAttribute("transferToId", toId);
                request.setAttribute("transferRegionId", regionId);
                request.setAttribute("transferReason", reason);
                request.setAttribute("transferPreview", errors.isEmpty() ? preview : null);
                CustomerAssignmentServlet.show(request, response);
                return;
            }
            int moved = assignmentService.transfer(fromId, toId, regionId, reason, CurrentUser.get(request).getId(),
                    request.getRemoteAddr());
            request.getSession().setAttribute(CustomerAssignmentServlet.FLASH_MESSAGE, "Đã chuyển giao " + moved
                    + " đại lý" + (preview.regions().isEmpty() ? "" : " và " + preview.regions().size() + " địa bàn")
                    + " cho " + CustomerAssignmentService.find(activeReps, toId).getName() + ".");
            response.sendRedirect(request.getContextPath() + "/customers/assignments?salesRep=" + toId);
        } catch (SQLException e) {
            log("Không chuyển giao được đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
