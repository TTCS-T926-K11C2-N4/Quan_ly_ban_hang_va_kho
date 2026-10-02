package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.FileObjectDao;
import com.oms.dao.UserDao;
import com.oms.model.AuditLogEntry;
import com.oms.util.AvatarImages;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;
import com.oms.util.FileStorage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

// Ảnh đại diện (S2-03): JPG/PNG tối đa 2MB, cắt vuông, lưu bản 256px và bản thu nhỏ 64px
public class AvatarService {

    public static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final String PURPOSE = "AVATAR";

    private final FileObjectDao fileObjectDao = new FileObjectDao();
    private final UserDao userDao = new UserDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Trả về id file_objects của ảnh mới
    public long upload(long userId, byte[] content, String originalName, String ipAddress)
            throws AvatarImages.InvalidImageException, IOException, SQLException {
        if (content.length == 0) {
            throw new AvatarImages.InvalidImageException("Vui lòng chọn ảnh.");
        }
        if (content.length > MAX_BYTES) {
            throw new AvatarImages.InvalidImageException("Ảnh vượt quá dung lượng tối đa 2MB.");
        }
        BufferedImage square = AvatarImages.readSquare(content);
        byte[] full = AvatarImages.resizePng(square, AvatarImages.FULL_SIZE);
        byte[] thumb = AvatarImages.resizePng(square, AvatarImages.THUMB_SIZE);

        String key = "avatars/" + userId + "-" + UUID.randomUUID() + ".png";
        FileStorage.save(key, full);
        FileStorage.save(thumbKey(key), thumb);
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long fileId = fileObjectDao.insert(connection, key, originalName, "image/png", full.length, PURPOSE,
                        userId);
                userDao.updateAvatar(connection, userId, fileId);
                auditLogDao.insertUserAction(connection, userId, userId, AuditLogEntry.AVATAR_UPDATE, null,
                        JsonUtil.object(Map.of("avatarChanged", true)), null, ipAddress);
                connection.commit();
                return fileId;
            } catch (SQLException e) {
                connection.rollback();
                deleteQuietly(key);
                throw e;
            }
        }
    }

    // Đường dẫn tệp ảnh của người dùng; null nếu chưa có ảnh hoặc tệp đã mất
    public Path findAvatarPath(long userId, boolean thumbnail) throws SQLException {
        String key = fileObjectDao.findAvatarKey(userId);
        if (key == null) {
            return null;
        }
        Path path = FileStorage.resolve(thumbnail ? thumbKey(key) : key);
        return Files.isRegularFile(path) ? path : null;
    }

    static String thumbKey(String key) {
        return key.substring(0, key.length() - ".png".length()) + "-thumb.png";
    }

    private static void deleteQuietly(String key) {
        try {
            Files.deleteIfExists(FileStorage.resolve(key));
            Files.deleteIfExists(FileStorage.resolve(thumbKey(key)));
        } catch (IOException e) {
            // Tệp mồ côi không ảnh hưởng chức năng, chỉ tốn chỗ
        }
    }
}
