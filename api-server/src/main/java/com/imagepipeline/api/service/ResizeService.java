package com.imagepipeline.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResizeService {

    private final ImageService        imageService;
    private final NotificationService notificationService;

    public void onResizeCompleted(String jobId, String userId, String resultKey) {
        try {
            imageService.completeResizeJob(jobId, resultKey);
            notificationService.notifyResizeComplete(userId, jobId, resultKey);
            log.info("Resize completado y notificado: jobId={} userId={}", jobId, userId);
        } catch (Exception e) {
            log.error("Error procesando resize completado: jobId={}", jobId, e);
        }
    }
}
