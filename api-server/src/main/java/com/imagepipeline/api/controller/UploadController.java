package com.imagepipeline.api.controller;

import com.imagepipeline.api.model.dto.Dtos.*;
import com.imagepipeline.api.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class UploadController {

    private final ImageService imageService;

    /**
     * POST /api/images/upload
     * Sube una imagen (archivo + metadatos).
     */
    @PostMapping("/upload")
    public ResponseEntity<ImageResponse> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "tags", required = false) List<String> tags,
        @RequestParam(value = "isPublic", defaultValue = "true") Boolean isPublic,
        @RequestHeader("X-User-Id") String userId  // En prod: extraer del JWT
    ) throws Exception {
        UploadImageRequest request = new UploadImageRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setTags(tags);
        request.setIsPublic(isPublic);

        ImageResponse response = imageService.upload(file, request, UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/images/{id}
     * Obtiene una imagen por ID (combina PG + MongoDB).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ImageResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(imageService.getById(UUID.fromString(id)));
    }

    /**
     * GET /api/images/feed
     * Feed personalizado del usuario autenticado.
     */
    @GetMapping("/feed")
    public ResponseEntity<List<ImageResponse>> getFeed(
        @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(imageService.getFeed(UUID.fromString(userId)));
    }

    /**
     * GET /api/images/explore
     * Todas las imágenes públicas.
     */
    @GetMapping("/explore")
    public ResponseEntity<List<ImageResponse>> explore() {
        return ResponseEntity.ok(imageService.getPublic());
    }

    /**
     * POST /api/images/resize
     * Solicita un resize de imagen (crea job y lo manda a RabbitMQ).
     */
    @PostMapping("/resize")
    public ResponseEntity<ResizeJobResponse> requestResize(
        @RequestBody ResizeRequest request,
        @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(imageService.requestResize(request, UUID.fromString(userId)));
    }
}
