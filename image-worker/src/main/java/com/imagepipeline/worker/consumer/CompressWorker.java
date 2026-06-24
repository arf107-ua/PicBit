package com.imagepipeline.worker.consumer;

import com.imagepipeline.worker.model.ImageTaskMessage;
import com.imagepipeline.worker.processor.CompressProcessor;
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
 * Listens on the "image.compress" queue.
 * Same ACK pattern as ResizeWorker — see that class for detailed comments.
 */
@Component
public class CompressWorker {

    private static final Logger log = LoggerFactory.getLogger(CompressWorker.class);

    private final StorageService storageService;
    private final CompressProcessor compressProcessor;

    public CompressWorker(StorageService storageService, CompressProcessor compressProcessor) {
        this.storageService = storageService;
        this.compressProcessor = compressProcessor;
    }

    @RabbitListener(
            queues = "${rabbitmq.queues.compress}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleCompress(
            ImageTaskMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        log.info("[CompressWorker] Received task: {}", message);

        try {
            InputStream original = storageService.download(message.getImageKey());

            byte[] compressed = compressProcessor.process(message.getImageKey(), original);

            String resultKey = compressProcessor.buildResultKey(message.getImageKey());
            storageService.upload(resultKey, compressed, "image/jpeg");

            channel.basicAck(deliveryTag, false);
            log.info("[CompressWorker] Done: {} → {}", message.getImageKey(), resultKey);

        } catch (Exception e) {
            log.error("[CompressWorker] Failed processing {}: {}", message.getImageKey(), e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackEx) {
                log.error("[CompressWorker] Failed to NACK message", nackEx);
            }
        }
    }
}
