package com.oms.controller;

import com.oms.model.DeliveryNoteDetail;
import com.oms.service.DeliveryNoteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S6-05: xem phiếu xuất kho đã xác nhận (chỉ đọc, không sửa được): cần xuất / thực xuất / thiếu từng dòng kèm lô,
// lý do thiếu, xử lý phần thiếu và kết quả giao hàng.
@WebServlet("/delivery-notes/view")
public class DeliveryNoteViewServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/delivery-notes/delivery-note-view.jsp";

    static final String FLASH_MESSAGE = "deliveryNoteFlashMessage";

    private final DeliveryNoteService noteService = new DeliveryNoteService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            DeliveryNoteDetail note = id == null ? null
                    : noteService.find(id, noteService.scope(CurrentUser.get(request).getId()));
            if (note == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            request.setAttribute("note", note);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            log("Không tải được phiếu xuất", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
