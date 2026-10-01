package com.oms.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S2-03: chỉ JPG/PNG tối đa 2MB, cắt vuông và tạo bản thu nhỏ
class AvatarServiceTest {

    private final AvatarService avatarService = new AvatarService();

    // Ảnh ngang 400×200: hai dải đỏ hai bên, phần vuông 200×200 ở giữa màu xanh
    private static byte[] landscape(String format, int imageType) throws IOException {
        BufferedImage image = new BufferedImage(400, 200, imageType);
        for (int x = 0; x < 400; x++) {
            int rgb = x < 100 || x >= 300 ? Color.RED.getRGB() : Color.BLUE.getRGB();
            for (int y = 0; y < 200; y++) {
                image.setRGB(x, y, rgb);
            }
        }
        return write(image, format);
    }

    private static byte[] write(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private static BufferedImage read(byte[] data) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    private static boolean isMostlyBlue(int rgb) {
        Color color = new Color(rgb);
        return color.getBlue() > 200 && color.getRed() < 60;
    }

    @Test
    void jpegIsCroppedToCenterSquareInTwoSizes() throws Exception {
        AvatarService.ProcessedAvatar avatar = avatarService.process(landscape("jpg", BufferedImage.TYPE_INT_RGB));
        assertEquals("image/jpeg", avatar.getContentType());

        BufferedImage full = read(avatar.getFull());
        BufferedImage thumbnail = read(avatar.getThumbnail());
        assertEquals(AvatarService.FULL_SIZE, full.getWidth());
        assertEquals(AvatarService.FULL_SIZE, full.getHeight());
        assertEquals(AvatarService.THUMB_SIZE, thumbnail.getWidth());
        assertEquals(AvatarService.THUMB_SIZE, thumbnail.getHeight());
        // Hai dải đỏ bị cắt bỏ: cả mép trái lẫn mép phải đều là phần xanh ở giữa
        assertTrue(isMostlyBlue(full.getRGB(5, 128)));
        assertTrue(isMostlyBlue(full.getRGB(250, 128)));
    }

    @Test
    void pngStaysPngAndKeepsTransparency() throws Exception {
        BufferedImage transparent = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        AvatarService.ProcessedAvatar avatar = avatarService.process(write(transparent, "png"));
        assertEquals("image/png", avatar.getContentType());
        assertEquals(0, read(avatar.getFull()).getRGB(10, 10) >>> 24);
    }

    @Test
    void otherFormatsAndBrokenFilesAreRejected() throws Exception {
        BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        InvalidAvatarException gif = assertThrows(InvalidAvatarException.class,
                () -> avatarService.process(write(image, "gif")));
        assertEquals("Chỉ chấp nhận ảnh JPG hoặc PNG.", gif.getMessage());

        assertThrows(InvalidAvatarException.class,
                () -> avatarService.process("đây không phải ảnh".getBytes()));
        // Đuôi .png nhưng nội dung hỏng sau phần đầu tệp
        byte[] png = write(image, "png");
        byte[] broken = Arrays.copyOf(png, 40);
        assertThrows(InvalidAvatarException.class, () -> avatarService.process(broken));
    }

    @Test
    void emptyOrTooLargeFileIsRejected() {
        assertEquals("Vui lòng chọn ảnh.",
                assertThrows(InvalidAvatarException.class, () -> avatarService.process(new byte[0])).getMessage());
        assertEquals("Ảnh tối đa 2MB.", assertThrows(InvalidAvatarException.class,
                () -> avatarService.process(new byte[(int) AvatarService.MAX_BYTES + 1])).getMessage());
    }

    @Test
    void hugeDimensionsAreRejectedBeforeDecoding() throws Exception {
        byte[] wide = write(new BufferedImage(6001, 1, BufferedImage.TYPE_INT_RGB), "png");
        assertTrue(wide.length < AvatarService.MAX_BYTES);
        assertThrows(InvalidAvatarException.class, () -> avatarService.process(wide));
    }

    @Test
    void thumbnailSitsNextToFullImage() {
        assertEquals("avatars/abc_thumb.png", AvatarService.thumbnailKey("avatars/abc.png"));
        assertEquals("avatars/a.b_thumb.jpg", AvatarService.thumbnailKey("avatars/a.b.jpg"));
    }
}
