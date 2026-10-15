package com.oms.controller;

import com.oms.model.DeliveryIssueDraft;
import com.oms.model.DeliveryNoteStatus;
import com.oms.model.PickListStatus;
import com.oms.service.DeliveryNoteService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

// S6-05: "Xác nhận xuất kho & giao thiếu" cho một đơn của phiếu soạn đã soạn xong (?pickList=&order=).
// Nhập thực xuất từng dòng, lý do thiếu, xử lý phần thiếu; xác nhận (sau hộp thoại kiểm tra lại) thì lập phiếu xuất
// và mở phiếu xuất vừa lập. Đơn đã xuất rồi thì chuyển sang xem phiếu xuất (không sửa được).
@WebServlet("/delivery-notes/confirm")
public class DeliveryNoteConfirmServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/delivery-notes/delivery-note-confirm.jsp";

    private final DeliveryNoteService noteService = new DeliveryNoteService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            DeliveryIssueDraft draft = loadDraft(request);
            if (draft == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (redirectIfClosed(request, response, draft)) {
                return;
            }
            forward(request, response, draft, DeliveryNoteService.defaultForm(draft, DateTimeUtil.today()), Map.of());
        } catch (SQLException e) {
            log("Không tải được dữ liệu xuất kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            DeliveryIssueDraft draft = loadDraft(request);
            if (draft == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (redirectIfClosed(request, response, draft)) {
                return;
            }
            DeliveryNoteService.IssueForm form = parse(request, draft);
            Map<String, String> errors = DeliveryNoteService.validate(draft, form, DateTimeUtil.today());
            if (!errors.isEmpty()) {
                forward(request, response, draft, form, errors);
                return;
            }
            DeliveryNoteService.Issued issued = noteService.issue(draft, form, CurrentUser.get(request).getId(),
                    request.getRemoteAddr());
            request.getSession().setAttribute(DeliveryNoteViewServlet.FLASH_MESSAGE, "Đã xác nhận xuất kho đơn "
                    + draft.getOrderNo() + " theo phiếu xuất " + issued.deliveryNo() + ".");
            response.sendRedirect(request.getContextPath() + "/delivery-notes/view?id=" + issued.id());
        } catch (DeliveryNoteService.DeliveryException e) {
            request.getSession().setAttribute(PickListListServlet.FLASH_ERROR, e.getMessage());
            response.sendRedirect(request.getContextPath() + "/pick-lists/view?id="
                    + AccountFormParser.parseId(request.getParameter("pickList")));
        } catch (SQLException e) {
            log("Không xác nhận được xuất kho", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private DeliveryIssueDraft loadDraft(HttpServletRequest request) throws SQLException {
        Long pickListId = AccountFormParser.parseId(request.getParameter("pickList"));
        Long orderId = AccountFormParser.parseId(request.getParameter("order"));
        if (pickListId == null || orderId == null) {
            return null;
        }
        return noteService.draft(pickListId, orderId, noteService.scope(CurrentUser.get(request).getId()));
    }

    // Đã xuất: mở phiếu xuất; phiếu soạn chưa xong hoặc không phải kho mình: quay về phiếu soạn kèm thông báo
    private boolean redirectIfClosed(HttpServletRequest request, HttpServletResponse response, DeliveryIssueDraft draft)
            throws IOException, SQLException {
        if (draft.getDeliveryNoteId() != null) {
            response.sendRedirect(request.getContextPath() + "/delivery-notes/view?id=" + draft.getDeliveryNoteId());
            return true;
        }
        String error = null;
        if (!draft.isPickDone()) {
            error = "Phiếu soạn " + draft.getPickNo() + " chưa soạn xong, chưa xác nhận xuất kho được.";
        } else if (!noteService.canUseWarehouse(CurrentUser.get(request).getId(), draft.getWarehouseId())) {
            error = "Bạn chỉ xác nhận xuất kho cho kho mình phụ trách.";
        }
        if (error == null) {
            return false;
        }
        request.getSession().setAttribute(PickListListServlet.FLASH_ERROR, error);
        response.sendRedirect(request.getContextPath() + "/pick-lists/view?id=" + draft.getPickListId());
        return true;
    }

    private static DeliveryNoteService.IssueForm parse(HttpServletRequest request, DeliveryIssueDraft draft) {
        Map<Long, DeliveryNoteService.LineInput> lines = new LinkedHashMap<>();
        for (DeliveryIssueDraft.Line line : draft.getLines()) {
            String key = Long.toString(line.getOrderItemId());
            lines.put(line.getOrderItemId(), new DeliveryNoteService.LineInput(trim(request.getParameter("qty_" + key)),
                    trim(request.getParameter("reason_" + key))));
        }
        return new DeliveryNoteService.IssueForm(lines, trim(request.getParameter("shortageAction")),
                trim(request.getParameter("backorderDate")), trim(request.getParameter("backorderTime")),
                trim(request.getParameter("shortageNote")));
    }

    private static void forward(HttpServletRequest request, HttpServletResponse response, DeliveryIssueDraft draft,
                                DeliveryNoteService.IssueForm form, Map<String, String> errors)
            throws ServletException, IOException {
        request.setAttribute("draft", draft);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("shortReasons", PickListStatus.SHORT_REASONS);
        request.setAttribute("shortageLabels", DeliveryNoteStatus.shortageLabels());
        request.setAttribute("today", DateTimeUtil.today());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
