package com.imagepipeline.worker.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Adds a semi-transparent text watermark to the bottom-right corner of an image.
 *
 * Uses Java2D (Graphics2D) directly — no extra library needed.
 * The watermark scales with the image: font size = image height / 20,
 * so it always looks proportional regardless of input resolution.
 */
@Component
public class WatermarkProcessor {

    private static final Logger log = LoggerFactory.getLogger(WatermarkProcessor.class);

    @Value("${image.watermark.text:© ImagePipeline}")
    private String watermarkText;

    @Value("${image.watermark.opacity:0.4}")
    private float opacity;

    /**
     * @param imageKey  original filename, used only for logging
     * @param input     InputStream of the original image
     * @return          byte array of the watermarked JPEG
     */
    public byte[] process(String imageKey, InputStream input) {
        log.info("Watermarking {}", imageKey);

        try {
            // 1. Read image into a BufferedImage (in-memory pixel grid)
            BufferedImage original = ImageIO.read(input);
            if (original == null) {
                throw new RuntimeException("ImageIO could not read the image: " + imageKey);
            }

            // 2. Create a Graphics2D context to draw on top of the image
            Graphics2D g2d = original.createGraphics();

            // 3. Enable antialiasing for smooth text rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 4. Font size proportional to image height (always readable)
            int fontSize = Math.max(16, original.getHeight() / 20);
            Font font = new Font("Arial", Font.BOLD, fontSize);
            g2d.setFont(font);

            // 5. Measure the text to position it in the bottom-right corner
            FontMetrics metrics = g2d.getFontMetrics(font);
            int textWidth  = metrics.stringWidth(watermarkText);
            int textHeight = metrics.getHeight();

            int x = original.getWidth()  - textWidth  - 20;  // 20px margin right
            int y = original.getHeight() - textHeight  + 10;  // 10px margin bottom

            // 6. Draw a semi-transparent dark shadow first (improves readability on any bg)
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity * 0.5f));
            g2d.setColor(Color.BLACK);
            g2d.drawString(watermarkText, x + 2, y + 2);  // 2px offset for shadow

            // 7. Draw the actual white watermark text
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2d.setColor(Color.WHITE);
            g2d.drawString(watermarkText, x, y);

            g2d.dispose();  // always release Graphics2D resources

            // 8. Encode the modified BufferedImage back to JPEG bytes
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(original, "jpg", output);

            byte[] result = output.toByteArray();
            log.info("Watermark complete: {} → {} bytes", imageKey, result.length);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Watermark failed for: " + imageKey, e);
        }
    }

    /**
     * Builds the output key for the watermarked image.
     * Example: "photo.jpg" → "wm_photo.jpg"
     */
    public String buildResultKey(String imageKey) {
        return "wm_" + imageKey;
    }
}
