package com.imagepipeline.api.service;

import com.imagepipeline.api.config.RabbitMQConfig;
import com.imagepipeline.api.model.ImageTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publica un job de resize para que image-worker lo procese.
     */
    public void publishResizeJob(String jobId, String storageKey, String userId,
                                  Integer width, Integer height) {
        ImageTaskMessage message = new ImageTaskMessage();
        message.setJobId(jobId);
        message.setStorageKey(storageKey);
        message.setUserId(userId);
        message.setWidth(width);
        message.setHeight(height);
        message.setOperation("resize");

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_IMAGES,
            RabbitMQConfig.RK_RESIZE,
            message
        );
        log.info("Resize job publicado: jobId={} {}x{}", jobId, width, height);
    }

    /**
     * Publica un job de compresión.
     */
    public void publishCompressJob(String jobId, String storageKey, String userId) {
        ImageTaskMessage message = new ImageTaskMessage();
        message.setJobId(jobId);
        message.setStorageKey(storageKey);
        message.setUserId(userId);
        message.setOperation("compress");

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_IMAGES,
            RabbitMQConfig.RK_COMPRESS,
            message
        );
        log.info("Compress job publicado: jobId={}", jobId);
    }

    /**
     * Publica un job de marca de agua.
     */
    public void publishWatermarkJob(String jobId, String storageKey, String userId) {
        ImageTaskMessage message = new ImageTaskMessage();
        message.setJobId(jobId);
        message.setStorageKey(storageKey);
        message.setUserId(userId);
        message.setOperation("watermark");

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_IMAGES,
            RabbitMQConfig.RK_WATERMARK,
            message
        );
        log.info("Watermark job publicado: jobId={}", jobId);
    }

    /**
     * Notifica a los seguidores que hay una imagen nueva.
     */
    public void publishImageUploaded(String imageId, String userId) {
        Map<String, String> payload = Map.of(
            "imageId", imageId,
            "userId", userId
        );
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_IMAGES,
            RabbitMQConfig.RK_UPLOADED,
            payload
        );
        log.info("Evento image.uploaded publicado: imageId={}", imageId);
    }
}
