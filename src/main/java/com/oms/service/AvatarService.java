package com.oms.service;

import com.oms.dao.FileObjectDao;
import com.oms.dao.UserDao;
import com.oms.model.StoredFile;
import com.oms.util.DbConnection;
import com.oms.util.FileStorage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

// Ảnh đại diện (S2-03): chỉ JPG/PNG tối đa 2MB, cắt vuông giữa ảnh, lưu bản lớn và bản thu nhỏ
public class AvatarService {

    public static final long MAX_BYTES = 2L * 1024 * 1024;
    public static final int FULL_SIZE = 256;
    public static final int THUMB_SIZE = 64;
    // Ảnh PNG 2MB vẫn có thể khai báo kích thước rất lớn và làm hết bộ nhớ khi giải nén
    private static final int MAX_SOURCE_SIDE = 6000;
    private static final int ORIGINAL_NAME_MAX_LENGTH = 255;

    private final UserDao userDao = new UserDao();
    private final FileObjectDao fileObjectDao = new FileObjectDao();

    // Ảnh đã kiểm tra và cắt vuông, sẵn sàng lưu
    public static final class ProcessedAvatar {

        private final String extension;
        private final String contentType;
        private final byte[] full;
        private final byte[] thumbnail;

        ProcessedAvatar(String extension, String contentType, byte[] full, byte[] thumbnail) {
            this.extension = extension;
            this.contentType = contentType;
            this.full = full;
            this.thumbnail = thumbnail;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getFull() {
            return full;
        }

        public byte[] getThumbnail() {
            return thumbnail;
        }
    }

    // Nhận diện định dạng theo nội dung tệp, không tin đuôi tệp hay Content-Type trình duyệt gửi lên
    public ProcessedAvatar process(byte[] data) throws InvalidAvatarException {
        if (data == null || data.length == 0) {
            throw new InvalidAvatarException("Vui lòng chọn ảnh.");
        }
        if (data.length > MAX_BYTES) {
            throw new InvalidAvatarException("Ảnh tối đa 2MB.");
        }

        String format;
        BufferedImage source;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidAvatarException("Chỉ chấp nhận ảnh JPG hoặc PNG.");
            }
            ImageReader reader = readers.next();
            try {
                format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!"jpeg".equals(format) && !"png".equals(format)) {
                    throw new InvalidAvatarException("Chỉ chấp nhận ảnh JPG hoặc PNG.");
                }
                reader.setInput(input, true, true);
                if (reader.getWidth(0) > MAX_SOURCE_SIDE || reader.getHeight(0) > MAX_SOURCE_SIDE) {
                    throw new InvalidAvatarException("Ảnh tối đa " + MAX_SOURCE_SIDE + "×" + MAX_SOURCE_SIDE
                            + " điểm ảnh.");
                }
                source = reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            // Tệp hỏng hoặc JPG hệ màu CMYK mà ImageIO không đọc được
            throw new InvalidAvatarException("Không đọc được ảnh. Hãy chọn ảnh JPG hoặc PNG khác.");
        }

        boolean png = "png".equals(format);
        String extension = png ? "png" : "jpg";
        try {
            return new ProcessedAvatar(extension, png ? "image/png" : "image/jpeg",
                    encode(cropSquare(source, FULL_SIZE, png), extension),
                    encode(cropSquare(source, THUMB_SIZE, png), extension));
        } catch (IOException e) {
            throw new InvalidAvatarException("Không xử lý được ảnh. Hãy chọn ảnh khác.");
        }
    }

    // Ghi tệp trước rồi mới đổi DB: DB lỗi thì xoá tệp mới; xong thì xoá tệp ảnh cũ.
    // Trả về id file_objects của ảnh mới.
    public long save(long userId, String originalName, ProcessedAvatar avatar) throws SQLException, IOException {
        String storageKey = "avatars/" + UUID.randomUUID() + "." + avatar.extension;
        FileStorage.write(storageKey, avatar.full);
        FileStorage.write(thumbnailKey(storageKey), avatar.thumbnail);

        StoredFile oldFile = null;
        long newFileId;
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Long oldFileId = userDao.findAvatarFileIdForUpdate(connection, userId);
                newFileId = fileObjectDao.insert(connection, storageKey, shorten(originalName), avatar.contentType,
                        avatar.full.length, FileObjectDao.PURPOSE_AVATAR, userId);
                userDao.updateAvatar(connection, userId, newFileId);
                if (oldFileId != null) {
                    oldFile = fileObjectDao.find(connection, oldFileId, FileObjectDao.PURPOSE_AVATAR);
                    fileObjectDao.delete(connection, oldFileId);
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            FileStorage.deleteQuietly(storageKey);
            FileStorage.deleteQuietly(thumbnailKey(storageKey));
            throw e;
        }

        if (oldFile != null) {
            FileStorage.deleteQuietly(oldFile.getStorageKey());
            FileStorage.deleteQuietly(thumbnailKey(oldFile.getStorageKey()));
        }
        return newFileId;
    }

    public StoredFile findAvatar(long fileId) throws SQLException {
        return fileObjectDao.findByIdAndPurpose(fileId, FileObjectDao.PURPOSE_AVATAR);
    }

    // Bản thu nhỏ nằm cạnh bản lớn, cùng tên thêm hậu tố _thumb, nên không cần dòng file_objects riêng
    public static String thumbnailKey(String storageKey) {
        int dot = storageKey.lastIndexOf('.');
        return storageKey.substring(0, dot) + "_thumb" + storageKey.substring(dot);
    }

    // Lấy phần vuông ở giữa ảnh rồi thu/phóng về size × size
    static BufferedImage cropSquare(BufferedImage source, int size, boolean keepTransparency) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;

        BufferedImage result = new BufferedImage(size, size,
                keepTransparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (!keepTransparency) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, size, size);
            }
            graphics.drawImage(source, 0, 0, size, size, x, y, x + side, y + side, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static byte[] encode(BufferedImage image, String extension) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, extension, output)) {
            throw new IOException("Không có bộ ghi ảnh " + extension);
        }
        return output.toByteArray();
    }

    private static String shorten(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "avatar";
        }
        return originalName.length() > ORIGINAL_NAME_MAX_LENGTH
                ? originalName.substring(0, ORIGINAL_NAME_MAX_LENGTH) : originalName;
    }
}
