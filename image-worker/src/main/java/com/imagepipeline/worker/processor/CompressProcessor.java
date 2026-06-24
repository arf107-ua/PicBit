package com.imagepipeline.worker.processor;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Compresses an image keeping its original dimensions.
 *
 * Strategy: re-encode to JPEG at reduced quality.
 * quality 0.75 → typically 40-60% smaller file with no visible degradation
 * at normal screen sizes.
 *
 * Does NOT resize — use ResizeProcessor for that.
 */
@Component
public class CompressProcessor {

    private static final Logger log = LoggerFactory.getLogger(CompressProcessor.class);

    @Value("${image.compress.quality:0.75}")
    private double quality;

    /**
     * @param imageKey  original filename, used only for logging
     * @param input     InputStream of the original image
     * @return          byte array of the compressed JPEG
     */
    public byte[] process(String imageKey, InputStream input) {
        log.info("Compressing {} at quality {}", imageKey, quality);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            Thumbnails.of(input)
                    .scale(1.0)           // keep original dimensions
                    .outputFormat("jpg")
                    .outputQuality(quality)
                    .toOutputStream(output);

            byte[] result = output.toByteArray();
            log.info("Compress complete: {} → {} bytes", imageKey, result.length);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Compression failed for: " + imageKey, e);
        }
    }

    /**
     * Builds the output key for the compressed image.
     * Example: "photo.jpg" → "web_photo.jpg"
     */
    public String buildResultKey(String imageKey) {
        return "web_" + imageKey;
    }
}
