package com.oms.controller;

import com.oms.security.SessionRegistry;
import com.oms.service.AuthService;
import com.oms.service.AvatarService;
import com.oms.util.AvatarImages;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

// S2-03: người dùng tải ảnh đại diện của chính mình (ô file tên "avatar"); xong quay lại trang Hồ sơ
@WebServlet("/profile/avatar")
@MultipartConfig(maxFileSize = AvatarService.MAX_BYTES, maxRequestSize = AvatarService.MAX_BYTES + 64 * 1024)
public class ProfileAvatarServlet extends HttpServlet {

    static final String FLASH_ERROR = "profileFlashError";

    private final AvatarService avatarService = new AvatarService();
    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long userId = CurrentUser.get(request).getId();
        HttpSession session = request.getSession();
        try {
            Part part;
            try {
                part = request.getPart("avatar");
            } catch (IllegalStateException e) {
                // Tomcat báo vượt maxFileSize bằng IllegalStateException
                throw new AvatarImages.InvalidImageException("Ảnh vượt quá dung lượng tối đa 2MB.");
            }
            if (part == null || part.getSize() == 0) {
                throw new AvatarImages.InvalidImageException("Vui lòng chọn ảnh.");
            }
            byte[] content;
            try (InputStream in = part.getInputStream()) {
                content = in.readAllBytes();
            }
            avatarService.upload(userId, content, fileName(part.getSubmittedFileName()), request.getRemoteAddr());
            SessionRegistry.replaceUser(authService.loadSessionUser(userId));
            session.setAttribute(ProfileServlet.FLASH_MESSAGE, "Đã cập nhật ảnh đại diện.");
        } catch (AvatarImages.InvalidImageException e) {
            session.setAttribute(FLASH_ERROR, e.getMessage());
        } catch (SQLException e) {
            log("Không lưu được ảnh đại diện", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/profile");
    }

    // Chỉ giữ tên tệp (một số trình duyệt gửi cả đường dẫn trên máy), cắt cho vừa cột original_name
    private static String fileName(String submitted) {
        if (submitted == null || submitted.isBlank()) {
            return "avatar";
        }
        String name = submitted.substring(Math.max(submitted.lastIndexOf('/'), submitted.lastIndexOf('\\')) + 1);
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
