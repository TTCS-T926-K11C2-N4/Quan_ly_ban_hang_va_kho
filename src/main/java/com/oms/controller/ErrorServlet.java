package com.oms.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Trang lỗi chung (S1-07), web.xml chuyển mọi lỗi 4xx/5xx và exception về đây.
// Ghi đè service() vì lỗi của request POST cũng được chuyển tới đây bằng POST.
@WebServlet("/error")
public class ErrorServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/error/error.jsp";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        // Mở thẳng /error (không phải do lỗi chuyển tới) thì coi như trang không tồn tại
        int status = statusAttribute instanceof Integer code ? code : HttpServletResponse.SC_NOT_FOUND;

        String title;
        String message;
        if (status == HttpServletResponse.SC_FORBIDDEN) {
            title = "Bạn không có quyền truy cập";
            message = "Tài khoản của bạn chưa được cấp quyền dùng chức năng này."
                    + " Nếu cần dùng, hãy liên hệ quản trị viên để được cấp quyền.";
        } else if (status == HttpServletResponse.SC_NOT_FOUND) {
            title = "Không tìm thấy trang";
            message = "Trang bạn tìm không tồn tại, đã được chuyển đi hoặc chức năng này chưa được mở.";
        } else if (status < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            title = "Yêu cầu không hợp lệ";
            message = "Thao tác vừa gửi không hợp lệ. Vui lòng quay lại và thử lại.";
        } else {
            title = "Hệ thống đang gặp sự cố";
            message = "Đã có lỗi khi xử lý yêu cầu của bạn. Vui lòng thử lại sau ít phút;"
                    + " nếu vẫn lỗi, hãy báo cho quản trị viên.";
        }

        response.setStatus(status);
        request.setAttribute("errorStatus", status);
        request.setAttribute("errorTitle", title);
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
