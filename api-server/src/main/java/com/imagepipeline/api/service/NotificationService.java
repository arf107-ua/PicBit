package com.imagepipeline.api.service;

import com.imagepipeline.api.model.dto.WsEvent;
import com.imagepipeline.api.model.pg.Notification;
import com.imagepipeline.api.model.pg.User;
import com.imagepipeline.api.repository.pg.NotificationRepository;
import com.imagepipeline.api.repository.pg.UserRepository;
import com.imagepipeline.api.repository.pg.ImageRepository;
import com.imagepipeline.api.socket.SessionRegistry;
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

    public void notifyResizeComplete(String userId, String jobId, String resultUrl) {
        User recipient = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (recipient == null) return;

        Notification notification = Notification.builder()
            .recipient(recipient)
            .type(Notification.NotificationType.resize_complete)
            .message("Tu imagen ha sido redimensionada correctamente")
            .build();
        notificationRepository.save(notification);

        sessionRegistry.sendToUser(userId, new WsEvent("resize_complete", Map.of(
            "jobId", jobId,
            "resultUrl", resultUrl,
            "notificationId", notification.getId().toString()
        )));

        log.info("Notificación resize_complete enviada a userId={}", userId);
    }

    public void notifyNewImage(String imageId, String authorId) {
        sessionRegistry.broadcastToRoom("feed", new WsEvent("new_image", Map.of(
            "imageId", imageId,
            "authorId", authorId
        )));
    }

    public void markAllRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }
}
