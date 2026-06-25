package com.imagepipeline.api.controller;

import com.imagepipeline.api.model.pg.Notification;
import com.imagepipeline.api.repository.pg.NotificationRepository;
import com.imagepipeline.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService    notificationService;

    /**
     * GET /api/notifications
     * Returns all notifications for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(
            notificationRepository.findByRecipientIdOrderByCreatedAtDesc(UUID.fromString(userId))
        );
    }

    /**
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal String userId
    ) {
        long count = notificationService.countUnread(UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * POST /api/notifications/mark-all-read
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal String userId
    ) {
        notificationService.markAllRead(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }
}
