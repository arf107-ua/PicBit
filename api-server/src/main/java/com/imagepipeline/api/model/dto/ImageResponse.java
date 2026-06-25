package com.imagepipeline.api.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImageResponse {
    private String id;
    private String mongoId;
    private String title;
    private String description;
    private String storageUrl;
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
