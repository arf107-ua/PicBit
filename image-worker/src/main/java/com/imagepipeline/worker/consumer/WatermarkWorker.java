package com.imagepipeline.worker.consumer;

import com.imagepipeline.worker.model.ImageTaskMessage;
import com.imagepipeline.worker.processor.WatermarkProcessor;
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
 * Listens on the "image.watermark" queue.
 * Same ACK pattern as ResizeWorker — see that class for detailed comments.
 */
@Component
public class WatermarkWorker {

    private static final Logger log = LoggerFactory.getLogger(WatermarkWorker.class);

    private final StorageService storageService;
    private final WatermarkProcessor watermarkProcessor;

    public WatermarkWorker(StorageService storageService, WatermarkProcessor watermarkProcessor) {
        this.storageService = storageService;
        this.watermarkProcessor = watermarkProcessor;
    }

    @RabbitListener(
            queues = "${rabbitmq.queues.watermark}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleWatermark(
            ImageTaskMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        log.info("[WatermarkWorker] Received task: {}", message);

        try {
            InputStream original = storageService.download(message.getImageKey());

            byte[] watermarked = watermarkProcessor.process(message.getImageKey(), original);

            String resultKey = watermarkProcessor.buildResultKey(message.getImageKey());
            storageService.upload(resultKey, watermarked, "image/jpeg");

            channel.basicAck(deliveryTag, false);
            log.info("[WatermarkWorker] Done: {} → {}", message.getImageKey(), resultKey);

        } catch (Exception e) {
            log.error("[WatermarkWorker] Failed processing {}: {}", message.getImageKey(), e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackEx) {
                log.error("[WatermarkWorker] Failed to NACK message", nackEx);
            }
        }
    }
}
