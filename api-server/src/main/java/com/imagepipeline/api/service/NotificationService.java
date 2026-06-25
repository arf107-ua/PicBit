package com.imagepipeline.api.service;

import com.imagepipeline.api.model.pg.*;
import com.imagepipeline.api.repository.pg.*;
import com.imagepipeline.api.socket.SessionRegistry;
import com.imagepipeline.api.model.dto.Dtos.WsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;
    private final ImageRepository        imageRepository;
    private final SessionRegistry        sessionRegistry;

    /**
     * Notifica al usuario que su resize ha terminado.
     */
    public void notifyResizeComplete(String userId, String jobId, String resultUrl) {
        User recipient = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (recipient == null) return;

        // Guardar en BD
        Notification notification = Notification.builder()
            .recipient(recipient)
            .type(Notification.NotificationType.resize_complete)
            .message("Tu imagen ha sido redimensionada correctamente")
            .build();
        notificationRepository.save(notification);

        // Enviar por WebSocket en tiempo real
        sessionRegistry.sendToUser(userId, new WsEvent("resize_complete", Map.of(
            "jobId", jobId,
            "resultUrl", resultUrl,
            "notificationId", notification.getId().toString()
        )));

        log.info("Notificación resize_complete enviada a userId={}", userId);
    }

    /**
     * Notifica a los seguidores de un usuario que hay una imagen nueva.
     */
    public void notifyNewImage(String imageId, String authorId) {
        // Broadcast a todos los seguidores conectados
        sessionRegistry.broadcastToRoom("feed", new WsEvent("new_image", Map.of(
            "imageId", imageId,
            "authorId", authorId
        )));
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas.
     */
    public void markAllRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /**
     * Cuenta notificaciones no leídas.
     */
    public long countUnread(UUID userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }
}
