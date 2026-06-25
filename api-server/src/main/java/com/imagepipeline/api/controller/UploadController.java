package com.imagepipeline.api.controller;

import com.imagepipeline.api.model.dto.ImageResponse;
import com.imagepipeline.api.model.dto.ResizeJobResponse;
import com.imagepipeline.api.model.dto.ResizeRequest;
import com.imagepipeline.api.model.dto.UploadImageRequest;
import com.imagepipeline.api.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     * Requires: Authorization: Bearer <token>
     */
    @PostMapping("/upload")
    public ResponseEntity<ImageResponse> upload(
            @RequestParam("file")                          MultipartFile file,
            @RequestParam("title")                         String title,
            @RequestParam(value = "description",  required = false) String description,
            @RequestParam(value = "tags",         required = false) List<String> tags,
            @RequestParam(value = "isPublic", defaultValue = "true") Boolean isPublic,
            @AuthenticationPrincipal String userId   // extracted from JWT by JwtFilter
    ) throws Exception {
        UploadImageRequest request = new UploadImageRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setTags(tags);
        request.setIsPublic(isPublic);

        return ResponseEntity.ok(imageService.upload(file, request, UUID.fromString(userId)));
    }

    /**
     * GET /api/images/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ImageResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(imageService.getById(UUID.fromString(id)));
    }

    /**
     * GET /api/images/feed
     */
    @GetMapping("/feed")
    public ResponseEntity<List<ImageResponse>> getFeed(
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(imageService.getFeed(UUID.fromString(userId)));
    }

    /**
     * GET /api/images/explore
     */
    @GetMapping("/explore")
    public ResponseEntity<List<ImageResponse>> explore() {
        return ResponseEntity.ok(imageService.getPublic());
    }

    /**
     * POST /api/images/resize
     */
    @PostMapping("/resize")
    public ResponseEntity<ResizeJobResponse> requestResize(
            @RequestBody ResizeRequest request,
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(imageService.requestResize(request, UUID.fromString(userId)));
    }
}
