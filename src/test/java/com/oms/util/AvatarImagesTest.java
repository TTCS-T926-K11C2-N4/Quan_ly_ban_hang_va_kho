package com.oms.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// S2-03: chỉ nhận JPG/PNG theo nội dung, cắt vuông và thu nhỏ
class AvatarImagesTest {

    private static byte[] image(String format, int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }

    @Test
    void cropsCenterSquareAndResizes() throws Exception {
        BufferedImage square = AvatarImages.readSquare(image("jpg", 400, 300));
        assertEquals(300, square.getWidth());
        assertEquals(300, square.getHeight());

        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(AvatarImages.resizePng(square, 64)));
        assertEquals(64, thumb.getWidth());
        assertEquals(64, thumb.getHeight());
    }

    @Test
    void acceptsPng() throws Exception {
        assertEquals(50, AvatarImages.readSquare(image("png", 50, 80)).getWidth());
    }

    @Test
    void rejectsOtherFormatsEvenWithImageExtension() throws Exception {
        assertThrows(AvatarImages.InvalidImageException.class,
                () -> AvatarImages.readSquare(image("gif", 20, 20)), "GIF không được nhận");
        assertThrows(AvatarImages.InvalidImageException.class,
                () -> AvatarImages.readSquare("không phải ảnh".getBytes(StandardCharsets.UTF_8)));
    }
}
