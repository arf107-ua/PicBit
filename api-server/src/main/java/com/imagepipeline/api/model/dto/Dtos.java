package com.imagepipeline.api.model.dto;

import lombok.Data;
import java.util.List;

// ── Request: subir imagen ────────────────────────────────────────────
@Data
public class UploadImageRequest {
    private String title;
    private String description;
    private List<String> tags;
    private Boolean isPublic = true;
    private String boardId;
}

// ── Request: resize ──────────────────────────────────────────────────
@Data
public class ResizeRequest {
    private String imageId;
    private Integer width;
    private Integer height;
}

// ── Response: imagen completa (combina PG + Mongo) ───────────────────
@Data
public class ImageResponse {
    private String id;
    private String mongoId;
    private String title;
    private String description;
    private String storageUrl;       // URL pública en Supabase Storage
    private Boolean isPublic;
    private List<String> tags;
    private DimensionsDto dimensions;
    private List<String> colorPalette;
    private UserSummaryDto user;
    private String createdAt;

    @Data
    public static class DimensionsDto {
        private Integer width;
        private Integer height;
    }

    @Data
    public static class UserSummaryDto {
        private String id;
        private String username;
        private String avatarUrl;
    }
}

// ── Response: job de resize ──────────────────────────────────────────
@Data
public class ResizeJobResponse {
    private String jobId;
    private String imageId;
    private String status;
    private Integer width;
    private Integer height;
    private String resultUrl;
    private String createdAt;
}

// ── WebSocket: evento enviado al cliente ─────────────────────────────
@Data
public class WsEvent {
    private String type;       // "resize_complete", "new_image", "like", etc.
    private Object payload;

    public WsEvent(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
}
