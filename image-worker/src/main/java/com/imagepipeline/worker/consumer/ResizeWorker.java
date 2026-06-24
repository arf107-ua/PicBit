package com.imagepipeline.worker.consumer;

import com.imagepipeline.worker.model.ImageTaskMessage;
import com.imagepipeline.worker.processor.ResizeProcessor;
import com.imagepipeline.worker.service.StorageService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Listens on the "image.resize" queue.
 *
 * Manual ACK explained:
 *   - Spring receives the message and calls this method.
 *   - We only call channel.basicAck() AFTER the result is saved to storage.
 *   - If anything fails before that, channel.basicNack() sends the message
 *     to the Dead Letter Queue (DLQ) instead of losing it.
 *
 * prefetch=1 (set in application.yml) means this worker processes
 * one message at a time — no message piling up while one is in progress.
 */
@Component
public class ResizeWorker {

    private static final Logger log = LoggerFactory.getLogger(ResizeWorker.class);

    private final StorageService storageService;
    private final ResizeProcessor resizeProcessor;

    public ResizeWorker(StorageService storageService, ResizeProcessor resizeProcessor) {
        this.storageService = storageService;
        this.resizeProcessor = resizeProcessor;
    }

    @RabbitListener(
            queues = "${rabbitmq.queues.resize}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleResize(
            ImageTaskMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        log.info("[ResizeWorker] Received task: {}", message);

        try {
            // Step 1: download the original image from MinIO
            InputStream original = storageService.download(message.getImageKey());

            // Step 2: apply the resize transformation
            byte[] resized = resizeProcessor.process(message.getImageKey(), original);

            // Step 3: upload the result to the output bucket
            String resultKey = resizeProcessor.buildResultKey(message.getImageKey());
            storageService.upload(resultKey, resized, "image/jpeg");

            // Step 4: ACK — tell RabbitMQ the task is done, remove from queue
            channel.basicAck(deliveryTag, false);
            log.info("[ResizeWorker] Done: {} → {}", message.getImageKey(), resultKey);

        } catch (Exception e) {
            log.error("[ResizeWorker] Failed processing {}: {}", message.getImageKey(), e.getMessage());
            try {
                // NACK with requeue=false → message goes to Dead Letter Queue
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackEx) {
                log.error("[ResizeWorker] Failed to NACK message", nackEx);
            }
        }
    }
}
