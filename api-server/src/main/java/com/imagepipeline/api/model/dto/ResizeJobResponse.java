package com.imagepipeline.api.model.dto;

import lombok.Data;

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
