package com.oms.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Lưu tệp tải lên (ảnh đại diện...) ra đĩa, ngoài thư mục webapps để deploy lại không mất.
// Thư mục gốc: biến môi trường UPLOAD_DIR (đặt trong setenv.bat), mặc định <thư mục Tomcat>/uploads.
public final class FileStorage {

    private FileStorage() {
    }

    public static Path root() {
        String configured = System.getenv("UPLOAD_DIR");
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        String catalinaBase = System.getProperty("catalina.base");
        return catalinaBase != null ? Paths.get(catalinaBase, "uploads") : Paths.get(System.getProperty("user.home"), "oms-uploads");
    }

    // storageKey do hệ thống sinh (vd avatars/12-uuid.png), không lấy từ tên tệp người dùng gửi lên
    public static void save(String storageKey, byte[] content) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
    }

    public static Path resolve(String storageKey) {
        Path root = root().toAbsolutePath().normalize();
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ: " + storageKey);
        }
        return target;
    }
}
