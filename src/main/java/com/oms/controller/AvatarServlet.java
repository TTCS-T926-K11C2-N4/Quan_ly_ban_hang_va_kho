package com.oms.controller;

import com.oms.service.AvatarService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

// S2-03: ảnh đại diện của một người dùng, ?user=id&size=thumb|full. Ai đã đăng nhập cũng xem được
// (để nhận ra ai tạo đơn); chưa có ảnh thì chuyển sang ảnh mặc định.
@WebServlet("/avatar")
public class AvatarServlet extends HttpServlet {

    private final AvatarService avatarService = new AvatarService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long userId = AccountFormParser.parseId(request.getParameter("user"));
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            Path path = avatarService.findAvatarPath(userId, !"full".equals(request.getParameter("size")));
            if (path == null) {
                response.sendRedirect(request.getContextPath() + "/assets/img/avatar-placeholder.svg");
                return;
            }
            response.setContentType("image/png");
            response.setContentLengthLong(Files.size(path));
            // Đường dẫn ảnh kèm v=<id ảnh> nên ảnh mới có đường dẫn mới, được lưu đệm lâu
            response.setHeader("Cache-Control", "private, max-age=86400");
            Files.copy(path, response.getOutputStream());
        } catch (SQLException e) {
            log("Không đọc được ảnh đại diện", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
