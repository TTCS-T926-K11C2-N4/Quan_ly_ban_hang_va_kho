package com.oms.controller;

import com.oms.model.StoredFile;
import com.oms.service.AvatarService;
import com.oms.util.FileStorage;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.SQLException;

// Trả ảnh đại diện (S2-03): /avatar?id={avatarFileId}&size=thumb|full (mặc định thumb).
// Mọi người đã đăng nhập xem được ảnh của nhau để nhận ra ai tạo đơn.
@WebServlet("/avatar")
public class AvatarServlet extends HttpServlet {

    private final AvatarService avatarService = new AvatarService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            StoredFile file = id == null ? null : avatarService.findAvatar(id);
            if (file == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String key = "full".equals(request.getParameter("size")) ? file.getStorageKey()
                    : AvatarService.thumbnailKey(file.getStorageKey());
            Path path = FileStorage.resolve(key);

            response.setContentType(file.getContentType());
            response.setContentLengthLong(Files.size(path));
            // Đổi ảnh thì đổi id nên nội dung theo id không bao giờ đổi: cho trình duyệt giữ lâu
            response.setHeader("Cache-Control", "private, max-age=31536000, immutable");
            response.setHeader("X-Content-Type-Options", "nosniff");
            Files.copy(path, response.getOutputStream());
        } catch (NoSuchFileException e) {
            log("Thiếu tệp ảnh đại diện id=" + id, e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException | IOException e) {
            log("Không trả được ảnh đại diện id=" + id, e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
