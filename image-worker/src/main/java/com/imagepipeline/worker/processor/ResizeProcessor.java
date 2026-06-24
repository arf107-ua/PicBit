package com.imagepipeline.worker.processor;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Resizes an image to a thumbnail.
 *
 * Uses Thumbnailator which internally applies the Lanczos algorithm
 * — much better visual quality than basic Java bilinear scaling.
 *
 * keepAspectRatio(true) → if the original is 1920×1080 and target is 200×200,
 * the result will be 200×112 (no distortion).
 */
@Component
public class ResizeProcessor {

    private static final Logger log = LoggerFactory.getLogger(ResizeProcessor.class);

    @Value("${image.resize.target-width:200}")
    private int targetWidth;

    @Value("${image.resize.target-height:200}")
    private int targetHeight;

    /**
     * @param imageKey  original filename, used only for logging
     * @param input     InputStream of the original image
     * @return          byte array of the resized JPEG
     */
    public byte[] process(String imageKey, InputStream input) {
        log.info("Resizing {} to {}x{}", imageKey, targetWidth, targetHeight);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            Thumbnails.of(input)
                    .size(targetWidth, targetHeight)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(output);

            byte[] result = output.toByteArray();
            log.info("Resize complete: {} → {} bytes", imageKey, result.length);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Resize failed for: " + imageKey, e);
        }
    }

    /**
     * Builds the output key for the resized image.
     * Example: "photo.jpg" → "thumb_photo.jpg"
     */
    public String buildResultKey(String imageKey) {
        return "thumb_" + imageKey;
    }
}
