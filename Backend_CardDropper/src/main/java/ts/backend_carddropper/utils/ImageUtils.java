package ts.backend_carddropper.utils;

import lombok.experimental.UtilityClass;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@UtilityClass
public class ImageUtils {

    public static final int TARGET_WIDTH = 455;
    public static final int TARGET_HEIGHT = 638;

    /**
     * Nettoie un nom de fichier en ne gardant que les caractères sûrs.
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Resize an image to the standard card size (455x638).
     * Returns the resized image as an InputStream in PNG format.
     */
    public InputStream resizeToCardSize(InputStream input) throws IOException {
        BufferedImage original = ImageIO.read(input);
        if (original == null) {
            throw new IOException("Could not read image");
        }

        BufferedImage resized = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, TARGET_WIDTH, TARGET_HEIGHT, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "png", baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }
}
