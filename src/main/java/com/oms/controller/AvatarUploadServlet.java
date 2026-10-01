package com.oms.controller;

import com.oms.model.SessionUser;
import com.oms.security.SessionRegistry;
import com.oms.service.AuthService;
import com.oms.service.AvatarService;
import com.oms.service.InvalidAvatarException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

// Tải ảnh đại diện của chính mình (S2-03): POST multipart/form-data, ô tệp tên "avatar".
// Xong (hoặc lỗi) đều quay về trang hồ sơ kèm thông báo.
@WebServlet("/profile/avatar")
@MultipartConfig(maxFileSize = AvatarService.MAX_BYTES, maxRequestSize = AvatarService.MAX_BYTES + 64 * 1024)
public class AvatarUploadServlet extends HttpServlet {

    private final AvatarService avatarService = new AvatarService();
    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long userId = CurrentUser.get(request).getId();
        try {
            Part part;
            try {
                part = request.getPart("avatar");
            } catch (IllegalStateException e) {
                // Tomcat chặn tệp vượt maxFileSize trước khi Servlet đọc
                throw new InvalidAvatarException("Ảnh tối đa 2MB.");
            } catch (ServletException e) {
                throw new InvalidAvatarException("Vui lòng chọn ảnh.");
            }
            if (part == null || part.getSize() == 0) {
                throw new InvalidAvatarException("Vui lòng chọn ảnh.");
            }

            byte[] data;
            try (InputStream input = part.getInputStream()) {
                data = input.readAllBytes();
            }
            AvatarService.ProcessedAvatar avatar = avatarService.process(data);
            avatarService.save(userId, part.getSubmittedFileName(), avatar);

            // Ảnh mới hiện ngay ở sidebar trên mọi phiên đang đăng nhập của người này
            SessionUser refreshed = authService.loadSessionUser(userId);
            if (refreshed != null) {
                SessionRegistry.replaceUser(refreshed);
            }
            request.getSession().setAttribute(ProfileServlet.FLASH_UPDATED, "Đã cập nhật ảnh đại diện.");
        } catch (InvalidAvatarException e) {
            request.getSession().setAttribute(ProfileServlet.FLASH_AVATAR_ERROR, e.getMessage());
        } catch (SQLException | IOException e) {
            log("Không lưu được ảnh đại diện", e);
            request.getSession().setAttribute(ProfileServlet.FLASH_AVATAR_ERROR,
                    "Hệ thống chưa lưu được ảnh. Vui lòng thử lại sau.");
        }
        response.sendRedirect(request.getContextPath() + "/profile");
    }
}
