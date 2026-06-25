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

    public void publishResizeJob(String jobId, String storageKey, String userId,
                                  Integer width, Integer height) {
        ImageTaskMessage message = new ImageTaskMessage(
            jobId, storageKey, userId, "resize", width, height
        );
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_IMAGES, RabbitMQConfig.RK_RESIZE, message
        );
        log.info("Resize job publicado: jobId={} {}x{}", jobId, width, height);
    }

    public void publishCompressJob(String jobId, String storageKey, String userId) {
        ImageTaskMessage message = new ImageTaskMessage(
            jobId, storageKey, userId, "compress", null, null
        );
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_IMAGES, RabbitMQConfig.RK_COMPRESS, message
        );
        log.info("Compress job publicado: jobId={}", jobId);
    }

    public void publishWatermarkJob(String jobId, String storageKey, String userId) {
        ImageTaskMessage message = new ImageTaskMessage(
            jobId, storageKey, userId, "watermark", null, null
        );
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_IMAGES, RabbitMQConfig.RK_WATERMARK, message
        );
        log.info("Watermark job publicado: jobId={}", jobId);
    }

    public void publishImageUploaded(String imageId, String userId) {
        Map<String, String> payload = Map.of("imageId", imageId, "userId", userId);
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_IMAGES, RabbitMQConfig.RK_UPLOADED, payload
        );
        log.info("Evento image.uploaded publicado: imageId={}", imageId);
    }
}
