package com.imagepipeline.api.service;

import com.imagepipeline.api.model.dto.ImageResponse;
import com.imagepipeline.api.model.dto.ResizeJobResponse;
import com.imagepipeline.api.model.dto.ResizeRequest;
import com.imagepipeline.api.model.dto.UploadImageRequest;
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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository          imageRepository;
    private final ImageDocumentRepository  imageDocumentRepository;
    private final ResizeJobRepository      resizeJobRepository;
    private final UserRepository           userRepository;
    private final StorageService           storageService;
    private final QueueService             queueService;

    // ── Upload ───────────────────────────────────────────────────────

    @Transactional
    public ImageResponse upload(MultipartFile file, UploadImageRequest request, UUID userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Upload file to Supabase Storage
        String storageKey = storageService.upload(file, userId.toString());

        // 2. Save rich metadata in MongoDB
        ImageDocument doc = ImageDocument.builder()
                .tags(request.getTags())
                .originalDimensions(new ImageDocument.Dimensions(null, null))
                .createdAt(java.time.LocalDateTime.now())
                .build();
        ImageDocument savedDoc = imageDocumentRepository.save(doc);

        // 3. Save lightweight reference in PostgreSQL
        Image image = Image.builder()
                .user(user)
                .mongoId(savedDoc.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .storageKey(storageKey)
                .isPublic(request.getIsPublic())
                .build();
        Image savedImage = imageRepository.save(image);

        // 4. Update MongoDB with PG id (bidirectional bridge)
        savedDoc.setPgImageId(savedImage.getId().toString());
        imageDocumentRepository.save(savedDoc);

        // 5. Publish event to RabbitMQ
        queueService.publishImageUploaded(savedImage.getId().toString(), userId.toString());

        log.info("Imagen subida: pg={} mongo={}", savedImage.getId(), savedDoc.getId());
        return toResponse(savedImage, savedDoc);
    }

    // ── Get by id ────────────────────────────────────────────────────

    public ImageResponse getById(UUID imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));
        ImageDocument doc = imageDocumentRepository.findById(image.getMongoId())
                .orElseThrow(() -> new RuntimeException("Metadatos no encontrados"));
        return toResponse(image, doc);
    }

    // ── Feed ─────────────────────────────────────────────────────────

    /**
     * Fixed: was doing 1 PG query + N MongoDB queries (N+1 problem).
     * Now does 1 PG query + 1 MongoDB query using findAllById.
     */
    public List<ImageResponse> getFeed(UUID userId) {
        List<Image> images = imageRepository.findFeedForUser(userId);
        return mergeWithMongoDocuments(images);
    }

    // ── Explore ──────────────────────────────────────────────────────

    /**
     * Same fix applied here.
     */
    public List<ImageResponse> getPublic() {
        List<Image> images = imageRepository.findByIsPublicTrueOrderByCreatedAtDesc();
        return mergeWithMongoDocuments(images);
    }

    // ── Resize ───────────────────────────────────────────────────────

    @Transactional
    public ResizeJobResponse requestResize(ResizeRequest request, UUID userId) {
        Image image = imageRepository.findById(UUID.fromString(request.getImageId()))
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        ResizeJob job = ResizeJob.builder()
                .image(image)
                .user(user)
                .width(request.getWidth())
                .height(request.getHeight())
                .status(ResizeJob.JobStatus.pending)
                .build();
        ResizeJob savedJob = resizeJobRepository.save(job);

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

    // ── Complete resize (called by worker callback) ───────────────────

    @Transactional
    public void completeResizeJob(String jobId, String resultKey) {
        ResizeJob job = resizeJobRepository.findById(UUID.fromString(jobId))
                .orElseThrow(() -> new RuntimeException("Job no encontrado"));

        job.setStatus(ResizeJob.JobStatus.done);
        job.setResultKey(resultKey);
        resizeJobRepository.save(job);

        // Append processing record to MongoDB history
        imageDocumentRepository.findById(job.getImage().getMongoId()).ifPresent(doc -> {
            ImageDocument.ProcessingRecord record = new ImageDocument.ProcessingRecord(
                    "resize", resultKey, job.getWidth(), job.getHeight(),
                    java.time.LocalDateTime.now()
            );
            if (doc.getProcessingHistory() == null) {
                doc.setProcessingHistory(new ArrayList<>());
            }
            doc.getProcessingHistory().add(record);
            imageDocumentRepository.save(doc);
        });
    }

    // ── Private helpers ──────────────────────────────────────────────

    /**
     * Fetches MongoDB documents for a list of PG images in a single query,
     * then zips them together. Avoids N+1 problem.
     */
    private List<ImageResponse> mergeWithMongoDocuments(List<Image> images) {
        if (images.isEmpty()) return Collections.emptyList();

        List<String> mongoIds = images.stream()
                .map(Image::getMongoId)
                .toList();

        // Single MongoDB query for all documents
        Map<String, ImageDocument> docsById = imageDocumentRepository
                .findAllById(mongoIds)
                .stream()
                .collect(Collectors.toMap(ImageDocument::getId, d -> d));

        return images.stream()
                .map(image -> toResponse(image,
                        docsById.getOrDefault(image.getMongoId(), new ImageDocument())))
                .toList();
    }

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
