package com.imagepipeline.api.service;

import com.imagepipeline.api.model.dto.Dtos.*;
import com.imagepipeline.api.model.mongo.ImageDocument;
import com.imagepipeline.api.model.pg.Image;
import com.imagepipeline.api.model.pg.ResizeJob;
import com.imagepipeline.api.model.pg.User;
import com.imagepipeline.api.repository.mongo.ImageDocumentRepository;
import com.imagepipeline.api.repository.pg.ImageRepository;
import com.imagepipeline.api.repository.pg.ResizeJobRepository;
import com.imagepipeline.api.repository.pg.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository        imageRepository;
    private final ImageDocumentRepository imageDocumentRepository;
    private final ResizeJobRepository    resizeJobRepository;
    private final UserRepository         userRepository;
    private final StorageService         storageService;
    private final QueueService           queueService;

    /**
     * Sube una imagen: guarda en Supabase Storage, MongoDB y PostgreSQL.
     */
    @Transactional
    public ImageResponse upload(MultipartFile file, UploadImageRequest request, UUID userId) throws Exception {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Subir archivo a Supabase Storage
        String storageKey = storageService.upload(file, userId.toString());

        // 2. Guardar metadatos ricos en MongoDB
        ImageDocument doc = ImageDocument.builder()
            .tags(request.getTags())
            .originalDimensions(new ImageDocument.Dimensions(null, null)) // se rellena con EXIF si hay
            .createdAt(java.time.LocalDateTime.now())
            .build();
        ImageDocument savedDoc = imageDocumentRepository.save(doc);

        // 3. Guardar referencia ligera en PostgreSQL
        Image image = Image.builder()
            .user(user)
            .mongoId(savedDoc.getId())
            .title(request.getTitle())
            .description(request.getDescription())
            .storageKey(storageKey)
            .isPublic(request.getIsPublic())
            .build();
        Image savedImage = imageRepository.save(image);

        // 4. Actualizar MongoDB con el ID de PG (puente bidireccional)
        savedDoc.setPgImageId(savedImage.getId().toString());
        imageDocumentRepository.save(savedDoc);

        // 5. Publicar evento en RabbitMQ para notificar a seguidores
        queueService.publishImageUploaded(savedImage.getId().toString(), userId.toString());

        log.info("Imagen subida: pg={} mongo={}", savedImage.getId(), savedDoc.getId());
        return toResponse(savedImage, savedDoc);
    }

    /**
     * Obtiene una imagen combinando datos de PG y MongoDB.
     */
    public ImageResponse getById(UUID imageId) {
        Image image = imageRepository.findById(imageId)
            .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));

        ImageDocument doc = imageDocumentRepository.findById(image.getMongoId())
            .orElseThrow(() -> new RuntimeException("Metadatos no encontrados"));

        return toResponse(image, doc);
    }

    /**
     * Feed personalizado del usuario.
     */
    public List<ImageResponse> getFeed(UUID userId) {
        return imageRepository.findFeedForUser(userId).stream()
            .map(image -> {
                ImageDocument doc = imageDocumentRepository
                    .findById(image.getMongoId()).orElse(new ImageDocument());
                return toResponse(image, doc);
            })
            .collect(Collectors.toList());
    }

    /**
     * Imágenes públicas (explorar).
     */
    public List<ImageResponse> getPublic() {
        return imageRepository.findByIsPublicTrueOrderByCreatedAtDesc().stream()
            .map(image -> {
                ImageDocument doc = imageDocumentRepository
                    .findById(image.getMongoId()).orElse(new ImageDocument());
                return toResponse(image, doc);
            })
            .collect(Collectors.toList());
    }

    /**
     * Crea un job de resize y lo publica en RabbitMQ.
     */
    @Transactional
    public ResizeJobResponse requestResize(ResizeRequest request, UUID userId) {
        Image image = imageRepository.findById(UUID.fromString(request.getImageId()))
            .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Guardar el job en PG con estado pending
        ResizeJob job = ResizeJob.builder()
            .image(image)
            .user(user)
            .width(request.getWidth())
            .height(request.getHeight())
            .status(ResizeJob.JobStatus.pending)
            .build();
        ResizeJob savedJob = resizeJobRepository.save(job);

        // Publicar en RabbitMQ para que image-worker lo procese
        queueService.publishResizeJob(
            savedJob.getId().toString(),
            image.getStorageKey(),
            userId.toString(),
            request.getWidth(),
            request.getHeight()
        );

        log.info("Resize job creado: {}", savedJob.getId());
        return toJobResponse(savedJob);
    }

    /**
     * Actualiza el job tras recibir la notificación del worker.
     */
    @Transactional
    public void completeResizeJob(String jobId, String resultKey) {
        ResizeJob job = resizeJobRepository.findById(UUID.fromString(jobId))
            .orElseThrow(() -> new RuntimeException("Job no encontrado"));

        job.setStatus(ResizeJob.JobStatus.done);
        job.setResultKey(resultKey);
        resizeJobRepository.save(job);

        // Actualizar historial en MongoDB
        imageDocumentRepository.findById(job.getImage().getMongoId()).ifPresent(doc -> {
            ImageDocument.ProcessingRecord record = new ImageDocument.ProcessingRecord(
                "resize", resultKey, job.getWidth(), job.getHeight(), java.time.LocalDateTime.now()
            );
            if (doc.getProcessingHistory() == null) {
                doc.setProcessingHistory(new java.util.ArrayList<>());
            }
            doc.getProcessingHistory().add(record);
            imageDocumentRepository.save(doc);
        });
    }

    // ── Mappers ─────────────────────────────────────────────────────

    private ImageResponse toResponse(Image image, ImageDocument doc) {
        ImageResponse response = new ImageResponse();
        response.setId(image.getId().toString());
        response.setMongoId(image.getMongoId());
        response.setTitle(image.getTitle());
        response.setDescription(image.getDescription());
        response.setStorageUrl(storageService.getPublicUrl(image.getStorageKey()));
        response.setIsPublic(image.getIsPublic());
        response.setTags(doc.getTags());
        response.setColorPalette(doc.getColorPalette());
        response.setCreatedAt(image.getCreatedAt() != null ? image.getCreatedAt().toString() : null);

        if (doc.getCurrentDimensions() != null) {
            ImageResponse.DimensionsDto dims = new ImageResponse.DimensionsDto();
            dims.setWidth(doc.getCurrentDimensions().getWidth());
            dims.setHeight(doc.getCurrentDimensions().getHeight());
            response.setDimensions(dims);
        }

        if (image.getUser() != null) {
            ImageResponse.UserSummaryDto userDto = new ImageResponse.UserSummaryDto();
            userDto.setId(image.getUser().getId().toString());
            userDto.setUsername(image.getUser().getUsername());
            userDto.setAvatarUrl(image.getUser().getAvatarUrl());
            response.setUser(userDto);
        }

        return response;
    }

    private ResizeJobResponse toJobResponse(ResizeJob job) {
        ResizeJobResponse response = new ResizeJobResponse();
        response.setJobId(job.getId().toString());
        response.setImageId(job.getImage().getId().toString());
        response.setStatus(job.getStatus().name());
        response.setWidth(job.getWidth());
        response.setHeight(job.getHeight());
        response.setResultUrl(job.getResultKey() != null
            ? storageService.getPublicUrl(job.getResultKey()) : null);
        response.setCreatedAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
        return response;
    }
}
