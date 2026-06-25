package com.imagepipeline.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResizeService {

    private final ImageService       imageService;
    private final NotificationService notificationService;

    /**
     * Llamado por RabbitMQConsumer cuando image-worker termina un resize.
     * Actualiza el estado en BD y notifica al usuario por WebSocket.
     */
    public void onResizeCompleted(String jobId, String userId, String resultKey) {
        try {
            // 1. Actualizar job en PG y añadir registro en MongoDB
            imageService.completeResizeJob(jobId, resultKey);

            // 2. Notificar al usuario por WebSocket
            String resultUrl = resultKey; // StorageService construye la URL pública
            notificationService.notifyResizeComplete(userId, jobId, resultUrl);

            log.info("Resize completado y notificado: jobId={} userId={}", jobId, userId);

        } catch (Exception e) {
            log.error("Error procesando resize completado: jobId={}", jobId, e);
        }
    }
}
