package com.oms.controller;

import com.oms.model.Permission;
import com.oms.model.PickListDetail;
import com.oms.model.PickListStatus;
import com.oms.model.SessionUser;
import com.oms.service.PickListService;
import com.oms.service.PickLotService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

// S6-03: màn soạn hàng của một phiếu (dùng được trên điện thoại): thông tin phiếu, tiến độ, danh sách dòng soạn
// theo đường đi kèm vị trí; người có quyền ghi nhận số thực lấy, hoàn tất, huỷ phiếu, in phiếu.
// Phiếu đã soạn xong: mỗi đơn có nút xác nhận xuất kho (S6-05).
@WebServlet("/pick-lists/view")
public class PickListViewServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/pick-lists/pick-list-view.jsp";

    private final PickListService pickListService = new PickListService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            PickListDetail pickList = id == null ? null
                    : pickListService.find(id, pickListService.scope(CurrentUser.get(request).getId()));
            if (pickList == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, PickListListServlet.FLASH_MESSAGE, "flashMessage");
            Flash.moveToRequest(request, PickListListServlet.FLASH_ERROR, "flashError");
            forward(request, response, pickList, null, null);
        } catch (SQLException e) {
            log("Không tải được phiếu soạn", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // inputs, errors khác null khi hiện lại form soạn sau lần lưu có lỗi
    static void forward(HttpServletRequest request, HttpServletResponse response, PickListDetail pickList,
                        Map<Long, PickListService.LineInput> inputs, Map<Long, String> errors)
            throws ServletException, IOException, SQLException {
        SessionUser user = CurrentUser.get(request);
        boolean canManage = user.can(Permission.DELIVERY_MANAGE)
                && new PickListService().canUseWarehouse(user.getId(), pickList.getWarehouseId());
        request.setAttribute("pickList", pickList);
        request.setAttribute("canPick", canManage && pickList.isEditable());
        request.setAttribute("canIssue", canManage && pickList.isDone());
        request.setAttribute("canCancel", canManage && (pickList.isEditable()
                || (pickList.isDone() && !pickList.isAnyIssued())));
        request.setAttribute("shortReasons", PickListStatus.SHORT_REASONS);
        // S6-04: lô khác có thể chọn thay cho dòng chưa soạn (thủ kho đổi lô phải nhập lý do)
        request.setAttribute("lotCandidates", canManage && pickList.isEditable()
                ? new PickLotService().candidates(pickList.getId()) : Map.of());
        request.setAttribute("lotReasonMax", PickLotService.REASON_MAX_LENGTH);
        request.setAttribute("inputs", inputs);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
