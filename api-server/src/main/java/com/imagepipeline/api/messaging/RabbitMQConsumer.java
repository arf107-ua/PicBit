package com.imagepipeline.api.messaging;

import com.imagepipeline.api.config.RabbitMQConfig;
import com.imagepipeline.api.service.NotificationService;
import com.imagepipeline.api.service.ResizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private final ResizeService       resizeService;
    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_IMAGE_RESIZED)
    public void onImageResized(Map<String, Object> message) {
        try {
            String jobId     = (String) message.get("jobId");
            String userId    = (String) message.get("userId");
            String resultKey = (String) message.get("resultKey");

            log.info("Resize completado recibido: jobId={} userId={}", jobId, userId);
            resizeService.onResizeCompleted(jobId, userId, resultKey);

        } catch (Exception e) {
            log.error("Error procesando mensaje image.resized: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_IMAGE_UPLOADED)
    public void onImageUploaded(Map<String, Object> message) {
        try {
            String imageId  = (String) message.get("imageId");
            String authorId = (String) message.get("userId");

            log.info("Nueva imagen subida: imageId={} authorId={}", imageId, authorId);
            notificationService.notifyNewImage(imageId, authorId);

        } catch (Exception e) {
            log.error("Error procesando mensaje image.uploaded: {}", e.getMessage(), e);
        }
    }
}
