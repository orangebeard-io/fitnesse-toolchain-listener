package io.orangebeard.listener.helper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;

public class ImageEncoder {

    public static String encodeForEmbedding(File file) throws IOException {
        BufferedImage imageToEncode = resizeImage(ImageIO.read(file), 200, -1);
        ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
        ImageIO.write(imageToEncode, "jpg", imageOutputStream);
        byte[] imageBytes = imageOutputStream.toByteArray();

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static BufferedImage resizeImage(final Image image, int width, int height) {
        double ratio = (double) image.getWidth(null) / (double) image.getHeight(null);
        if (width < 1 && height < 1) {
            throw new IllegalArgumentException("Width or height must be larger than 0!");
        }
        if (width < 1) {
            width = (int) (height * ratio);
        } else if (height < 1) {
            height = (int) (width / ratio);
        }

        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
        g2d.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();

        return resizedImage;
    }
}
