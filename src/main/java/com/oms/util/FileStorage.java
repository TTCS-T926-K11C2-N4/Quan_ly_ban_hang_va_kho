package com.oms.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Tệp tải lên (ảnh đại diện...) lưu trong thư mục UPLOAD_DIR trên server, ngoài thư mục webapp để
// deploy lại không mất. Đường dẫn tương đối (storage_key) lưu trong bảng file_objects.
public final class FileStorage {

    private static final String UPLOAD_DIR = System.getenv("UPLOAD_DIR");

    private FileStorage() {
    }

    public static void write(String storageKey, byte[] data) throws IOException {
        Path path = resolve(storageKey);
        Files.createDirectories(path.getParent());
        Files.write(path, data);
    }

    public static Path resolve(String storageKey) throws IOException {
        if (UPLOAD_DIR == null || UPLOAD_DIR.isBlank()) {
            throw new IOException("Chưa cấu hình biến môi trường UPLOAD_DIR");
        }
        Path root = Path.of(UPLOAD_DIR).toAbsolutePath().normalize();
        Path path = root.resolve(storageKey).normalize();
        // storage_key do hệ thống sinh nhưng vẫn chặn "../" để không đọc/ghi ra ngoài UPLOAD_DIR
        if (!path.startsWith(root)) {
            throw new IOException("Đường dẫn tệp không hợp lệ: " + storageKey);
        }
        return path;
    }

    // Không ném lỗi: xoá tệp cũ thất bại chỉ để lại tệp thừa, không ảnh hưởng dữ liệu
    public static void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            // Bỏ qua
        }
    }
}
