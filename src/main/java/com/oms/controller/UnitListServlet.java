package com.oms.controller;

import com.oms.service.UnitService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S2-07: danh mục đơn vị tính (lon, lốc, thùng...) kèm số sản phẩm đang dùng
@WebServlet("/units")
public class UnitListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/units/unit-list.jsp";

    // Thông báo một lần sau khi thêm/sửa/xoá (Post/Redirect/Get)
    static final String FLASH_MESSAGE = "unitFlashMessage";
    static final String FLASH_ERROR = "unitFlashError";

    private final UnitService unitService = new UnitService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("units", unitService.getUnits());
        } catch (SQLException e) {
            log("Không tải được đơn vị tính", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
        Flash.moveToRequest(request, FLASH_ERROR, "flashError");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
