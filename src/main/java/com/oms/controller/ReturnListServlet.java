package com.oms.controller;

import com.oms.service.ReturnService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S8-01: danh sách phiếu trả hàng; nhân viên kinh doanh chỉ thấy phiếu của đại lý mình phụ trách
@WebServlet("/returns")
public class ReturnListServlet extends HttpServlet {

    static final String FLASH_MESSAGE = "returnFlashMessage";
    static final String FLASH_ERROR = "returnFlashError";
    private static final String VIEW = "/WEB-INF/views/returns/return-list.jsp";

    private final ReturnService returnService = new ReturnService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ReturnService.Access access = returnService.access(CurrentUser.get(request).getId());
            if (!access.isAllowed()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("returns", returnService.findAll(access.getSalesRepId()));
            request.setAttribute("access", access);
        } catch (SQLException e) {
            log("Không tải được phiếu trả hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
