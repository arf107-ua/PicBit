package com.imagepipeline.api.model.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDocument {

    @Id
    private String id;

    private String pgImageId;
    private List<String> tags;
    private Dimensions originalDimensions;
    private Dimensions currentDimensions;
    private Map<String, String> variants;
    private List<String> colorPalette;
    private Map<String, Object> exif;
    private List<ProcessingRecord> processingHistory;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dimensions {
        private Integer width;
        private Integer height;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingRecord {
        private String operation;
        private String resultKey;
        private Integer width;
        private Integer height;
        private LocalDateTime processedAt;
    }
}
