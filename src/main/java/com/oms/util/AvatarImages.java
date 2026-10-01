package com.oms.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

// Xử lý ảnh đại diện (S2-03): chỉ nhận JPG/PNG theo nội dung tệp (không tin đuôi tên tệp),
// cắt hình vuông ở giữa rồi thu nhỏ, xuất PNG.
public final class AvatarImages {

    public static final int FULL_SIZE = 256;
    public static final int THUMB_SIZE = 64;
    // Chặn ảnh kích thước khổng lồ nén rất nhỏ (giải nén ra hàng GB bộ nhớ)
    private static final int MAX_DIMENSION = 6000;

    private AvatarImages() {
    }

    public static class InvalidImageException extends Exception {
        public InvalidImageException(String message) {
            super(message);
        }
    }

    // Đọc và kiểm tra ảnh, trả về ảnh đã cắt vuông (chưa thu nhỏ)
    public static BufferedImage readSquare(byte[] content) throws InvalidImageException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = input == null ? null : ImageIO.getImageReaders(input);
            if (readers == null || !readers.hasNext()) {
                throw new InvalidImageException("Tệp không phải ảnh JPG hoặc PNG.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png")) {
                    throw new InvalidImageException("Chỉ nhận ảnh JPG hoặc PNG.");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new InvalidImageException("Ảnh quá lớn (tối đa " + MAX_DIMENSION + " × " + MAX_DIMENSION + " điểm ảnh).");
                }
                BufferedImage image = reader.read(0);
                int side = Math.min(image.getWidth(), image.getHeight());
                return image.getSubimage((image.getWidth() - side) / 2, (image.getHeight() - side) / 2, side, side);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidImageException("Không đọc được ảnh. Hãy chọn tệp JPG hoặc PNG khác.");
        }
    }

    public static byte[] resizePng(BufferedImage square, int size) throws IOException {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(square, 0, 0, size, size, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(scaled, "png", out);
        return out.toByteArray();
    }
}
